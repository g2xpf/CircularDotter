package jp.ac.titech.itpro.sdl.circulardotter.component

import android.opengl.GLES31
import android.util.Log
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

    private var width = 1.0f;
    private var height = 1.0f;

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
        this.width = width.toFloat()
        this.height = height.toFloat()
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
            GLES31.glUniform2f(it, width, height)
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
            GLES31.glUniform2i(it, if(showGrid) 1 else 0, if(showCentralGrid) 1 else 0)
        }

        // uniform: imageDimension
        shaderProgram.getUniformLocation("imageDimension").also {
            Log.d(TAG, "imageDimension: $it")
            GLES31.glUniform2f(it, imageWidth.toFloat(), imageHeight.toFloat())
        }


        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_FAN, 0, verticesCnt)

        GLES31.glDisableVertexAttribArray(pos)
    }

    fun setGrid(v: Boolean) {
        showGrid = v
    }

    fun setCentralGrid(v: Boolean) {
        showCentralGrid = v
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
in vec2 uv;
out vec4 fragColor;

const float GRID_RATIO = 0.05;

void main() {
    if(abs(gl_FragCoord.x * 2.0 - iResolution.x) > iResolution.y) discard;
    
    vec2 uvCentered = vec2(clamp((uv.x - .5) * iResolution.x / iResolution.y + .5, 0.0, 1.0), uv.y);
    vec2 cellSize = 1.0 / imageDimension;
    vec2 gridBorder = cellSize * GRID_RATIO;
    vec2 centralGridBorder = gridBorder * 2.0;
    
    // central grid
    if(showGrid.y > 0) {
        vec2 width = abs(uvCentered - vec2(.5));
        if(width.x < centralGridBorder.x || width.y < centralGridBorder.y) {
            fragColor = vec4(vec3(0.0), 1.0);
            return;
        }
    }
    
    // normal grid
    if(showGrid.x > 0) {
        vec2 roundWidth = cellSize * .5 - abs(mod(uvCentered, cellSize) - (cellSize * .5)); 
        if(roundWidth.x < gridBorder.x || roundWidth.y < gridBorder.y) {
            fragColor = vec4(vec3(0.3), 1.0);
            return;
        }
    }
    
    fragColor = vec4(texture(canvasTexture, uvCentered).xyz, 1.0);
}
        """
    }
}