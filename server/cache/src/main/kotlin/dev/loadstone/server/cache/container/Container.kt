package dev.loadstone.server.cache.container

import io.netty.buffer.ByteBuf
import io.netty.buffer.DefaultByteBufHolder
import io.netty.buffer.Unpooled

data class Container(
    var data: ByteBuf,
    var compression: Compression = Compression.NONE,
    var version: Int? = null
) : DefaultByteBufHolder(data) {

    val isVersioned: Boolean get() = version != null

    data class Size(var compressed: Int, var uncompressed: Int)

    companion object {

        const val ENC_HEADER_SIZE: Int = Byte.SIZE_BYTES + Int.SIZE_BYTES

        private val IntArray.isZeroKey get() = contentEquals(XTEA_ZERO_KEY)

        fun decode(buf: ByteBuf, xteaKey: IntArray = XTEA_ZERO_KEY): Container {
            val compression = Compression.fromOpcode(buf.readUnsignedByte().toInt())
            val comprSize = buf.readInt()
            val encComprSize = compression.headerSize + comprSize
            val decBuf = if(!xteaKey.isZeroKey) {
                buf.xteaDecrypt(xteaKey, end = buf.readerIndex() + encComprSize)
            } else {
                buf.slice(buf.readerIndex(), buf.readableBytes())
            }
            val decomprBuf = if(compression != Compression.NONE) {
                val uncomprSize = decBuf.readInt()
                val uncompressed = try {
                    compression.decompress(decBuf, uncomprSize)
                } catch(e: Exception) {
                    throw DecryptionFailedException("Failed to decrypt container using XTEA key ${xteaKey.contentToString()}.")
                }
                Unpooled.wrappedBuffer(uncompressed)
            } else decBuf.slice(0, comprSize)
            decBuf.readerIndex(encComprSize)
            val version = if(decBuf.readableBytes() >= 2) decBuf.readShort().toInt() else null
            return Container(decomprBuf, compression, version)
        }

        fun decodeVersion(buf: ByteBuf): Int? {
            val compression = Compression.fromOpcode(buf.readUnsignedByte().toInt())
            val comprSize = buf.readInt()
            val size = ENC_HEADER_SIZE + compression.headerSize + comprSize
            buf.readerIndex(size)
            return if(buf.readableBytes() >= 2) buf.readShort().toInt() else null
        }

        fun decodeSize(buf: ByteBuf, xteaKey: IntArray = XTEA_ZERO_KEY): Size {
            val compression = Compression.fromOpcode(buf.readUnsignedByte().toInt())
            val comprSize = buf.readInt()
            if(compression == Compression.NONE) return Size(comprSize, comprSize)
            val encComprSize = compression.headerSize + comprSize
            val decBuf = if(!xteaKey.isZeroKey) {
                buf.xteaDecrypt(xteaKey, end = buf.readerIndex() + encComprSize)
            } else buf.slice(buf.readerIndex(), buf.readableBytes())
            val uncomprSize = decBuf.readInt()
            return Size(comprSize, uncomprSize)
        }
    }
}