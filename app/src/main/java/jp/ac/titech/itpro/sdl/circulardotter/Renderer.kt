package jp.ac.titech.itpro.sdl.circulardotter

import android.opengl.GLES31
import android.opengl.GLSurfaceView
import android.util.Log
import jp.ac.titech.itpro.sdl.circulardotter.component.Canvas
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Renderer : GLSurfaceView.Renderer {
    private val TAG = Renderer::class.qualifiedName
    private val components: HashMap<String, Component> = hashMapOf()
    private var width = 0;
    private var height = 0;

    fun addComponent(name: String, component: Component) {
        components[name] = component
    }

    override fun onDrawFrame(unused: GL10) {
        Log.d(TAG, "onDrawFrame")
        GLES31.glClearColor(1.0f, 0.0f, 0.0f, 1.0f)
        GLES31.glClear(GL10.GL_COLOR_BUFFER_BIT or GL10.GL_DEPTH_BUFFER_BIT)

        for ((_, component) in components) {
            component.draw()
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceChanged")

        for ((_, component) in components) {
            component.onWindowResized(width, height)
        }

        GLES31.glViewport(0, 0, width, height)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d(TAG, "onSurfaceCreated")
        addComponent("canvas", Canvas())
    }
}