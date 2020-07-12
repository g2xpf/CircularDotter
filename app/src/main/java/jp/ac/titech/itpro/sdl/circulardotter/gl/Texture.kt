package jp.ac.titech.itpro.sdl.circulardotter.gl

import android.opengl.GLES31
import android.util.Log
import java.nio.Buffer
import java.nio.IntBuffer

class Texture(
    private val width: Int,
    private val height: Int,
    private val data: Buffer
) {
    private val TAG = Texture::class.qualifiedName
    private val texture: Int;
    private val textureCount: Int;

    init {
        textureCount = totalTextureCount++
        val buf = IntBuffer.allocate(1)

        GLES31.glActiveTexture(GLES31.GL_TEXTURE0 + textureCount)
        GLES31.glGenTextures(1, buf);
        texture = buf[0];
        Log.d(TAG, "texture: $texture")
        GLES31.glEnable(GLES31.GL_TEXTURE_2D)
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture)

        GLES31.glTexImage2D(GLES31.GL_TEXTURE_2D, 0, GLES31.GL_RGB, width, height, 0, GLES31.GL_RGB, GLES31.GL_UNSIGNED_BYTE, data);
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_NEAREST);
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_NEAREST);
        GLES31.glDisable(GLES31.GL_TEXTURE_2D)
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, 0)
    }

    fun use(): Int {
        GLES31.glActiveTexture(GLES31.GL_TEXTURE0 + textureCount)
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture)
        return textureCount
    }

    fun write(x: Int, y: Int, w: Int, h: Int, data: Buffer) {
        GLES31.glActiveTexture(GLES31.GL_TEXTURE0 + textureCount)
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture)
        GLES31.glTexSubImage2D(GLES31.GL_TEXTURE_2D, 0, x, y, w, h, GLES31.GL_UNSIGNED_BYTE, GLES31.GL_RGB, data)

        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, 0)
    }

    companion object {
        var totalTextureCount = 0
    }
}