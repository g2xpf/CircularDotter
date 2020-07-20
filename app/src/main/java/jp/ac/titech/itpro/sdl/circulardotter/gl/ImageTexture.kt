package jp.ac.titech.itpro.sdl.circulardotter.gl

import android.graphics.Bitmap
import android.opengl.GLES31
import android.opengl.GLUtils
import android.util.Log
import jp.ac.titech.itpro.sdl.circulardotter.gl.MutableTexture.Companion.totalTextureCount
import java.nio.IntBuffer

class ImageTexture(
    private val bmp: Bitmap
    ) {
    private var texture: Int = 0
    private val textureCount: Int = totalTextureCount++

    fun initialize() {
        val buf = IntBuffer.allocate(1)

        GLES31.glActiveTexture(GLES31.GL_TEXTURE0 + textureCount)
        GLES31.glGenTextures(1, buf)
        texture = buf[0]
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture)

        GLES31.glPixelStorei(GLES31.GL_UNPACK_ALIGNMENT, 1)
        GLUtils.texImage2D(GLES31.GL_TEXTURE_2D, 0, bmp, 0)
        GLES31.glTexParameteri(
            GLES31.GL_TEXTURE_2D,
            GLES31.GL_TEXTURE_MAG_FILTER,
            GLES31.GL_NEAREST
        )
        GLES31.glTexParameteri(
            GLES31.GL_TEXTURE_2D,
            GLES31.GL_TEXTURE_MIN_FILTER,
            GLES31.GL_NEAREST
        )
        GLES31.glGenerateMipmap(GLES31.GL_TEXTURE_2D)
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, 0)
    }

    fun use(): Int {
        GLES31.glActiveTexture(GLES31.GL_TEXTURE0 + textureCount)
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture)
        return textureCount
    }

    companion object {
        val TAG = ImageTexture::class.qualifiedName
    }
}