package jp.ac.titech.itpro.sdl.circulardotter.gl

import android.opengl.GLES31
import android.util.Log
import androidx.core.math.MathUtils.clamp
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer
import java.util.*
import kotlin.math.max
import kotlin.math.min

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
            while (colorUpdateQueue.size > 0) {
                val cellInfo = colorUpdateQueue.pop()
                val (x, y) = cellInfo.coord
                val (w, h) = cellInfo.size
                val (r, g, b) = cellInfo.color

                val renderingArea = getRenderingArea(x, y, w, h)
                val (nw, nh) = renderingArea.wh
                val (lx, by) = renderingArea.leftBottom

                var buffer = ByteBuffer.allocateDirect(3 * nw * nh).run() {
                    order(ByteOrder.nativeOrder())
                }

                repeat(nw * nh) {
                    buffer.run {
                        put(r)
                        put(g)
                        put(b)
                    }
                }
                buffer.rewind()

                GLES31.glTexSubImage2D(
                    GLES31.GL_TEXTURE_2D,
                    0,
                    lx,
                    by,
                    nw,
                    nh,
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

    private fun getRenderingArea(cx: Int, cy: Int, w: Int, h: Int): RenderingArea {
        val (lx, rx) = max(cx - w / 2, 0) to min(cx + w / 2, width - 1)
        val (by, ty) = max(cy - h / 2, 0) to min(cy + h / 2, height - 1)
        val (nw, nh) = rx - lx + 1 to ty - by + 1
        return RenderingArea(lx to by, rx to ty, nw to nh)
    }

    private fun writeBytes(x: Int, y: Int, w: Int, h: Int, color: Triple<Byte, Byte, Byte>) {
        val (r, g, b) = color
        val renderingArea =  getRenderingArea(x, y, w, h)
        val (nx, ny) = renderingArea.leftBottom
        val (nw, nh) = renderingArea.wh

        Log.d(TAG, "xy = ${renderingArea.leftBottom}")
        Log.d(TAG, "wh = ${renderingArea.wh}")

        data.run {
            position(3 * (nx + ny * width))
            for(i in 0 until nh) {
                for(i in 0 until nw) {
                    put(r)
                    put(g)
                    put(b)
                }
                val curPos = position()
                Log.d(TAG, "curPos: ${curPos / 3}")
                position(curPos + 3 * (width - nw))
            }
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

    data class RenderingArea (
        val leftBottom: Pair<Int, Int>, val rightTop: Pair<Int, Int>, val wh: Pair<Int, Int>
    )

    data class CellInfo(
        val coord: Pair<Int, Int>,
        val size: Pair<Int, Int>,
        val color: Triple<Byte, Byte, Byte>
    )
}