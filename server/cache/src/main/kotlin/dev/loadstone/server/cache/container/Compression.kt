package dev.loadstone.server.cache.container

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufInputStream
import io.netty.buffer.Unpooled
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.ByteArrayInputStream
import java.io.SequenceInputStream
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream

sealed class Compression(val opcode: Int, val headerSize: Int) {

    abstract fun decompress(input: ByteBuf, length: Int): ByteBuf

    companion object {
        fun fromOpcode(opcode: Int): Compression = when(opcode) {
            0 -> NONE
            1 -> BZIP2
            2 -> GZIP
            else -> throw IllegalArgumentException("Unknown compression type $opcode.")
        }
    }

    object NONE : Compression(opcode = 0, headerSize = 0) {
        override fun decompress(input: ByteBuf, length: Int): ByteBuf {
            return input.slice(input.readerIndex(), length)
        }
    }

    object BZIP2 : Compression(opcode = 1, headerSize = Int.SIZE_BYTES) {
        override fun decompress(input: ByteBuf, length: Int): ByteBuf {
            val output = Unpooled.buffer(length)
            val str = SequenceInputStream(ByteArrayInputStream("BZh1".toByteArray(StandardCharsets.US_ASCII)), ByteBufInputStream(input))
            BZip2CompressorInputStream(str).use {
                output.writeBytes(it, length)
            }
            return output
        }
    }

    object GZIP : Compression(opcode = 2, headerSize = Int.SIZE_BYTES) {
        override fun decompress(input: ByteBuf, length: Int): ByteBuf {
            val output = Unpooled.buffer(length)
            GZIPInputStream(ByteBufInputStream(input)).use {
                while(it.available() == 1) output.writeBytes(it, length)
            }
            return output
        }
    }
}
