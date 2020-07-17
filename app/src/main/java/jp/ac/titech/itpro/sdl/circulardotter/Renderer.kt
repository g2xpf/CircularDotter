package jp.ac.titech.itpro.sdl.circulardotter

import android.graphics.Point
import android.opengl.GLES10.GL_LINE_SMOOTH
import android.opengl.GLES31
import android.opengl.GLSurfaceView
import android.util.Log
import jp.ac.titech.itpro.sdl.circulardotter.component.Canvas
import jp.ac.titech.itpro.sdl.circulardotter.component.Controller
import jp.ac.titech.itpro.sdl.circulardotter.component.DrawButton
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Renderer : GLSurfaceView.Renderer {
    private val TAG = Renderer::class.qualifiedName

    private lateinit var canvas: Canvas
    private lateinit var drawButton: DrawButton
    private lateinit var controller: Controller

    private var width = 0
    private var height = 0

    private var globalInfo = GlobalInfo(0.0, 0.0f)

    override fun onDrawFrame(unused: GL10) {
        // Log.d(TAG, "onDrawFrame")
        GLES31.glClearColor(0.3f, 0.3f, 0.3f, 1.0f)
        GLES31.glClear(GL10.GL_COLOR_BUFFER_BIT or GL10.GL_DEPTH_BUFFER_BIT)

        canvas.draw(globalInfo)
        drawButton.draw(globalInfo)
        controller.draw(globalInfo)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceChanged")

        canvas.onWindowResized(width, height)
        drawButton.onWindowResized(width, height)
        controller.onWindowResized(width, height)

        GLES31.glViewport(0, 0, width, height)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d(TAG, "onSurfaceCreated")
        canvas = Canvas()
        drawButton = DrawButton()
        controller = Controller()
    }

    fun onTouch(pointerIndex: PointerIndex, x: Float, y: Float) {
        Log.d(TAG, "touched: ($pointerIndex, $x, $y)")
        drawButton.onTouch(pointerIndex, x, y)
        drawButton.send(canvas)
        canvas.onTouch(pointerIndex, x, y)
    }

    fun onRelease(pointerIndex: PointerIndex, x: Float, y: Float) {
        Log.d(TAG, "released: ($pointerIndex, $x, $y)")
        drawButton.onRelease(pointerIndex, x, y)
        drawButton.send(canvas)
        canvas.onRelease(pointerIndex, x, y)
    }

    fun onScroll(pointerIndex: PointerIndex, dx: Float, dy: Float) {
        Log.d(TAG, "scrolled: ($pointerIndex, $dx, $dy)")
        // y reversed
        drawButton.onScroll(pointerIndex, dx, -dy)
        drawButton.send(canvas)
        canvas.onScroll(pointerIndex, dx, -dy)
    }

    fun setGlobalInfo(globalInfo: GlobalInfo) {
        this.globalInfo = globalInfo
    }
}