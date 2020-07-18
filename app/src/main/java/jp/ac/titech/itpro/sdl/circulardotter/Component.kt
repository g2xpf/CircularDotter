package jp.ac.titech.itpro.sdl.circulardotter

class GlobalInfo (var inclination: Double, var time: Float){}
typealias PointerIndex = Int

interface Component {
    var windowWidth: Float
    var windowHeight: Float

    fun draw(globalInfo: GlobalInfo) {}
    fun onWindowResized(width: Int, height: Int) {
        windowWidth = width.toFloat()
        windowHeight = height.toFloat()
    }

    fun onScroll(pointerIndex: PointerIndex, dx: Float, dy: Float) {}
    fun onTouch(pointerIndex: PointerIndex, x: Float, y: Float) {}
    fun onRelease(pointerIndex: PointerIndex, x: Float, y: Float) {}
}