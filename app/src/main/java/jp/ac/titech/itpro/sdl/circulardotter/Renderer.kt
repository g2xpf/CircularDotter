package jp.ac.titech.itpro.sdl.circulardotter

import android.content.ContentResolver
import android.content.Context
import android.graphics.Point
import android.opengl.GLES10.GL_LINE_SMOOTH
import android.opengl.GLES31
import android.opengl.GLSurfaceView
import android.util.Log
import jp.ac.titech.itpro.sdl.circulardotter.component.*
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.sqrt

sealed class SaveImageState {
    object Steady: SaveImageState()
    data class SaveRequested(val destSize: Int): SaveImageState()
}

data class RendererState(
    var canvasMode: CanvasMode = CanvasMode.Uninit,
    var brushColor: Triple<Float, Float, Float> = Triple(1.0f, 0.0f, 0.0f),
    var brushSize: Int = 1,
    var isDrawing: Boolean = false,
    var controllerMode: ControllerMode = ControllerMode.ColorWheel,
    var showGrid: Boolean = true,
    var fillCanvas: Boolean = true,
    var showCentralGrid: Boolean = true,
    var saveImageState: SaveImageState = SaveImageState.Steady,
    var contentResolver: ContentResolver? = null,
    var activity: MainActivity? = null
)

class Renderer : GLSurfaceView.Renderer {
    private val TAG = Renderer::class.qualifiedName
    private var rendererState = RendererState()
    private var globalInfo = GlobalInfo(0.0, 0.0f)
    private val components = mutableListOf<Component>()

    init {
        components.add(Buttons(globalInfo, rendererState))
        components.add(Controller(globalInfo, rendererState))
        components.add(DrawButton(globalInfo, rendererState))
        components.add(Canvas(globalInfo, rendererState))
    }

    fun setActivityInfo(activity: MainActivity?, contentResolver: ContentResolver) {
        this.rendererState.activity = activity
        this.rendererState.contentResolver = contentResolver
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
        for (c in components) {
            c.onTouch(pointerIndex, x, y)
        }
    }

    fun onRelease(pointerIndex: PointerIndex, x: Float, y: Float) {
        for (c in components) {
            c.onRelease(pointerIndex, x, y)
        }
    }

    fun onScroll(pointerIndex: PointerIndex, x: Float, y: Float, dx: Float, dy: Float) {
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