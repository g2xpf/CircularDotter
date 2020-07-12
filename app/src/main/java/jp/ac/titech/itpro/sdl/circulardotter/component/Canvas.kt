package jp.ac.titech.itpro.sdl.circulardotter.component

import android.opengl.GLES31
import android.util.Log
import androidx.core.math.MathUtils.clamp
import jp.ac.titech.itpro.sdl.circulardotter.Component
import jp.ac.titech.itpro.sdl.circulardotter.gl.ShaderProgram
import jp.ac.titech.itpro.sdl.circulardotter.gl.Texture
import jp.ac.titech.itpro.sdl.circulardotter.gl.build
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.*

class Canvas : Component {
    private val TAG = Canvas::class.qualifiedName

    // private val pixels: Buffer
    private val vertexBuffer: FloatBuffer
    private val shaderProgram: ShaderProgram

    private var imageWidth = 32;
    private var imageHeight = 32;
    private var imageBuffer: ByteBuffer

    private val canvasTexture: Texture;

    private var showCentralGrid = true
    private var showGrid = true

    private var windowWidth = 1.0f;
    private var windowHeight = 1.0f;

    // uv coord
    private var cursor = Pair<Float, Float>(0.0f, 0.0f)

    init {
        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4).run() {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(vertices)
                position(0)
            }
        }

        shaderProgram = ShaderProgram.setFragment(fragmentShader).setVertex(vertexShader).build()
        Log.d(TAG, "status: " + GLES31.glGetError())

        imageBuffer = ByteBuffer.allocateDirect(imageWidth * imageHeight * 3).run() {
            order(ByteOrder.nativeOrder())
        }
        // TODO: naive implementation
        repeat(imageWidth * imageHeight) { i ->
            val r = (i % imageWidth) * (256 / imageWidth)
            val g = (i / imageHeight) * (256 / imageHeight)
            imageBuffer.put(r.toByte())
            imageBuffer.put(g.toByte())
            imageBuffer.put(0.toByte())
        }
        imageBuffer.rewind()

        canvasTexture = Texture(imageWidth, imageHeight, imageBuffer);
    }

    override fun onWindowResized(width: Int, height: Int) {
        windowWidth = width.toFloat()
        windowHeight = height.toFloat()
    }

    override fun draw() {
        Log.d(TAG, "drawing...");
        // GLES31.glTexImage2D()
        shaderProgram.use()

        // attribute: pos
        val pos = shaderProgram.getAttribLocation("pos").also {
            Log.d(TAG, "pos: $it")
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
        val iResolution = shaderProgram.getUniformLocation("iResolution").also {
            Log.d(TAG, "iResolution: $it")
            GLES31.glUniform2f(it, windowWidth, windowHeight)
        }

        // uniform: canvasTexture
        val textureIndex = canvasTexture.use();
        Log.d(TAG, "textureIndex: $textureIndex")
        val texture = shaderProgram.getUniformLocation("canvasTexture").also {
            Log.d(TAG, "canvasTexture: $it")
            GLES31.glUniform1i(it, textureIndex)
        }

        // uniform: showGrid, showCentralGrid
        shaderProgram.getUniformLocation("showGrid").also {
            Log.d(TAG, "showGrid: $it")
            GLES31.glUniform2i(it, if (showGrid) 1 else 0, if (showCentralGrid) 1 else 0)
        }

        // uniform: imageDimension
        shaderProgram.getUniformLocation("imageDimension").also {
            Log.d(TAG, "imageDimension: $it")
            GLES31.glUniform2f(it, imageWidth.toFloat(), imageHeight.toFloat())
        }

        // uniform: cursor
        shaderProgram.getUniformLocation("cursorPos").also {
            Log.d(TAG, "cursorPos: $it")
            val (x, y) = cursor
            GLES31.glUniform2i(it, (x * imageWidth).toInt(), (y * imageHeight).toInt())
            Log.d(TAG, "x: " + (x * imageWidth).toInt() + ", y: " + (y * imageHeight).toInt())
        }

        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_FAN, 0, verticesCnt)

        GLES31.glDisableVertexAttribArray(pos)
    }

    fun moveCursor(dx: Float, dy: Float) {
        val (x, y) = cursor

        val nx = clamp(x + dx / windowHeight, 0.0f, 0.9999f)
        // div by width, and y is reversed
        val ny = clamp(y + dy / windowHeight, 0.0f, 0.9999f)

        Log.d(TAG, "(x, y, dx, dy, nx, ny) = ($x, $y, $dx, $dy, $nx, $ny)")
        cursor = Pair(nx, ny)
    }

    fun setGrid(v: Boolean) {
        showGrid = v
    }

    fun setCentralGrid(v: Boolean) {
        showCentralGrid = v
    }

    fun setColor(x: Int, y: Int, color: Triple<Int, Int, Int>) {
        val buffer = ByteBuffer.allocateDirect(3).run {
            order(ByteOrder.nativeOrder())
            val (r, g, b) = color
            put(r.toByte())
            put(g.toByte())
            put(b.toByte())
            rewind()
        }
        canvasTexture.write(x, y, 1, 1, buffer)
    }

    companion object {
        const val DIMENSION = 2
        const val STRIDE = DIMENSION * 4

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
uniform vec2 iResolution;
uniform vec2 imageDimension;
// x: showGrid, y: showCentralGrid
uniform ivec2 showGrid;
uniform ivec2 cursorPos;

in vec2 uv;

out vec4 fragColor;

const float GRID_WIDTH = 0.001;
const float CENTRAL_GRID_WIDTH = GRID_WIDTH * 2.;

bool fillGrid(in vec2 p, in vec2 cellSize, in float gridWidth, in vec4 color) {
    vec2 roundWidth = cellSize * .5 - abs(mod(p, cellSize) - (cellSize * .5)); 
    return (roundWidth.x < gridWidth || roundWidth.y < gridWidth) && (fragColor = color, true);
}

void main() {
    if(abs(gl_FragCoord.x * 2.0 - iResolution.x) > iResolution.y) discard;
    
    vec2 uvCentered = vec2(clamp((uv.x - .5) * iResolution.x / iResolution.y + .5, 0.0, 1.0), uv.y);
    vec2 cellSize = 1.0 / imageDimension;
    
    // cursor
    if(cursorPos == ivec2(floor(uvCentered * imageDimension)) && fillGrid(uvCentered, cellSize, GRID_WIDTH * 2., vec4(1.0, 0.0, 0.0, 1.0))) {
        return;
    }
    
    // central grid
    if(showGrid.y > 0) {
        vec2 width = abs(uvCentered - vec2(.5));
        if(width.x < CENTRAL_GRID_WIDTH || width.y < CENTRAL_GRID_WIDTH) {
            fragColor = vec4(vec3(0.0), 1.0);
            return;
        }
    }
    
    // normal grid and cursor
    if(showGrid.x > 0 && fillGrid(uvCentered, cellSize, GRID_WIDTH, vec4(vec3(0.7), 1.0))) {
        return;
    }
    
    fragColor = vec4(texture(canvasTexture, uvCentered).xyz, 1.0);
}
        """
    }
}