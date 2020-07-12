package jp.ac.titech.itpro.sdl.circulardotter

class GlobalInfo (var inclination: Double, var time: Float){}

interface Component {
    fun draw(globalInfo: GlobalInfo)
    fun onWindowResized(width: Int, height: Int)
}