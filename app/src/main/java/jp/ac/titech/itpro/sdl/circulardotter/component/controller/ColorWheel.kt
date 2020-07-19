package jp.ac.titech.itpro.sdl.circulardotter.component.controller

import android.opengl.GLES31
import android.util.Log
import jp.ac.titech.itpro.sdl.circulardotter.GlobalInfo
import jp.ac.titech.itpro.sdl.circulardotter.RendererState
import jp.ac.titech.itpro.sdl.circulardotter.component.CircularComponent
import jp.ac.titech.itpro.sdl.circulardotter.gl.ShaderProgram
import jp.ac.titech.itpro.sdl.circulardotter.gl.build
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.sqrt

class ColorWheel(
    globalInfo: GlobalInfo,
    rendererState: RendererState
) : CircularComponent(globalInfo, rendererState) {
    private val TAG = ColorWheel::class.qualifiedName

    override val componentRadius: Float
        get() = windowHeight / sqrt(2.0f)
    override val componentWidth: Float
        get() = 150.0f


    private val vertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(vertices.size * 4).run() {
        order(ByteOrder.nativeOrder())
        asFloatBuffer().apply {
            put(Companion.vertices)
            rewind()
        }
    }
    private lateinit var shaderProgram: ShaderProgram
    private var cursor: Pair<Float, Float>? = null

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

        // uniform: iResolution
        shaderProgram.getUniformLocation("iResolution").also {
            GLES31.glUniform2f(it, windowWidth, windowHeight)
        }

        // uniform: iInclination
        shaderProgram.getUniformLocation("iInclination").also {
            GLES31.glUniform1f(it, -globalInfo.inclination.toFloat())
        }

        // uniform: iWheelWidth
        shaderProgram.getUniformLocation("iWheelWidth").also {
            GLES31.glUniform1f(it, componentWidth)
        }

        // uniform: iColor
        shaderProgram.getUniformLocation("iColor").also {
            val (r, g, b) = rendererState.brushColor
            GLES31.glUniform3f(it, r, g, b)
        }

        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_FAN, 0, verticesCnt)

        GLES31.glDisableVertexAttribArray(pos)
    }

    override fun onTouchScaled(isOnController: Boolean, r: Float, theta: Float) {
        if (isOnController) {
            cursor = r to theta
            rendererState.brushColor = thetaToColor(theta)
            Log.d(TAG, "onTouchScaled: $theta")
        }
    }

    override fun onScrollScaled(isOnController: Boolean, r: Float, theta: Float, dr: Float, dtheta: Float) {
        if (cursor == null) return
        cursor = r to theta
        rendererState.brushColor = thetaToColor(theta)
        Log.d(TAG, "onScrollScaled: $theta")
    }

    override fun onReleaseScaled(isOnController: Boolean, r: Float, theta: Float) {
        cursor = null
    }

    override fun update() {
        val theta = cursor?.second
        if (theta != null) {
            rendererState.brushColor = thetaToColor(theta)
            Log.d(TAG, "update: $theta")
        }
    }

    override fun onSurfaceCreated() {
        shaderProgram = ShaderProgram.setFragment(fragmentShader).setVertex(vertexShader).build()
    }

    private fun thetaToColor(theta: Float): Triple<Float, Float, Float> {
        val a = 0.95492965855f
        val b = 1.0471975512f
        val theta = ((theta - globalInfo.inclination + TWO_PI) % TWO_PI).toFloat()
        return when {
            theta < b -> Triple(1.0f, a * theta, 0.0f)
            theta < 2.0f * b -> Triple(-a * (theta - 2.0f * b), 1.0f, 0.0f)
            theta < PI -> Triple(0.0f, 1.0f, a * (theta - 2.0f * b))
            theta < 4.0f * b -> Triple(0.0f, -a * (theta - 4.0f * b), 1.0f)
            theta < 5.0f * b -> Triple(a * (theta - 4.0f * b), 0.0f, 1.0f)
            else -> Triple(1.0f, 0.0f, -a * (theta - 6.0f * b))
        }
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
uniform float iWheelWidth;
uniform vec3 iColor;
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
    float wheelWidth = iWheelWidth / iResolution.y;
    float r = length(coordCentered);
    if(r < sqrt2Halved || r > sqrt2Halved + wheelWidth) discard;
    float theta = mod(iInclination + atan2(coordCentered.y, coordCentered.x) + TWO_PI, TWO_PI);
    
    if(r > sqrt2Halved + wheelWidth - BORDER_WIDTH) {
        float ratio = smoothstep(0.0, 1.0, mod(theta, PI * 0.125) / (PI * 0.125));
        float woundRatio = abs(ratio - 0.5) * 2.0;
        vec3 specularColor = mix(vec3(0.1), vec3(.9), woundRatio);
        fragColor = vec4(specularColor, 1.0);
        return;
    }
    
    vec3 color = (theta < b) ? vec3(1., a * theta, 0.)
        : (theta < 2. * b) ? vec3(-a * (theta - 2. * b), 1., 0.)
        : (theta < PI) ? vec3(0., 1., a * (theta - 2. * b))
        : (theta < 4. * b) ? vec3(0., -a * (theta - 4. * b), 1.)
        : (theta < 5. * b) ? vec3(a * (theta - 4. * b), 0., 1.)
        : vec3(1., 0., -a * (theta - 6. * b));
        
    if(r < sqrt2Halved + BORDER_WIDTH) {
        float rDiff = pow((r - sqrt2Halved) / BORDER_WIDTH, 0.2);
        float diff = (1.0 - length((iColor - color) * .5));
        float mixRatio = pow(diff, 15.0) * rDiff;
        
        float ratio = smoothstep(0.0, 1.0, mod(theta, PI * 0.125) / (PI * 0.125));
        float woundRatio = abs(ratio - 0.5) * 2.0;
        vec3 specularColor = mix(vec3(0.3), vec3(.9), woundRatio);
        vec3 mixedColor = mix(specularColor, color, mixRatio);
        
        fragColor = vec4(color * mixRatio, 1.0);
    } else {
        fragColor = vec4(color, 1.);
    }
}
        """
    }
}