package jp.ac.titech.itpro.sdl.circulardotter.component.controller

import android.util.Log
import jp.ac.titech.itpro.sdl.circulardotter.Component
import jp.ac.titech.itpro.sdl.circulardotter.GlobalInfo
import jp.ac.titech.itpro.sdl.circulardotter.PointerIndex
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

abstract class ControllerComponent(protected val controllerWidth: Float, globalInfo: GlobalInfo): Component(
    globalInfo
) {
    private val TAG = ControllerComponent::class.qualifiedName

    private var pointerIndex: PointerIndex? = null

    override fun onTouch(pointerIndex: PointerIndex, x: Float, y: Float) {
        this.pointerIndex = pointerIndex
        val (isOnController, r, theta) = toPolarCoord(x, y)
        onTouchScaled(isOnController, r, theta)
    }

    override fun onScroll(pointerIndex: PointerIndex, x: Float, y: Float, dx: Float, dy: Float) {
        if (this.pointerIndex != pointerIndex) return
        val (isOnController, r, theta) = toPolarCoord(x, y)
        onScrollScaled(isOnController, r, theta)
    }

    override fun onRelease(pointerIndex: PointerIndex, x: Float, y: Float) {
        if (this.pointerIndex == pointerIndex) {
            this.pointerIndex = null
        }
        val (isOnController, r, theta) = toPolarCoord(x, y)
        onReleaseScaled(isOnController, r, theta)
    }

    abstract fun onTouchScaled(isOnController: Boolean, r: Float, theta: Float)
    abstract fun onScrollScaled(isOnController: Boolean, r: Float,  theta: Float)
    abstract fun onReleaseScaled(isOnController: Boolean, r: Float,  theta: Float)

    private fun toPolarCoord(x: Float, y: Float): Triple<Boolean, Float, Float> {
        if((x == 0.0f) and (y == 0.0f)) {
            0.0 to 0.0
        }
        val (cx, cy) = x - windowWidth * 0.5 to windowHeight * 0.5 - y
        val r = sqrt(cx * cx + cy * cy).toFloat()
        val controllerInnerRadius = windowHeight / sqrt(2.0f)
        val rScaled = (r - controllerInnerRadius) / controllerWidth
        val theta = atan2(cy, cx).toFloat()
        Log.d(TAG, "rScaled, theta: $rScaled, $theta")
        return Triple((0.0f <= rScaled) and (rScaled <= 1.0f), rScaled, theta)
    }
}