package jp.ac.titech.itpro.sdl.circulardotter.gl

import android.opengl.GLES31
import android.util.Log
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer
import java.util.*
import kotlin.collections.ArrayDeque

class Texture(
    private val width: Int,
    private val height: Int,
    private val data: Buffer,
    private val colorUpdateQueue: LinkedList<CellInfo> = LinkedList(),
    private var prevCellInfo: CellInfo = CellInfo(-1 to -1, -1 to -1, Triple(0, 0, 0))
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
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture)

        GLES31.glPixelStorei(GLES31.GL_UNPACK_ALIGNMENT, 1);
        GLES31.glTexImage2D(
            GLES31.GL_TEXTURE_2D,
            0,
            GLES31.GL_RGB,
            width,
            height,
            0,
            GLES31.GL_RGB,
            GLES31.GL_UNSIGNED_BYTE,
            data
        )
        Log.d(TAG, "error: " + GLES31.glGetError())
        GLES31.glTexParameteri(
            GLES31.GL_TEXTURE_2D,
            GLES31.GL_TEXTURE_MAG_FILTER,
            GLES31.GL_NEAREST
        );
        GLES31.glTexParameteri(
            GLES31.GL_TEXTURE_2D,
            GLES31.GL_TEXTURE_MIN_FILTER,
            GLES31.GL_NEAREST
        );
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, 0)
    }

    fun use(): Int {
        GLES31.glActiveTexture(GLES31.GL_TEXTURE0 + textureCount)
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture)

        if (colorUpdateQueue.size > 0) {
            val buffer = ByteBuffer.allocateDirect(3).run() {
                order(ByteOrder.nativeOrder())
            }

            while (colorUpdateQueue.size > 0) {
                val cellInfo = colorUpdateQueue.pop()
                val (x, y) = cellInfo.coord
                val (w, h) = cellInfo.size
                val (r, g, b) = cellInfo.color
                buffer.put(r)
                buffer.put(g)
                buffer.put(b)
                buffer.rewind()
                GLES31.glTexSubImage2D(
                    GLES31.GL_TEXTURE_2D,
                    0,
                    x,
                    y,
                    w,
                    h,
                    GLES31.GL_RGB,
                    GLES31.GL_UNSIGNED_BYTE,
                    buffer
                )
                buffer.rewind()
            }
        }
        return textureCount
    }

    fun write(x: Int, y: Int, w: Int, h: Int, data: Triple<Float, Float, Float>) {
        val (r, g, b) = data
        val newCellInfo = CellInfo(
                Pair(x, y),
                Pair(w, h),
                Triple(
                    (r * 255).toInt().toByte(),
                    (g * 255).toInt().toByte(),
                    (b * 255).toInt().toByte()
                )
            )
        if(prevCellInfo == newCellInfo) return

        colorUpdateQueue.push( newCellInfo )
        prevCellInfo = newCellInfo
    }

    companion object {
        var totalTextureCount = 0
    }

    class CellInfo(
        val coord: Pair<Int, Int>,
        val size: Pair<Int, Int>,
        val color: Triple<Byte, Byte, Byte>
    ) {
    }
}