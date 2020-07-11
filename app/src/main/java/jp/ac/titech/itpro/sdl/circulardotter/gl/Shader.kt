package jp.ac.titech.itpro.sdl.circulardotter.gl

import android.opengl.GLES31
import android.util.Log

class ShaderProgram constructor(
    private val vertex: Int,
    private val fragment: Int,
    private val program: Int
) {
    abstract class NotSet
    abstract class Set

    companion object : ShaderProgramBuilder<NotSet, NotSet>()

    fun use() {
        GLES31.glUseProgram(program)
    }

    fun getAttribLocation(attribName: String): Int {
        return GLES31.glGetAttribLocation(program, attribName)
    }

    fun getUniformLocation(uniformName: String): Int {
        return GLES31.glGetUniformLocation(program, uniformName)
    }

    open class ShaderProgramBuilder<Vertex, Fragment> internal constructor(
        internal var vertex: Int = 0,
        internal var fragment: Int = 0
    ) {
        internal val TAG = ShaderProgramBuilder::class.qualifiedName
        fun setFragment(shaderSrc: String): ShaderProgramBuilder<Vertex, Set> {
            fragment = load(GLES31.GL_FRAGMENT_SHADER, shaderSrc)
            Log.d(TAG, "fragment: $fragment")
            return ShaderProgramBuilder(vertex, fragment)
        }

        fun setVertex(shaderSrc: String): ShaderProgramBuilder<Set, Fragment> {
            vertex = load(GLES31.GL_VERTEX_SHADER, shaderSrc)
            Log.d(TAG, "vertex: $vertex")
            return ShaderProgramBuilder(vertex, fragment)
        }

        private fun load(type: Int, shaderSrc: String): Int {
            return GLES31.glCreateShader(type).also { shader ->
                Log.d(TAG, "createShader: $shader")
                if (shader == 0) {
                    Log.d(TAG, "error: " + GLES31.glGetError())
                }
                GLES31.glShaderSource(shader, shaderSrc)
                GLES31.glCompileShader(shader)
            }
        }
    }

}

fun ShaderProgram.ShaderProgramBuilder<ShaderProgram.Set, ShaderProgram.Set>.build(): ShaderProgram {
    val program = GLES31.glCreateProgram().also {
        GLES31.glAttachShader(it, vertex)
        GLES31.glAttachShader(it, fragment)
        GLES31.glLinkProgram(it)
    }
    Log.d(TAG, "program: $program")
    return ShaderProgram(vertex, fragment, program)
}
