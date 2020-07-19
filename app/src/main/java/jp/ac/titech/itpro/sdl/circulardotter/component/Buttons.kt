package jp.ac.titech.itpro.sdl.circulardotter.component

import android.opengl.GLES31
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

class Buttons(globalInfo: GlobalInfo, rendererState: RendererState) :
    CircularComponent(globalInfo, rendererState) {
    private val TAG = Buttons::class.qualifiedName
    private var pointerIndex: PointerIndex? = null

    override val componentRadius: Float
        get() = windowHeight / sqrt(2.0f) + 150.0f
    override val componentWidth: Float
        get() = 150.0f

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
            GLES31.glUniform1f(it, componentRadius)
        }

        // iAreaWidth
        shaderProgram.getUniformLocation("iAreaWidth").also {
            GLES31.glUniform1f(it, componentWidth)
        }

        // iSeparateNum
        shaderProgram.getUniformLocation("iSeparateNum").also {
            GLES31.glUniform1i(it, SEPARATE_NUM)
        }

        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_FAN, 0, ColorWheel.verticesCnt)

        GLES31.glDisableVertexAttribArray(pos)
    }

    override fun onSurfaceCreated() {
        shaderProgram = ShaderProgram.setFragment(fragmentShader).setVertex(vertexShader).build()
    }

    override fun onRelease(pointerIndex: PointerIndex, x: Float, y: Float) {
        super.onRelease(pointerIndex, x, y)
    }

    override fun onTouchScaled(isOnController: Boolean, r: Float, theta: Float) {
        if(!isOnController) return
        val angle = (theta - globalInfo.inclination + TWO_PI) % TWO_PI
        val kind = (angle * SEPARATE_NUM.toFloat() / TWO_PI).toInt()
        when(kind) {
            0 -> rendererState.controllerMode = ControllerMode.ColorWheel
            1 -> rendererState.showGrid = !rendererState.showGrid
            2 -> rendererState.showCentralGrid = !rendererState.showCentralGrid
            5 -> rendererState.brushSize = if(rendererState.brushSize >= 11) 1 else rendererState.brushSize + 2
            6 -> rendererState.canvasMode = if(rendererState.canvasMode == CanvasMode.Write) CanvasMode.Read else CanvasMode.Write
            7 -> rendererState.brushColor = Triple(1.0f, 1.0f, 1.0f)
        }
    }

    override fun onScrollScaled(
        isOnComponent: Boolean,
        r: Float,
        theta: Float,
        dr: Float,
        dtheta: Float
    ) {
    }

    override fun onReleaseScaled(isOnController: Boolean, r: Float, theta: Float) {
    }

    companion object {
        const val DIMENSION = 2
        const val STRIDE = DIMENSION * 4
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
    float theta = mod(atan2(coordCentered.y, coordCentered.x) + iInclination + TWO_PI, TWO_PI);
    float borderAngle = TWO_PI / float(iSeparateNum);
    float modAngle = mod(theta, borderAngle);
    int buttonKind = int(floor(theta / borderAngle));
    for(int i = 0; i < iSeparateNum; ++i) {
        float r = ((i >> 2) & 1) == 1 ? 1.0 : 0.0;
        float g = ((i >> 1) & 1) == 1 ? 1.0 : 0.0;
        float b = ((i >> 0) & 1) == 1 ? 1.0 : 0.0;
        if(i == buttonKind) {
            fragColor = vec4(r, g, b, 1.0);
            break;
        }
    }
    if(modAngle < BORDER_WIDTH || modAngle > borderAngle - BORDER_WIDTH) {
        fragColor = vec4(.1);
    }
}
        """
    }
}