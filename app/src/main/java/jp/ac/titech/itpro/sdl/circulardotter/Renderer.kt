package jp.ac.titech.itpro.sdl.circulardotter

import android.graphics.Point
import android.opengl.GLES10.GL_LINE_SMOOTH
import android.opengl.GLES31
import android.opengl.GLSurfaceView
import android.util.Log
import jp.ac.titech.itpro.sdl.circulardotter.component.Buttons
import jp.ac.titech.itpro.sdl.circulardotter.component.Canvas
import jp.ac.titech.itpro.sdl.circulardotter.component.Controller
import jp.ac.titech.itpro.sdl.circulardotter.component.DrawButton
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

data class RendererState (
    var brushColor: Triple<Float, Float, Float>,
    var isDrawing: Boolean
)

class Renderer : GLSurfaceView.Renderer {
    private val TAG = Renderer::class.qualifiedName

    private var rendererState = RendererState(Triple(0.0f, 0.0f, 0.0f), false)

    private var globalInfo = GlobalInfo(0.0, 0.0f)

    private val components = mutableListOf<Component>()

    private var width = 0
    private var height = 0

    init {
        components.add(Buttons(globalInfo, rendererState))
        components.add(Controller(globalInfo, rendererState))
        components.add(DrawButton(globalInfo, rendererState))
        components.add(Canvas(globalInfo, rendererState))
    }

    override fun onDrawFrame(unused: GL10) {
        GLES31.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GLES31.glClear(GL10.GL_COLOR_BUFFER_BIT or GL10.GL_DEPTH_BUFFER_BIT)

        for (c in components) {
            c.draw()
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceChanged")

        for (c in components) {
            c.onWindowResized(width, height)
        }

        GLES31.glViewport(0, 0, width, height)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d(TAG, "onSurfaceCreated")
        for (c in components) {
            c.onSurfaceCreated()
        }

    }

    fun onTouch(pointerIndex: PointerIndex, x: Float, y: Float) {
        Log.d(TAG, "touched: ($pointerIndex, $x, $y)")
        for (c in components) {
            c.onTouch(pointerIndex, x, y)
        }
    }

    fun onRelease(pointerIndex: PointerIndex, x: Float, y: Float) {
        Log.d(TAG, "released: ($pointerIndex, $x, $y)")
        for (c in components) {
            c.onRelease(pointerIndex, x, y)
        }
    }

    fun onScroll(pointerIndex: PointerIndex, x: Float, y: Float, dx: Float, dy: Float) {
        Log.d(TAG, "scrolled: ($pointerIndex, $dx, $dy)")
        // y reversed
        for (c in components) {
            c.onScroll(pointerIndex, x, y, dx, -dy)
        }
    }

    fun setGlobalInfo(globalInfo: GlobalInfo) {
        this.globalInfo.time = globalInfo.time
        this.globalInfo.inclination = globalInfo.inclination
    }
}