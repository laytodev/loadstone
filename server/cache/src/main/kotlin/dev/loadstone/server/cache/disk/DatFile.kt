package dev.loadstone.server.cache.disk

import io.netty.buffer.ByteBuf
import io.netty.buffer.DefaultByteBufHolder
import io.netty.buffer.Unpooled
import java.io.File
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import kotlin.math.ceil

class DatFile(private val file: File) : AutoCloseable {

    private val channel = FileChannel.open(file.toPath(), StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE)

    val size: Long get() = channel.size()

    fun read(idxFileId: Int, containerId: Int, entry: IdxFile.Entry): ByteBuf {
        val data = Unpooled.compositeBuffer(
            ceil(entry.size.toDouble() / Sector.SIZE).toInt()
        )
        var sectorsRead = 0
        var dataToRead = entry.size
        var curOffset = entry.sector * Sector.SIZE.toLong()
        do {
            val sector = readSector(containerId, curOffset)
            sector.validate(idxFileId, containerId, sectorsRead)
            if(dataToRead > sector.data.writerIndex()) {
                data.addComponent(true, sector.data)
                dataToRead -= sector.data.writerIndex()
                sectorsRead++
                curOffset = sector.nextSector * Sector.SIZE.toLong()
            } else {
                data.addComponent(true, sector.data.slice(0, dataToRead))
                dataToRead = 0
            }
        } while(dataToRead > 0)
        return data
    }

    private fun readSector(containerId: Int, offset: Long): Sector {
        val buf = Unpooled.buffer(Sector.SIZE)
        buf.writeBytes(channel, offset, buf.writableBytes())
        return Sector.decode(buf)
    }

    override fun close() {
        channel.close()
    }

    data class Sector(
        val containerId: Int,
        val position: Int,
        val nextSector: Int,
        val idxFileId: Int,
        val data: ByteBuf
    ) : DefaultByteBufHolder(data) {

        fun encode(buf: ByteBuf = Unpooled.buffer(SIZE)): ByteBuf {
            buf.writeShort(containerId)
            buf.writeShort(position)
            buf.writeMedium(nextSector)
            buf.writeByte(idxFileId)
            buf.writeBytes(data)
            return buf
        }

        fun validate(idxFileId: Int, containerId: Int, position: Int): Sector {
            if(this.idxFileId != idxFileId) throw IOException(
                "Idx file id mismatch. Expected ${this.idxFileId} was $idxFileId."
            )
            if(this.containerId != containerId) throw IOException(
                "Container id mismatch. Expected ${this.containerId} was $containerId."
            )
            if(this.position != position) throw IOException(
                "Sector position mismatch. Expected ${this.position} was $position."
            )
            return this
        }

        companion object {

            const val HEADER_SIZE: Int = 8

            const val DATA_SIZE: Int = 512

            const val SIZE: Int = HEADER_SIZE + DATA_SIZE

            fun decode(buf: ByteBuf): Sector {
                val containerId = buf.readUnsignedShort()
                val position = buf.readUnsignedShort()
                val nextSector = buf.readUnsignedMedium()
                val idxFileId = buf.readUnsignedByte().toInt()
                val data = buf.slice(HEADER_SIZE, DATA_SIZE)
                return Sector(containerId, position, nextSector, idxFileId, data)
            }
        }
    }
}