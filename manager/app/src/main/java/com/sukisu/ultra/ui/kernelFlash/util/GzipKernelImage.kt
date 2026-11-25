package com.sukisu.ultra.ui.kernelFlash.util

import java.io.File
import java.io.IOException
import java.util.zip.CRC32
import java.util.zip.GZIPOutputStream
import java.util.zip.Inflater

/**
 * kptools only patches a raw arm64 Image. Non-GKI AnyKernel3 packages usually ship
 * the kernel gzip-compressed with the device trees appended (Image.gz-dtb), which
 * kptools rejects with a kernel image magic error. This unwraps such a file into
 * the raw Image plus the untouched trailing device trees, and wraps it back.
 */
object GzipKernelImage {
    private const val DTB_MAGIC = 0xD00DFEEDL

    /** Bytes that followed the gzip stream (appended DTBs); empty for a plain Image.gz. */
    class Tail(val bytes: ByteArray)

    /** Returns null when [input] is not gzip, so the caller keeps the existing raw-Image flow. */
    fun unwrap(input: File, rawOutput: File): Tail? {
        val data = input.readBytes()
        if (data.size < 18 || data[0] != 0x1f.toByte() || data[1] != 0x8b.toByte()) return null
        if (data[2].toInt() != 8) throw IOException("Unsupported gzip compression method")
        val flags = data[3].toInt() and 0xff
        var pos = 10
        if (flags and 4 != 0) pos += 2 + ((data[pos].toInt() and 0xff) or ((data[pos + 1].toInt() and 0xff) shl 8))
        if (flags and 8 != 0) { while (data[pos].toInt() != 0) pos++; pos++ }
        if (flags and 16 != 0) { while (data[pos].toInt() != 0) pos++; pos++ }
        if (flags and 2 != 0) pos += 2

        val inflater = Inflater(true)
        val crc = CRC32()
        var size = 0L
        try {
            inflater.setInput(data, pos, data.size - pos)
            val buffer = ByteArray(1 shl 16)
            rawOutput.outputStream().use { out ->
                while (!inflater.finished()) {
                    val n = inflater.inflate(buffer)
                    if (n == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
                        throw IOException("Truncated gzip kernel image")
                    }
                    out.write(buffer, 0, n)
                    crc.update(buffer, 0, n)
                    size += n
                }
            }
            pos += inflater.bytesRead.toInt()
        } finally {
            inflater.end()
        }
        if (pos + 8 > data.size) throw IOException("Truncated gzip kernel image")
        if (le32(data, pos) != crc.value || le32(data, pos + 4) != (size and 0xffffffffL)) {
            throw IOException("Gzip kernel image failed its checksum")
        }
        val tail = data.copyOfRange(pos + 8, data.size)
        // Anything after the kernel must be device trees; refuse to guess otherwise.
        if (tail.isNotEmpty() && (tail.size < 4 || be32(tail, 0) != DTB_MAGIC)) {
            throw IOException("Unknown data after the compressed kernel; refusing to repack")
        }
        return Tail(tail)
    }

    fun wrap(raw: File, tail: Tail, output: File) {
        output.outputStream().use { out ->
            val gzip = object : GZIPOutputStream(out, 1 shl 16) {
                init { def.setLevel(9) }
            }
            raw.inputStream().use { it.copyTo(gzip) }
            gzip.finish()
            out.write(tail.bytes)
        }
        check(output.length() > tail.bytes.size) { "Recompressed kernel is empty" }
    }

    private fun le32(b: ByteArray, o: Int): Long =
        (b[o].toLong() and 0xff) or ((b[o + 1].toLong() and 0xff) shl 8) or
            ((b[o + 2].toLong() and 0xff) shl 16) or ((b[o + 3].toLong() and 0xff) shl 24)

    private fun be32(b: ByteArray, o: Int): Long =
        ((b[o].toLong() and 0xff) shl 24) or ((b[o + 1].toLong() and 0xff) shl 16) or
            ((b[o + 2].toLong() and 0xff) shl 8) or (b[o + 3].toLong() and 0xff)
}
