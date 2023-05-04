package dev.loadstone.server.cache.disk

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import java.io.File
import java.io.FileNotFoundException
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption

class IdxFile(val id: Int, private val file: File) : AutoCloseable {

    private val channel = FileChannel.open(file.toPath(), StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE)

    val size: Long get() = channel.size()

    fun read(containerId: Int): Entry {
        val ptr = containerId.toLong() * Entry.SIZE.toLong()
        if(ptr < 0 || ptr >= channel.size()) {
            throw FileNotFoundException("Could not find container $containerId.")
        }
        val buf = Unpooled.buffer(Entry.SIZE)
        buf.writeBytes(channel, ptr, buf.writableBytes())
        return Entry.decode(buf)
    }

    fun write(containerId: Int, entry: Entry) {
        val buf = entry.encode()
        buf.readBytes(channel, containerId.toLong() * Entry.SIZE.toLong(), buf.readableBytes())
    }

    override fun close() {
        channel.close()
    }

    data class Entry(val sector: Int, val size: Int) {

        fun encode(): ByteBuf {
            val buf = Unpooled.buffer(SIZE)
            buf.writeMedium(size)
            buf.writeMedium(sector)
            return buf
        }

        companion object {

            const val SIZE: Int = 6

            fun decode(buf: ByteBuf): Entry {
                val size = buf.readUnsignedMedium()
                val sector = buf.readUnsignedMedium()
                return Entry(sector, size)
            }
        }
    }
}