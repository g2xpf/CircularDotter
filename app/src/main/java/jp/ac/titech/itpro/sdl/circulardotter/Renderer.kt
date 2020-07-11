package jp.ac.titech.itpro.sdl.circulardotter

import android.opengl.GLSurfaceView
import android.util.Log
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Renderer : GLSurfaceView.Renderer {
    private val TAG = Renderer::class.qualifiedName
    override fun onDrawFrame(gl: GL10?) {
        Log.d(TAG, "onDrawFrame")
        gl?.glClearColor(1.0f, 0.0f, 0.0f, 1.0f)
        gl?.glClear(GL10.GL_COLOR_BUFFER_BIT.or(GL10.GL_DEPTH_BUFFER_BIT))
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
    }
}