package jp.ac.titech.itpro.sdl.circulardotter.component

import android.opengl.GLES31
import android.util.Log
import jp.ac.titech.itpro.sdl.circulardotter.Component
import jp.ac.titech.itpro.sdl.circulardotter.GlobalInfo
import jp.ac.titech.itpro.sdl.circulardotter.PointerIndex
import jp.ac.titech.itpro.sdl.circulardotter.RendererState
import jp.ac.titech.itpro.sdl.circulardotter.component.controller.ColorWheel
import jp.ac.titech.itpro.sdl.circulardotter.gl.ShaderProgram
import jp.ac.titech.itpro.sdl.circulardotter.gl.build
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.sqrt

class Buttons(globalInfo: GlobalInfo, private var rendererState: RendererState) :
    Component(globalInfo) {
    private val TAG = Buttons::class.qualifiedName

    private val vertexBuffer: FloatBuffer =
        ByteBuffer.allocateDirect(ColorWheel.vertices.size * 4).run() {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(ColorWheel.vertices)
                rewind()
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
                ColorWheel.DIMENSION,
                GLES31.GL_FLOAT,
                false,
                ColorWheel.STRIDE,
                vertexBuffer
            )
        }

        // uniform: iInclination
        shaderProgram.getUniformLocation("iInclination").also {
            GLES31.glUniform1f(it, -globalInfo.inclination.toFloat())
        }

        // uniform: iResolution
        shaderProgram.getUniformLocation("iResolution").also {
            GLES31.glUniform2f(it, windowWidth, windowHeight)
        }

        // iInnerWidth
        shaderProgram.getUniformLocation("iInnerWidth").also {
            GLES31.glUniform1f(it, Controller.CONTROLLER_WIDTH + windowHeight / sqrt(2.0f))
        }

        // iAreaWidth
        shaderProgram.getUniformLocation("iAreaWidth").also {
            GLES31.glUniform1f(it, BUTTON_AREA_WIDTH)
        }

        // iSeparateNum
        shaderProgram.getUniformLocation("iSeparateNum").also {
            GLES31.glUniform1i(it, SEPARATE_NUM)
        }

        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_FAN, 0, ColorWheel.verticesCnt)

        GLES31.glDisableVertexAttribArray(pos)
    }

    override fun onRelease(pointerIndex: PointerIndex, x: Float, y: Float) {
        super.onRelease(pointerIndex, x, y)
    }

    override fun onSurfaceCreated() {
        shaderProgram = ShaderProgram.setFragment(fragmentShader).setVertex(vertexShader).build()
    }

    override fun onScroll(pointerIndex: PointerIndex, x: Float, y: Float, dx: Float, dy: Float) {
        super.onScroll(pointerIndex, x, y, dx, dy)
    }

    override fun onTouch(pointerIndex: PointerIndex, x: Float, y: Float) {
        super.onTouch(pointerIndex, x, y)
    }

    companion object {
        const val DIMENSION = 2
        const val STRIDE = DIMENSION * 4
        const val BUTTON_AREA_WIDTH = 150.0f
        const val SEPARATE_NUM = 8

        val vertices = floatArrayOf(
            -1.0f, -1.0f,
            1.0f, -1.0f,
            1.0f, 1.0f,
            -1.0f, 1.0f
        )
        val verticesCnt = vertices.size / DIMENSION
        const val TWO_PI = PI * 2.0f

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
uniform float iInclination;
uniform vec2 iResolution;
uniform float iAreaWidth;
uniform float iInnerWidth;
uniform int iSeparateNum;

out vec4 fragColor;

const float sqrt2Halved = 0.70710678118;
const float PI = 3.14159265358;
const float TWO_PI = 6.28318530718;

const float BORDER_WIDTH = .02;

const float a = 0.95492965855;
const float b = 1.0471975512;

float atan2(in float y, in float x) {
    return x == 0.0 ? sign(y)*PI*.5 : atan(y, x);
}

void main() {
    vec2 coordCentered = vec2(coord.x * iResolution.x / iResolution.y, coord.y);
    float innerWidth = iInnerWidth / iResolution.y;
    float areaWidth = iAreaWidth / iResolution.y;
    float outerR = innerWidth + areaWidth;
    float r = length(coordCentered);
    if(r < innerWidth || r > outerR) discard;
    float theta = mod(atan2(coordCentered.y, coordCentered.x) - iInclination + TWO_PI, TWO_PI);
    if(theta < 0.2) {
        fragColor = vec4(0.0);
    }
    fragColor = vec4(1.0); // vec4(vec3(iInclination / TWO_PI + .5), 1.0);
}
        """
    }
}