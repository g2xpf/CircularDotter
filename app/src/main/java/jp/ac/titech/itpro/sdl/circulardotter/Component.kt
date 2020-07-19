package jp.ac.titech.itpro.sdl.circulardotter

data class GlobalInfo(var inclination: Double, var time: Float)
typealias PointerIndex = Int

abstract class Component(
    protected var globalInfo: GlobalInfo,
    protected var rendererState: RendererState
) {
    protected var windowWidth = 1.0f
    protected var windowHeight = 1.0f

    open fun draw() {
        update()
    }

    open fun update() {}
    open fun onWindowResized(width: Int, height: Int) {
        windowWidth = width.toFloat()
        windowHeight = height.toFloat()
    }

    open fun onScroll(pointerIndex: PointerIndex, x: Float, y: Float, dx: Float, dy: Float) {}
    open fun onTouch(pointerIndex: PointerIndex, x: Float, y: Float) {}
    open fun onRelease(pointerIndex: PointerIndex, x: Float, y: Float) {}
    open fun onSurfaceCreated() {}
}