package jp.ac.titech.itpro.sdl.circulardotter.component

import jp.ac.titech.itpro.sdl.circulardotter.Component
import jp.ac.titech.itpro.sdl.circulardotter.GlobalInfo
import jp.ac.titech.itpro.sdl.circulardotter.PointerIndex
import jp.ac.titech.itpro.sdl.circulardotter.RendererState
import jp.ac.titech.itpro.sdl.circulardotter.component.controller.ColorWheel

enum class ControllerMode {
    ColorWheel
}

class Controller(globalInfo: GlobalInfo, private var rendererState: RendererState) : Component(globalInfo) {
    private val controllerWidth = 150.0f

    private var mode = ControllerMode.ColorWheel

    private val colorWheel = ColorWheel(controllerWidth, globalInfo, rendererState)

    override fun draw() {
        super.draw()
        when (mode) {
            ControllerMode.ColorWheel -> {
                colorWheel.draw()
            }
        }
    }

    override fun onWindowResized(width: Int, height: Int) {
        super.onWindowResized(width, height)
        colorWheel.onWindowResized(width, height)
    }

    override fun onTouch(pointerIndex: PointerIndex, x: Float, y: Float) {
        when(mode) {
            ControllerMode.ColorWheel -> {
                colorWheel.onTouch(pointerIndex, x, y)
            }
        }
    }

    override fun onScroll(pointerIndex: PointerIndex, x: Float, y: Float, dx: Float, dy: Float) {
        when(mode) {
            ControllerMode.ColorWheel -> {
                colorWheel.onScroll(pointerIndex, x, y, dx, dy)
            }
        }
    }

    override fun onRelease(pointerIndex: PointerIndex, x: Float, y: Float) {
        when(mode) {
            ControllerMode.ColorWheel -> {
                colorWheel.onRelease(pointerIndex, x, y)
            }
        }
    }
}