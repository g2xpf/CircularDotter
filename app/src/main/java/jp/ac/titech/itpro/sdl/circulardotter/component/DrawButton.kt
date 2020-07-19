package jp.ac.titech.itpro.sdl.circulardotter.component

import android.opengl.GLES31
import android.util.Log
import jp.ac.titech.itpro.sdl.circulardotter.*
import jp.ac.titech.itpro.sdl.circulardotter.gl.ShaderProgram
import jp.ac.titech.itpro.sdl.circulardotter.gl.build
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.abs

class DrawButton(globalInfo: GlobalInfo, rendererState: RendererState) :
    Component(globalInfo, rendererState) {
    private val TAG = DrawButton::class.qualifiedName
    private var pointerIndex: Int? = null

    private val vertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(vertices.size * 4).run() {
        order(ByteOrder.nativeOrder())
        asFloatBuffer().apply {
            put(vertices)
            position(0)
        }
    }
    private lateinit var shaderProgram: ShaderProgram

    override fun draw() {
        super.draw()
        shaderProgram.use()

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

        shaderProgram.getUniformLocation("iResolution").also {
            GLES31.glUniform2f(it, windowWidth, windowHeight)
        }

        shaderProgram.getUniformLocation("iPushed").also {
            GLES31.glUniform1i(it, if (rendererState.isDrawing) 1 else 0)
        }

        shaderProgram.getUniformLocation("iColor").also {
            val (r, g, b) = rendererState.brushColor
            GLES31.glUniform3f(it, r, g, b)
        }

        shaderProgram.getUniformLocation("iCanvasMode").also {
            GLES31.glUniform1i(it, if (rendererState.canvasMode == CanvasMode.Write) 1 else 0)
        }

        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_FAN, 0, verticesCnt)

        GLES31.glDisableVertexAttribArray(pos)
    }

    override fun onTouch(pointerIndex: PointerIndex, x: Float, y: Float) {
        if (isOnButton(x, y)) {
            rendererState.isDrawing = true
            this.pointerIndex = pointerIndex
        }
    }

    override fun onRelease(pointerIndex: PointerIndex, x: Float, y: Float) {
        if (this.pointerIndex == pointerIndex) {
            rendererState.isDrawing = false
            this.pointerIndex = null
        }
    }

    override fun onSurfaceCreated() {
        shaderProgram = ShaderProgram.setFragment(fragmentShader).setVertex(vertexShader).build()
    }

    private fun isOnButton(x: Float, y: Float): Boolean {
        val cx = x - windowWidth * 0.5
        val cy = y - windowHeight * 0.5
        val notOnCanvas = abs(cx) > windowHeight * 0.5
        val isOnCircle = Vector2D(cx, cy).normSq < windowHeight * windowHeight * 0.5
        return notOnCanvas && isOnCircle
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
out vec2 coord;

void main() {
    gl_Position = vec4(pos, 0.0, 1.0);
    coord = pos * 0.5;
}
        """
        const val fragmentShader = """
#version 310 es
precision mediump float;

in vec2 coord;
uniform vec3 iColor;
uniform int iPushed;
uniform int iCanvasMode;
uniform vec2 iResolution;
out vec4 fragColor;

const float sqrt2Halved = 0.70710678118;

void main() {
    vec2 coordCentered = vec2(coord.x * iResolution.x / iResolution.y, coord.y);
    if(abs(gl_FragCoord.x * 2.0 - iResolution.x) < iResolution.y || length(coordCentered) > sqrt2Halved) discard;
    fragColor = (iPushed > 0 && iCanvasMode > 0) ? vec4(vec3(0.0), 1.0) : vec4(iColor, 1.0);
}
        """
    }

}