package jp.ac.titech.itpro.sdl.circulardotter.component

import android.graphics.BitmapFactory
import android.opengl.GLES31
import jp.ac.titech.itpro.sdl.circulardotter.GlobalInfo
import jp.ac.titech.itpro.sdl.circulardotter.R
import jp.ac.titech.itpro.sdl.circulardotter.RendererState
import jp.ac.titech.itpro.sdl.circulardotter.gl.ImageTexture
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
    private lateinit var buttonTextures: Array<ImageTexture>

    override val componentRadius: Float
        get() = windowHeight / sqrt(2.0f) + 200.0f
    override val componentWidth: Float
        get() = 300.0f

    private val vertexBuffer: FloatBuffer =
        ByteBuffer.allocateDirect(vertices.size * 4).run() {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(vertices)
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
                DIMENSION,
                GLES31.GL_FLOAT,
                false,
                STRIDE,
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

        // iShouldOverlay
        shaderProgram.getUniformLocation("iShouldOverlay").also {
            GLES31.glUniform1i(it, if (!canvasIsShown) 1 else 0)
        }

        for (i in buttonTextures.indices) {
            shaderProgram.getUniformLocation("iTexture$i").also {
                GLES31.glUniform1i(it, buttonTextures[i].use())
            }
        }

        // iPushed
        shaderProgram.getUniformLocation("iPushed").also {
            GLES31.glUniform1i(it, if (rendererState.isDrawing) 1 else 0)
        }


        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_FAN, 0, verticesCnt)

        GLES31.glDisableVertexAttribArray(pos)
    }

    override fun onSurfaceCreated() {
        shaderProgram = ShaderProgram.setFragment(fragmentShader).setVertex(vertexShader).build()

        buttonTextures = arrayOf(
            R.drawable.new_icon,
            R.drawable.secondary_border,
            R.drawable.primary_border,
            R.drawable.fill,
            R.drawable.save,
            R.drawable.pencil_plus,
            R.drawable.dropper,
            R.drawable.eraser
        ).map {
            val bitmap = BitmapFactory.decodeResource(rendererState.activity?.resources, it)
            ImageTexture(bitmap)
        }.toTypedArray()
        buttonTextures.forEach { it.initialize() }
    }

    override fun onTouchScaled(isOnComponent: Boolean, r: Float, theta: Float) {
        if (!isOnComponent) return
        val angle = (theta - globalInfo.inclination + TWO_PI) % TWO_PI
        val kind = (angle * SEPARATE_NUM.toFloat() / TWO_PI).toInt()
        when (kind) {
            0 -> rendererState.canvasMode = CanvasMode.Uninit
            1 -> rendererState.showGrid = !rendererState.showGrid
            2 -> rendererState.showCentralGrid = !rendererState.showCentralGrid
            3 -> rendererState.fillCanvas = true
            4 -> rendererState.canvasMode =
                if (rendererState.canvasMode == CanvasMode.SaveRequested) CanvasMode.Write else CanvasMode.SaveRequested
            5 -> rendererState.brushSize =
                if (rendererState.brushSize >= 11) 1 else rendererState.brushSize + 2
            6 -> rendererState.canvasMode =
                if (rendererState.canvasMode == CanvasMode.Write) CanvasMode.Read else CanvasMode.Write
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

    override fun onReleaseScaled(isOnComponent: Boolean, r: Float, theta: Float) {
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
        val fragmentShader = """
#version 310 es
precision mediump float;

in vec2 coord;
uniform float iInclination;
uniform vec2 iResolution;
uniform float iAreaWidth;
uniform float iInnerWidth;
uniform int iSeparateNum;
uniform int iShouldOverlay;
uniform sampler2D iTexture0;
uniform sampler2D iTexture1;
uniform sampler2D iTexture2;
uniform sampler2D iTexture3;
uniform sampler2D iTexture4;
uniform sampler2D iTexture5;
uniform sampler2D iTexture6;
uniform sampler2D iTexture7;

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

vec2 rotate(vec2 p, float theta) {
    float c = cos(theta);
    float s = sin(theta);
    return mat2(c, s, -s, c) * p;
}

void main() {
    vec2 coordCentered = vec2(coord.x * iResolution.x / iResolution.y, coord.y);
    float innerWidth = iInnerWidth / iResolution.y;
    float areaWidth = iAreaWidth / iResolution.y;
    float outerR = innerWidth + areaWidth;
    float r = length(coordCentered);
    // if(r < innerWidth || r > outerR) discard;
    float theta = mod(atan2(coordCentered.y, coordCentered.x) + iInclination + TWO_PI, TWO_PI);
    float borderAngle = TWO_PI / float(iSeparateNum);
    float modAngle = mod(theta, borderAngle);
    int buttonKind = int(floor(theta / borderAngle));
    vec3 color = vec3(0.);
    bool matched = false;
    """ + IntArray(SEPARATE_NUM) { it }.map { i ->
            """
        do {
            if(matched) break;
            float r =${if ((i shr 2) and 1 == 1) 0.7f else 0.0f};
            float g =${if ((i shr 1) and 1 == 1) 0.7f else 0.0f};
            float b =${if ((i shr 0) and 1 == 1) 0.7f else 0.0f};
            if ($i == buttonKind) {
                matched = true;
                color = vec3(r, g, b);
            }
            
            vec2 center = rotate(((coordCentered - rotate(
                vec2(innerWidth + areaWidth / 4.0, 0.),
                -iInclination + float($i) * borderAngle + borderAngle / 2.0
            ))) / ((sqrt2Halved - 0.5) / 2.0), PI * .5 -(-iInclination + float($i) * borderAngle + borderAngle / 2.0));

            if (all(lessThan(abs(center), vec2(0.5)))) {
                vec2 centerFlipped = vec2(center.x, -center.y);
                vec4 tex = texture(iTexture$i, centerFlipped + vec2(.5));
                if(tex.a > 0.01) {
                    color = vec3(tex.a);
                }
            }
        } while(false);
        """
        }.fold("", { acc, init -> acc + init }) +
                """
    if(modAngle < BORDER_WIDTH || modAngle > borderAngle - BORDER_WIDTH) {
        color = vec3(.1);
    }
    
    fragColor = vec4(iShouldOverlay > 0 ? max(color - vec3(.5), vec3(.0)): color, 1.0);
}
        """
    }
}