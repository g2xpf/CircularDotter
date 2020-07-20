package jp.ac.titech.itpro.sdl.circulardotter.component

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Bitmap.createScaledBitmap
import android.graphics.BitmapFactory
import android.opengl.GLES31
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.math.MathUtils.clamp
import jp.ac.titech.itpro.sdl.circulardotter.*
import jp.ac.titech.itpro.sdl.circulardotter.gl.ImageTexture
import jp.ac.titech.itpro.sdl.circulardotter.gl.ShaderProgram
import jp.ac.titech.itpro.sdl.circulardotter.gl.MutableTexture
import jp.ac.titech.itpro.sdl.circulardotter.gl.build
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.floor


enum class CanvasMode(val innerValue: Int) {
    Read(0),
    Write(1),
    Uninit(2),
    SaveRequested(3)
}

class Canvas(globalInfo: GlobalInfo, rendererState: RendererState) :
    Component(globalInfo, rendererState) {
    private val TAG = Canvas::class.qualifiedName

    // private val pixels: Buffer
    private val vertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(vertices.size * 4).run() {
        order(ByteOrder.nativeOrder())
        asFloatBuffer().apply {
            put(vertices)
            position(0)
        }
    }
    private lateinit var shaderProgram: ShaderProgram

    private var initSize: Pair<Int, Int>? = null

    private var imageWidth = 32
    private var imageHeight = 32
    private lateinit var imageBuffer: ByteBuffer

    private var canvasTexture: MutableTexture? = null
    private lateinit var initImageTexture: ImageTexture
    private lateinit var saveImageTexture: ImageTexture

    private var pointerIndex: Int? = null

    // uv coord
    private var cursor = Pair<Float, Float>(0.0f, 0.0f)

    private fun initializeCanvasTexture(width: Int, height: Int) {
        imageWidth = width
        imageHeight = height

        imageBuffer = ByteBuffer.allocateDirect(imageWidth * imageHeight * 3).run() {
            order(ByteOrder.nativeOrder())
        }
        repeat(3 * imageWidth * imageHeight) {
            imageBuffer.put(255.toByte())
        }
        imageBuffer.rewind()

        canvasTexture = MutableTexture(imageWidth, imageHeight, imageBuffer)
        canvasTexture!!.initialize()
    }

    override fun draw() {
        super.draw()
        // GLES31.glTexImage2D()
        shaderProgram.use()

        // attribute: pos
        val pos = shaderProgram.getAttribLocation("pos").also {
            GLES31.glEnableVertexAttribArray(it)
            GLES31.glVertexAttribPointer(
                it,
                DIMENSION,
                GLES31.GL_FLOAT,
                false,
                STRIDE,
                vertexBuffer
            )
        }

        // uniform: iResolution
        shaderProgram.getUniformLocation("iResolution").also {
            GLES31.glUniform2f(it, windowWidth, windowHeight)
        }

        // uniform: canvasTexture
        when (rendererState.canvasMode) {
            CanvasMode.Write, CanvasMode.Read -> {
                val textureIndex = canvasTexture!!.use()
                shaderProgram.getUniformLocation("canvasTexture").also {
                    GLES31.glUniform1i(it, textureIndex)
                }
            }
            CanvasMode.Uninit -> {
                val textureIndex = initImageTexture.use()
                shaderProgram.getUniformLocation("canvasTexture").also {
                    GLES31.glUniform1i(it, textureIndex)
                }
            }
            CanvasMode.SaveRequested -> {
                val textureIndex = saveImageTexture.use()
                shaderProgram.getUniformLocation("canvasTexture").also {
                    GLES31.glUniform1i(it, textureIndex)
                }
            }
        }

        // uniform: showGrid, showCentralGrid
        shaderProgram.getUniformLocation("iShowGrid").also {
            GLES31.glUniform2i(
                it,
                if (rendererState.showGrid) 1 else 0,
                if (rendererState.showCentralGrid) 1 else 0
            )
        }

        // uniform: imageDimension
        shaderProgram.getUniformLocation("imageDimension").also {
            GLES31.glUniform2f(it, imageWidth.toFloat(), imageHeight.toFloat())
        }

        // uniform: cursor
        shaderProgram.getUniformLocation("iCursorPos").also {
            val (x, y) = cursor
            GLES31.glUniform2i(it, (x * imageWidth).toInt(), (y * imageHeight).toInt())
        }

        // uniform: iTime
        shaderProgram.getUniformLocation("iTime").also {
            GLES31.glUniform1f(it, globalInfo.time)
        }

        // uniform: iCursorSize
        shaderProgram.getUniformLocation("iCursorSize").also {
            GLES31.glUniform1i(it, rendererState.brushSize)
        }

        // uniform: iCursorSize
        shaderProgram.getUniformLocation("iCanvasMode").also {
            GLES31.glUniform1i(it, rendererState.canvasMode.innerValue)
        }

        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_FAN, 0, verticesCnt)

        GLES31.glDisableVertexAttribArray(pos)
    }

    override fun update() {
        if(rendererState.fillCanvas) {
            rendererState.fillCanvas = false
            canvasTexture?.write(imageWidth / 2, imageHeight / 2, imageWidth + 1, imageHeight + 1, rendererState.brushColor)
        }
        val size = initSize
        if(rendererState.canvasMode == CanvasMode.Uninit && size != null) {
            val (w, h) = size
            initializeCanvasTexture(w, h)
            initSize = null
            rendererState.canvasMode = CanvasMode.Write
        }

        when (val saveImageState = rendererState.saveImageState) {
            is SaveImageState.SaveRequested -> {
                val destSize = saveImageState.destSize
                rendererState.saveImageState = SaveImageState.Steady
                rendererState.canvasMode = CanvasMode.Write

                thread {
                    val (width, height, image) = canvasTexture!!.read()
                    val fileName = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.JAPAN).format(Date())
                    Log.d(TAG, "image: $image")
                    var bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bmp.copyPixelsFromBuffer(image)
                    bmp = createScaledBitmap(bmp, destSize, destSize, false)

                    val contentResolver = rendererState.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, "$fileName.jpg")
                        put(MediaStore.Images.Media.TITLE, fileName)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put(MediaStore.Images.Media.DESCRIPTION, "generated by CircularDotter")
                        put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1_000)
                    }

                    contentResolver?.run {
                        val uri =
                            insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                                ?: return@run
                        Log.d(TAG, "$uri")
                        openOutputStream(uri).use {
                            bmp.compress(Bitmap.CompressFormat.PNG, 100, it)
                            update(uri, contentValues, null, null)
                        }
                    }

                    rendererState.activity?.runOnUiThread {
                        Toast.makeText(
                            rendererState.activity,
                            "successfully saved!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    override fun onTouch(pointerIndex: PointerIndex, x: Float, y: Float) {
        val isOnCanvas = abs(x - windowWidth * 0.5) <= windowHeight * 0.5
        if (isOnCanvas) {
            this.pointerIndex = pointerIndex
        }
        val (uvx, uvy) = (x - windowWidth * 0.5) / windowHeight + 0.5f to (windowHeight - y) / windowHeight
        val (ix, iy) = clamp((uvx * 2.0).toInt(), 0, 1) to clamp((uvy * 2.0).toInt(), 0, 1)
        when (rendererState.canvasMode) {
            CanvasMode.Write -> {
                if (rendererState.isDrawing) {
                    requestDraw()
                }
            }
            CanvasMode.Read -> {
                if (rendererState.isDrawing) {
                    val (x, y) = getCursorPos()
                    rendererState.brushColor = canvasTexture!!.readColor(x, y)
                }
            }
            CanvasMode.Uninit -> {
                if (!isOnCanvas) return
                val imageSize = initImageSizeArray[ix][iy]
                Log.d(TAG, "initialized as $imageSize * $imageSize")
                initSize = imageSize to imageSize
            }
            CanvasMode.SaveRequested -> {
                if (!isOnCanvas) return
                val (cuvx, cuvy) = uvx - 0.5 to uvy - 0.5
                val radius = 0.2
                if(radius * radius > cuvx * cuvx + cuvy * cuvy) {
                    rendererState.canvasMode = CanvasMode.Write
                } else {
                    val imageSize = saveImageSizeArray[ix][iy]
                    rendererState.saveImageState =
                        SaveImageState.SaveRequested(imageSize ?: imageWidth)
                }
            }
        }
    }

    override fun onScroll(pointerIndex: PointerIndex, x: Float, y: Float, dx: Float, dy: Float) {
        if (this.pointerIndex != pointerIndex) return
        val (x, y) = cursor

        val nx = clamp(x + dx / windowHeight, 0.0f, 0.9999f)
        // div by width, and y is reversed
        val ny = clamp(y + dy / windowHeight, 0.0f, 0.9999f)

        cursor = Pair(nx, ny)

        if (rendererState.isDrawing) {
            when (rendererState.canvasMode) {
                CanvasMode.Write -> {
                    requestDraw()
                }
                CanvasMode.Read -> {
                    val (x, y) = getCursorPos()
                    rendererState.brushColor = canvasTexture!!.readColor(x, y)
                }
            }
        }
    }

    override fun onRelease(pointerIndex: PointerIndex, x: Float, y: Float) {
        super.onRelease(pointerIndex, x, y)
        if (this.pointerIndex == pointerIndex) {
            this.pointerIndex = null
        }
    }

    override fun onSurfaceCreated() {
        shaderProgram = ShaderProgram.setFragment(fragmentShader).setVertex(vertexShader).build()

        canvasTexture?.initialize()

        val initImage = BitmapFactory.decodeResource(
            rendererState.activity?.resources,
            R.drawable.init_size_select
        )
        initImageTexture = ImageTexture(initImage)

        val saveImage = BitmapFactory.decodeResource(
            rendererState.activity?.resources,
            R.drawable.save_image_size_select
        )
        saveImageTexture = ImageTexture(saveImage)

        initImageTexture.initialize()
        saveImageTexture.initialize()
    }

    private fun getCursorPos(): Pair<Int, Int> {
        val (x, y) = cursor
        // pixel to imageCoord
        val nx = floor((x * imageWidth).toDouble()).toInt()
        val ny = floor((y * imageHeight).toDouble()).toInt()
        assert((nx in 0 until imageWidth) && (ny in 0 until imageHeight)) {
            Log.e(TAG, "pixel range error")
        }
        return nx to ny
    }

    private fun requestDraw() {
        val (x, y) = getCursorPos()
        canvasTexture!!.write(
            x,
            y,
            rendererState.brushSize,
            rendererState.brushSize,
            rendererState.brushColor
        )
    }

    companion object {
        const val DIMENSION = 2
        const val STRIDE = DIMENSION * 4
        val initImageSizeArray = arrayOf(arrayOf(16, 8), arrayOf(64, 32))
        val saveImageSizeArray = arrayOf(arrayOf(64, null), arrayOf(256, 128))

        val vertices = floatArrayOf(
            -1.0f, -1.0f,
            1.0f, -1.0f,
            1.0f, 1.0f,
            -1.0f, 1.0f
        )
        val verticesCnt = vertices.size / DIMENSION

        const val vertexShader = """
#version 310 es
in vec2 pos;
out vec2 uv;

void main() {
    gl_Position = vec4(pos, 0.0, 1.0);
    uv = (pos + 1.0) * .5; 
}
"""
        const val fragmentShader = """
#version 310 es
precision mediump float;

uniform sampler2D canvasTexture;
uniform float iTime;
uniform vec2 iResolution;
uniform vec2 imageDimension;
uniform int iCursorSize;
uniform int iCanvasMode;
// x: showGrid, y: showCentralGrid
uniform ivec2 iShowGrid;
uniform ivec2 iCursorPos;

in vec2 uv;

out vec4 fragColor;

const float GRID_WIDTH = 0.001;
const float CENTRAL_GRID_WIDTH = GRID_WIDTH * 2.;
const float TWO_PI = 6.283185;

void fillGrid(in vec2 p, in vec2 cellSize, in float gridWidth, in vec4 color) {
    vec2 roundWidth = cellSize * .5 - abs(mod(p, cellSize) - (cellSize * .5)); 
    if(roundWidth.x < gridWidth || roundWidth.y < gridWidth) {
        fragColor = color;
    }
}

void main() {
    if(abs(gl_FragCoord.x * 2.0 - iResolution.x) > iResolution.y) discard;
    
    vec2 uvCentered = vec2(clamp((uv.x - .5) * iResolution.x / iResolution.y + .5, 0.0, 1.0), uv.y);
    vec2 cellSize = 1.0 / imageDimension;
    
    // canvas
    if(iCanvasMode >= 2) {
        // when shouldn't draw the canvas, draw images only
        vec2 uvFlipped = vec2(uvCentered.x, 1.0 - uvCentered.y);
        fragColor = smoothstep(0.6, 1.0, texture(canvasTexture, uvFlipped));
        return;
    }
    vec4 canvasColor = texture(canvasTexture, uvCentered);
    fragColor = vec4(canvasColor.xyz, 1.0);
    
    // normal grid
    if(iShowGrid.x > 0) {
        fillGrid(uvCentered, cellSize, GRID_WIDTH, vec4(vec3(0.7), 1.0));
    }
    
    // central grid
    if(iShowGrid.y > 0) {
        vec2 width = abs(uvCentered - vec2(.5));
        if(width.x < CENTRAL_GRID_WIDTH || width.y < CENTRAL_GRID_WIDTH) {
            fragColor = vec4(vec3(0.0), 1.0);
        }
    }
    
    // cursor
    // float flash = abs(fract(iTime) - .5) * 2.;
    float flash = fract(iTime) * TWO_PI;
    vec4 flashColor = iCanvasMode > 0 ?
        vec4(
            (cos(flash) + 1.) * .5,
            (sin(flash) + 1.) * .5,
            (sin(2. * flash) + 1.) * .5, 1.0
        ) : vec4((cos(flash) + 1.) * .5, 1.0, (cos(flash) + 1.) * .5, 1.0);
    ivec2 diffVec = abs(iCursorPos - ivec2(uvCentered * imageDimension));
    
    int cursorSize = iCanvasMode > 0 ? iCursorSize : 1;
    
    if(diffVec.x <= cursorSize / 2 && diffVec.y <= cursorSize / 2) {
        ivec2 leftDown = max(iCursorPos - cursorSize / 2, ivec2(0, 0));
        ivec2 rightUp = min(iCursorPos + cursorSize / 2, ivec2(imageDimension) - ivec2(1));
        vec2 wh = vec2(rightUp - leftDown + ivec2(1)) * cellSize;
        fillGrid(uvCentered - vec2(leftDown) / imageDimension, wh, (cellSize * 0.1).x, flashColor);
    }
}
        """
    }
}