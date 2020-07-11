package jp.ac.titech.itpro.sdl.circulardotter

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet

class CDGLSurfaceView(context: Context, attributeSet: AttributeSet) :
    GLSurfaceView(context, attributeSet) {
    private val renderer: Renderer

    init {
        setEGLContextClientVersion(3)
        renderer = Renderer()

        setRenderer(renderer)

        renderMode = RENDERMODE_WHEN_DIRTY
    }
}