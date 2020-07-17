package jp.ac.titech.itpro.sdl.circulardotter.component

import android.opengl.GLES31
import jp.ac.titech.itpro.sdl.circulardotter.Component
import jp.ac.titech.itpro.sdl.circulardotter.GlobalInfo
import jp.ac.titech.itpro.sdl.circulardotter.gl.ShaderProgram
import jp.ac.titech.itpro.sdl.circulardotter.gl.build
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

enum class ControllerMode {
    ColorWheel
}

class Controller: Component {
    override var windowWidth = 1.0f
    override var windowHeight = 1.0f

    private var mode = ControllerMode.ColorWheel
    private val vertexBuffer: FloatBuffer
    private val shaderProgram: ShaderProgram

    init {
        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4).run() {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(vertices)
                rewind()
            }
        }

        shaderProgram = ShaderProgram.setFragment(fragmentShader).setVertex(vertexShader).build()
    }

    override fun draw(globalInfo: GlobalInfo) {
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

        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_FAN, 0, verticesCnt)

        GLES31.glDisableVertexAttribArray(pos)
    }

    override fun onWindowResized(width: Int, height: Int) {
        windowWidth = width.toFloat()
        windowHeight = height.toFloat()
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
uniform float iInclination;
uniform vec2 iResolution;
out vec4 fragColor;

const float sqrt2Halved = 0.70710678118;
const float RING_WIDTH = 0.2;
const float PI = 3.14159265358;
const float TWO_PI = 6.28318530718;

const float a = 0.95492965855;
const float b = 1.0471975512;

float atan2(in float y, in float x) {
    return x == 0.0 ? sign(y)*PI*.5 : atan(y, x);
}

void main() {
    vec2 coordCentered = vec2(coord.x * iResolution.x / iResolution.y, coord.y);
    if(length(coordCentered) < sqrt2Halved || length(coordCentered) > sqrt2Halved + RING_WIDTH) discard;
    float theta = mod(iInclination + atan2(coordCentered.y, coordCentered.x) + TWO_PI, TWO_PI);
    
    vec3 color = (theta < b) ? vec3(1., a * theta, 0.)
        : (theta < 2. * b) ? vec3(-a * (theta - 2. * b), 1., 0.)
        : (theta < PI) ? vec3(0., 1., a * (theta - 2. * b))
        : (theta < 4. * b) ? vec3(0., -a * (theta - 4. * b), 1.)
        : (theta < 5. * b) ? vec3(a * (theta - 4. * b), 0., 1.)
        : vec3(1., 0., -a * (theta - 6. * b));
    fragColor = vec4(color, 1.);
}
        """
    }
}