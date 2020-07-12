package jp.ac.titech.itpro.sdl.circulardotter

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent

class CDGLSurfaceView(context: Context, attributeSet: AttributeSet) :
    GLSurfaceView(context, attributeSet) {
    private var prevX = floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f);
    private var prevY = floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f);
    private var renderer = Renderer()

    init {
        setEGLContextClientVersion(3)

        super.setEGLConfigChooser(8, 8, 8, 8, 16, 0)

        setRenderer(renderer)

        renderMode = RENDERMODE_WHEN_DIRTY
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val actionIndex = event.actionIndex
        val x = event.getX(actionIndex)
        val y = event.getY(actionIndex)

        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN  -> {
                renderer.onTouched(actionIndex, x, y)
                prevX[actionIndex] = x
                prevY[actionIndex] = y
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                renderer.onReleased(actionIndex, x, y)
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - prevX[actionIndex]
                val dy = event.y - prevY[actionIndex]
                renderer.onScroll(dx, dy)
                prevX[actionIndex] = x
                prevY[actionIndex] = y
            }
        }

        requestRender()
        return true
    }
}