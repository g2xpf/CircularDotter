package jp.ac.titech.itpro.sdl.circulardotter.component

import jp.ac.titech.itpro.sdl.circulardotter.GlobalInfo
import jp.ac.titech.itpro.sdl.circulardotter.RendererState
import jp.ac.titech.itpro.sdl.circulardotter.component.controller.ColorWheel
import kotlin.math.sqrt

enum class ControllerMode {
    ColorWheel
}

class Controller(globalInfo: GlobalInfo, rendererState: RendererState) :
    CircularComponent(globalInfo, rendererState) {
    private var mode = ControllerMode.ColorWheel

    override val componentRadius: Float
        get() = windowHeight / sqrt(2.0f)
    override val componentWidth: Float
        get() = 150.0f

    private val colorWheel =
        ColorWheel(globalInfo, rendererState)

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

    override fun onTouchScaled(isOnComponent: Boolean, r: Float, theta: Float) {
        when (mode) {
            ControllerMode.ColorWheel -> {
                colorWheel.onTouchScaled(isOnComponent, r, theta)
            }
        }
    }

    override fun onScrollScaled(isOnComponent: Boolean, r: Float, theta: Float, dr: Float, dtheta: Float) {
        when (mode) {
            ControllerMode.ColorWheel -> {
                colorWheel.onScrollScaled(isOnComponent, r, theta, dr, dtheta)
            }
        }
    }

    override fun onReleaseScaled(isOnComponent: Boolean, r: Float, theta: Float) {
        when (mode) {
            ControllerMode.ColorWheel -> {
                colorWheel.onReleaseScaled(isOnComponent, r, theta)
            }
        }
    }

    override fun onSurfaceCreated() {
        colorWheel.onSurfaceCreated()
    }
}