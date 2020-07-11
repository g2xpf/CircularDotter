package jp.ac.titech.itpro.sdl.circulardotter

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import jp.ac.titech.itpro.sdl.circulardotter.component.Canvas

class CDGLSurfaceView(context: Context, attributeSet: AttributeSet) :
    GLSurfaceView(context, attributeSet) {
    private var renderer = Renderer()

    init {
        setEGLContextClientVersion(3)

        super.setEGLConfigChooser(8, 8, 8, 8, 16, 0)

        setRenderer(renderer)

        renderMode = RENDERMODE_WHEN_DIRTY
    }
}