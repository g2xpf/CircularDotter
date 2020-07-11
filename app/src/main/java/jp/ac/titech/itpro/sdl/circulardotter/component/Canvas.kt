package jp.ac.titech.itpro.sdl.circulardotter.component

import android.opengl.GLES31
import android.util.Log
import jp.ac.titech.itpro.sdl.circulardotter.Component
import jp.ac.titech.itpro.sdl.circulardotter.gl.ShaderProgram
import jp.ac.titech.itpro.sdl.circulardotter.gl.build
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class Canvas : Component {
    private val TAG = Canvas::class.qualifiedName

    // private val pixels: Buffer
    private val vertexBuffer: FloatBuffer
    private val shaderProgram: ShaderProgram

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
    }

    constructor(width: Int, height: Int) {
        // pixels = ByteBuffer()
    }

    override fun draw() {
        Log.d(TAG, "drawing...");
        // GLES31.glTexImage2D()
        shaderProgram.use()
        val pos = shaderProgram.getAttribLocation("pos")
        Log.d(TAG, "pos: $pos");
        GLES31.glEnableVertexAttribArray(pos)
        GLES31.glVertexAttribPointer(
            pos,
            DIMENSION,
            GLES31.GL_FLOAT,
            false,
            STRIDE,
            vertexBuffer
        )

        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_FAN, 0, verticesCnt)

        GLES31.glDisableVertexAttribArray(pos)
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
attribute vec2 pos;
varying vec2 uv;

void main() {
    gl_Position = vec4(pos, 0.0, 1.0);
    uv = (pos + 1.0) * .5; 
}
"""
        const val fragmentShader = """
precision mediump float;
varying vec2 uv;

void main() {
    gl_FragColor = (gl_FragCoord.x > 100.0) ? vec4(uv, 0.0, 1.0) : vec4(0.2, 0.2, 0.2, 1.0);
}
        """
    }
}