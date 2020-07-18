package jp.ac.titech.itpro.sdl.circulardotter

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import kotlin.math.abs
import kotlin.math.min

class CDGLSurfaceView(context: Context, attributeSet: AttributeSet) :
    GLSurfaceView(context, attributeSet) {
    private val TAG = CDGLSurfaceView::class.qualifiedName

    private var pointIdGap = 0

    private var prevX = floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f)
    private var prevY = floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f)
    private var renderer = Renderer()

    init {
        setEGLContextClientVersion(3)

        super.setEGLConfigChooser(8, 8, 8, 8, 16, 0)

        setRenderer(renderer)

        // renderMode = RENDERMODE_WHEN_DIRTY
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {

        val actionIndex = event.actionIndex

        Log.d(TAG, "actionIndex: $actionIndex")
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                performClick()
                pointIdGap = 0
                val (x, y) = event.getX(actionIndex) to event.getY(actionIndex)
                renderer.onTouch(actionIndex, x, y)
                prevX[actionIndex] = x
                prevY[actionIndex] = y
            }
            MotionEvent.ACTION_POINTER_UP -> {
                Log.d(TAG, "action pointer up: $actionIndex")
                if (actionIndex == 0) {
                    pointIdGap = 1
                }
                val (x, y) = event.getX(actionIndex) to event.getY(actionIndex)
                renderer.onRelease(actionIndex, x, y)
            }

            MotionEvent.ACTION_UP -> {
                Log.d(TAG, "action up: $actionIndex")
                val (x, y) = event.getX(actionIndex) to event.getY(actionIndex)
                renderer.onRelease(actionIndex + pointIdGap, x, y)
            }

            MotionEvent.ACTION_CANCEL -> {
                Log.w(TAG, "action was cancelled")
            }

            MotionEvent.ACTION_MOVE -> {
                Log.d(TAG, "pointerCount: ${event.pointerCount}")
                for (index in 0 until min(4, event.pointerCount)) {
                    val memoryIndex = index + pointIdGap
                    val (x, y) = event.getX(index) to event.getY(index)
                    val (dx, dy) = x - prevX[memoryIndex] to y - prevY[memoryIndex]
                    Log.d(TAG, "index $memoryIndex: {")
                    Log.d(TAG, "\tprevX, prevY = ${prevX[memoryIndex]}, ${prevY[memoryIndex]}")
                    Log.d(TAG, "\tx, y: $x, $y")
                    Log.d(TAG, "\tdx, dy: $dx, $dy")
                    Log.d(TAG, "}")

                    // only moving cursor
                    if (abs(dx) + abs(dy) > 0.0) {
                        renderer.onScroll(memoryIndex, x, y, dx, dy)
                        prevX[memoryIndex] = x
                        prevY[memoryIndex] = y
                    }
                }
            }
        }
        requestRender()
        return true
    }

    fun setGlobalInfo(globalInfo: GlobalInfo) {
        renderer.setGlobalInfo(globalInfo)
    }
}