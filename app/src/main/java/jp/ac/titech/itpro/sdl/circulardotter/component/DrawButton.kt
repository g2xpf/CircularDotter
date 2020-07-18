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

enum class ButtonState {
    Pushed,
    Released
}

class DrawButton(globalInfo: GlobalInfo, private val rendererState: RendererState) : Component(globalInfo) {
    private val TAG = DrawButton::class.qualifiedName
    private var state: ButtonState = ButtonState.Released
    private var pointerIndex: Int? = null

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
    }

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
            GLES31.glUniform1i(it, if (state == ButtonState.Pushed) 1 else 0)
        }

        shaderProgram.getUniformLocation("iColor").also {
            val (r, g, b) = rendererState.brushColor
            GLES31.glUniform3f(it, r, g, b)
        }

        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_FAN, 0, verticesCnt)

        GLES31.glDisableVertexAttribArray(pos)
    }

    override fun onTouch(pointerIndex: PointerIndex, x: Float, y: Float) {
        if (isOnButton(x, y)) {
            Log.d(TAG, "touched: ${this.pointerIndex} -> $pointerIndex")
            state = ButtonState.Pushed
            this.pointerIndex = pointerIndex
        }
    }

    override fun onRelease(pointerIndex: PointerIndex, x: Float, y: Float) {
        Log.d(TAG, "released: ${this.pointerIndex} -> $pointerIndex")
        if (this.pointerIndex == pointerIndex) {
            state = ButtonState.Released
            this.pointerIndex = null
        }
    }

    private fun isOnButton(x: Float, y: Float): Boolean {
        val cx = x - windowWidth * 0.5
        val cy = y - windowHeight * 0.5
        val notOnCanvas = abs(cx) > windowHeight * 0.5
        val isOnCircle = Vector2D(cx, cy).normSq < windowHeight * windowHeight * 0.5
        return notOnCanvas && isOnCircle
    }

    fun send(receiver: Canvas) {
        receiver.receive(Canvas.Message.ShouldDraw(isPushed()))
    }

    private fun isPushed(): Boolean {
        return state == ButtonState.Pushed
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
uniform vec2 iResolution;
out vec4 fragColor;

const float sqrt2Halved = 0.70710678118;

void main() {
    vec2 coordCentered = vec2(coord.x * iResolution.x / iResolution.y, coord.y);
    if(abs(gl_FragCoord.x * 2.0 - iResolution.x) < iResolution.y || length(coordCentered) > sqrt2Halved) discard;
    fragColor = (iPushed > 0) ? vec4(vec3(0.0), 1.0) : vec4(iColor, 1.0);
}
        """
    }

}