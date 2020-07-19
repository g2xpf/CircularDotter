package jp.ac.titech.itpro.sdl.circulardotter.gl

import android.opengl.GLES31
import android.util.Log
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer
import java.util.*

class Texture(
    private val width: Int,
    private val height: Int,
    private var data: ByteBuffer,
    private val colorUpdateQueue: LinkedList<CellInfo> = LinkedList(),
    private var prevCellInfo: CellInfo = CellInfo(-1 to -1, -1 to -1, Triple(0, 0, 0))
) {
    private val TAG = Texture::class.qualifiedName
    private var texture: Int = 0
    private val textureCount: Int

    init {
        textureCount = totalTextureCount++
    }

    fun initialize() {
        val buf = IntBuffer.allocate(1)

        GLES31.glActiveTexture(GLES31.GL_TEXTURE0 + textureCount)
        GLES31.glGenTextures(1, buf)
        texture = buf[0]
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture)

        GLES31.glPixelStorei(GLES31.GL_UNPACK_ALIGNMENT, 1)
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

    // write CellInfo to the texture only when it's not equivalent to the previous one
    fun write(x: Int, y: Int, w: Int, h: Int, data: Triple<Float, Float, Float>) {
        val (r, g, b) = data
        val colorBytes = Triple(
            (r * 255).toInt().toByte(),
            (g * 255).toInt().toByte(),
            (b * 255).toInt().toByte()
        )

        val newCellInfo = CellInfo(
            Pair(x, y),
            Pair(w, h),
            colorBytes
        )
        if (prevCellInfo == newCellInfo) return

        writeBytes(x, y, w, h, colorBytes)
        colorUpdateQueue.push(newCellInfo)

        prevCellInfo = newCellInfo
    }

    private fun writeBytes(x: Int, y: Int, w: Int, h: Int, color: Triple<Byte, Byte, Byte>) {
        val (r, g, b) = color
        data.run {
            position(3 * (x + y * width))
            put(r)
            put(g)
            put(b)
            rewind()
        }
    }

    fun readColor(x: Int, y: Int): Triple<Float, Float, Float> {
        data.run() {
            position(3 * (x + y * width))
            val r = get()
            val g = get()
            val b = get()
            rewind()
            return Triple(
                byteToNormalizedFloat(r),
                byteToNormalizedFloat(g),
                byteToNormalizedFloat(b)
            )
        }
    }

    private fun byteToNormalizedFloat(b: Byte): Float {
        return (b.toInt() and 0xff).toFloat() / 255.0f
    }

    fun read(): Triple<Int, Int, ByteBuffer> {
        return Triple(width, height, data)
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