package jp.ac.titech.itpro.sdl.circulardotter

import android.app.Activity
import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.MotionEvent

class CDGLSurfaceView(context: Context, attributeSet: AttributeSet) :
    GLSurfaceView(context, attributeSet) {
    private var prevX: Float = 0f;
    private var prevY: Float = 0f;
    private var renderer = Renderer()

    init {
        setEGLContextClientVersion(3)

        super.setEGLConfigChooser(8, 8, 8, 8, 16, 0)

        setRenderer(renderer)

        renderMode = RENDERMODE_WHEN_DIRTY
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x: Float = event.x
        val y: Float = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.onTouch(x, y)
                prevX = x
                prevY = y
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = x - prevX;
                val dy = y - prevY;
                renderer.onScroll(dx, dy)
                prevX = x
                prevY = y
            }
        }

        requestRender()
        return true
    }
}