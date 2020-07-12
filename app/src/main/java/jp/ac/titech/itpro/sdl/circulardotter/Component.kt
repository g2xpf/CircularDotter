package jp.ac.titech.itpro.sdl.circulardotter

interface Component {
    fun draw()
    fun onWindowResized(width: Int, height: Int)
}