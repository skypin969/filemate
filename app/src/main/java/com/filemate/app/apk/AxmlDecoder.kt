package com.filemate.app.apk

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Mengubah AndroidManifest.xml dari format biner (AXML) menjadi teks XML biasa.
 * Hasilnya hanya untuk dibaca. Teks ini belum bisa dikembalikan lagi ke format biner.
 */
object AxmlDecoder {

    private const val TYPE_STRING_POOL = 0x0001
    private const val TYPE_XML = 0x0003
    private const val TYPE_START_NS = 0x0100
    private const val TYPE_START_ELEMENT = 0x0102
    private const val TYPE_END_ELEMENT = 0x0103

    fun decode(data: ByteArray): String {
        require(data.size >= 8) { "Berkas terlalu kecil" }
        val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        require(u16(buf, 0) == TYPE_XML) { "Bukan AndroidManifest biner" }

        val out = StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
        var strings: List<String> = emptyList()
        val prefixByUri = HashMap<String, String>()
        val pendingNs = ArrayList<Pair<String, String>>() // deklarasi xmlns yang belum ditulis
        var depth = 0
        var offset = u16(buf, 2)

        while (offset + 8 <= data.size) {
            val type = u16(buf, offset)
            val size = u32(buf, offset + 4)
            if (size <= 0) break

            when (type) {
                TYPE_STRING_POOL -> strings = readStringPool(buf, offset)

                TYPE_START_NS -> {
                    val prefix = str(strings, u32(buf, offset + 16))
                    val uri = str(strings, u32(buf, offset + 20))
                    prefixByUri[uri] = prefix
                    pendingNs.add(prefix to uri)
                }

                TYPE_START_ELEMENT -> {
                    val name = str(strings, u32(buf, offset + 20))
                    val attrStart = u16(buf, offset + 24)
                    val attrSize = u16(buf, offset + 26)
                    val attrCount = u16(buf, offset + 28)

                    indent(out, depth).append('<').append(name)
                    for ((prefix, uri) in pendingNs) {
                        out.append(" xmlns:").append(prefix).append("=\"").append(esc(uri)).append('"')
                    }
                    pendingNs.clear()

                    for (i in 0 until attrCount) {
                        val a = offset + 16 + attrStart + i * attrSize
                        val nsIdx = u32(buf, a)
                        val nameIdx = u32(buf, a + 4)
                        val rawIdx = u32(buf, a + 8)
                        val valueType = data[a + 15].toInt() and 0xFF
                        val valueData = u32(buf, a + 16)

                        val attrName = str(strings, nameIdx)
                        val prefix = if (nsIdx != -1) {
                            prefixByUri[str(strings, nsIdx)]?.let { "$it:" } ?: ""
                        } else {
                            ""
                        }
                        val value = if (rawIdx != -1) {
                            str(strings, rawIdx)
                        } else {
                            typedValue(strings, valueType, valueData)
                        }
                        out.append(' ').append(prefix).append(attrName)
                            .append("=\"").append(esc(value)).append('"')
                    }
                    out.append(">\n")
                    depth++
                }

                TYPE_END_ELEMENT -> {
                    depth = maxOf(depth - 1, 0)
                    indent(out, depth).append("</")
                        .append(str(strings, u32(buf, offset + 20))).append(">\n")
                }
            }
            offset += size
        }
        return out.toString()
    }

    private fun typedValue(strings: List<String>, type: Int, data: Int): String = when (type) {
        0x01 -> "@0x" + Integer.toHexString(data)
        0x02 -> "?0x" + Integer.toHexString(data)
        0x03 -> str(strings, data)
        0x04 -> java.lang.Float.intBitsToFloat(data).toString()
        0x10 -> data.toString()
        0x11 -> "0x" + Integer.toHexString(data)
        0x12 -> if (data != 0) "true" else "false"
        in 0x1c..0x1f -> "#" + String.format("%08x", data)
        else -> "0x" + Integer.toHexString(data)
    }

    private fun readStringPool(buf: ByteBuffer, start: Int): List<String> {
        val headerSize = u16(buf, start + 2)
        val count = u32(buf, start + 8)
        val flags = u32(buf, start + 16)
        val stringsStart = u32(buf, start + 20)
        val isUtf8 = (flags and 0x100) != 0
        val poolStart = start + stringsStart

        val list = ArrayList<String>(count)
        for (i in 0 until count) {
            val offset = poolStart + u32(buf, start + headerSize + i * 4)
            list.add(if (isUtf8) readUtf8(buf, offset) else readUtf16(buf, offset))
        }
        return list
    }

    private fun readUtf8(buf: ByteBuffer, offset: Int): String {
        var p = offset
        // Panjang dalam karakter: dilewati, karena yang dipakai panjang dalam byte
        p += if ((buf.get(p).toInt() and 0x80) != 0) 2 else 1

        val first = buf.get(p).toInt() and 0xFF
        val byteLen: Int
        if ((first and 0x80) != 0) {
            byteLen = ((first and 0x7F) shl 8) or (buf.get(p + 1).toInt() and 0xFF)
            p += 2
        } else {
            byteLen = first
            p += 1
        }
        val base = p
        val bytes = ByteArray(byteLen) { buf.get(base + it) }
        return String(bytes, Charsets.UTF_8)
    }

    private fun readUtf16(buf: ByteBuffer, offset: Int): String {
        var p = offset
        var len = u16(buf, p)
        p += 2
        if ((len and 0x8000) != 0) {
            len = ((len and 0x7FFF) shl 16) or u16(buf, p)
            p += 2
        }
        val base = p
        val chars = CharArray(len) { u16(buf, base + it * 2).toChar() }
        return String(chars)
    }

    private fun u16(buf: ByteBuffer, index: Int): Int = buf.getShort(index).toInt() and 0xFFFF

    private fun u32(buf: ByteBuffer, index: Int): Int = buf.getInt(index)

    private fun str(strings: List<String>, index: Int): String =
        if (index in strings.indices) strings[index] else ""

    private fun indent(out: StringBuilder, depth: Int): StringBuilder {
        repeat(depth) { out.append("    ") }
        return out
    }

    private fun esc(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
