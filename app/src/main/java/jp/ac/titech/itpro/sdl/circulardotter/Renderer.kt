package jp.ac.titech.itpro.sdl.circulardotter

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

    fun onTouched(actionIndex: Int, x: Float, y: Float) {
        // 色の取得、塗り
        drawButton.onTouched(actionIndex, x, y)
        if(actionIndex > 0) {
            // canvas.touched = true
            // canvas.requestDraw()
        }
    }

    fun onReleased(actionIndex: Int, x: Float, y: Float) {
        drawButton.onReleased(actionIndex, x, y)
        if(actionIndex > 0) {
            // canvas.touched = false
        }
    }

    fun onScroll(dx: Float, dy: Float) {
        // y reversed
        canvas.moveCursor(dx, -dy)
        canvas.requestDraw()
    }

    fun setGlobalInfo(globalInfo: GlobalInfo) {
        this.globalInfo = globalInfo
    }
}