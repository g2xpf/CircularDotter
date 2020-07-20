package jp.ac.titech.itpro.sdl.circulardotter.component

import android.util.Log
import jp.ac.titech.itpro.sdl.circulardotter.Component
import jp.ac.titech.itpro.sdl.circulardotter.GlobalInfo
import jp.ac.titech.itpro.sdl.circulardotter.PointerIndex
import jp.ac.titech.itpro.sdl.circulardotter.RendererState
import kotlin.math.atan2
import kotlin.math.sqrt

abstract class CircularComponent(
    globalInfo: GlobalInfo,
    rendererState: RendererState
) : Component(
    globalInfo,
    rendererState
) {
    private val TAG = CircularComponent::class.qualifiedName

    abstract val componentWidth: Float
    abstract val componentRadius: Float

    private var pointerIndex: PointerIndex? = null

    override fun onTouch(pointerIndex: PointerIndex, x: Float, y: Float) {
        if (!canvasIsShown) return
        this.pointerIndex = pointerIndex
        val (r, theta) = toPolarCoord(x, y)
        onTouchScaled(isOnComponent(r), r, theta)
    }

    override fun onScroll(pointerIndex: PointerIndex, x: Float, y: Float, dx: Float, dy: Float) {
        if (!canvasIsShown) return
        val (r, theta) = toPolarCoord(x, y)
        val (dr, dtheta) = toPolarCoord(dx, -dy)
        onScrollScaled(isOnComponent(r), r, theta, dr, dtheta)
    }

    override fun onRelease(pointerIndex: PointerIndex, x: Float, y: Float) {
        if (!canvasIsShown) return
        if (this.pointerIndex == pointerIndex) {
            this.pointerIndex = null
        }
        val (r, theta) = toPolarCoord(x, y)
        onReleaseScaled(isOnComponent(r), r, theta)
    }

    abstract fun onTouchScaled(isOnComponent: Boolean, r: Float, theta: Float)
    abstract fun onScrollScaled(isOnComponent: Boolean, r: Float, theta: Float, dr: Float, dtheta: Float)
    abstract fun onReleaseScaled(isOnComponent: Boolean, r: Float, theta: Float)

    private fun toPolarCoord(x: Float, y: Float): Pair<Float, Float> {
        if ((x == 0.0f) and (y == 0.0f)) {
            0.0 to 0.0
        }
        val (cx, cy) = x - windowWidth * 0.5 to windowHeight * 0.5 - y
        val r = sqrt(cx * cx + cy * cy).toFloat()
        val rScaled = (r - componentRadius) / componentWidth
        val theta = atan2(cy, cx).toFloat()
        return rScaled to theta

    }

    private fun isOnComponent(r: Float): Boolean {
        return (0.0f <= r) and (r <= 1.0f)
    }

}