package dev.loadstone.server.cache

import dev.loadstone.server.cache.container.Compression
import dev.loadstone.server.cache.container.Container
import io.netty.buffer.ByteBuf
import io.netty.buffer.CompositeByteBuf
import io.netty.buffer.Unpooled
import java.util.SortedMap

data class Group(
    var id: Int,
    var version: Int,
    var chunkCount: Int,
    var nameHash: Int? = null,
    var compression: Compression = Compression.NONE,
    internal var compressedCrc: Int = 0,
    internal var uncompressedCrc: Int? = null,
    internal var sizes: Container.Size? = null,
    val files: SortedMap<Int, File> = sortedMapOf()
) {

    data class Data(
        val fileData: List<ByteBuf>,
        var chunkCount: Int = 1,
        var compression: Compression = Compression.NONE
    ) {
        companion object {

            fun decode(container: Container, fileCount: Int) = if(fileCount == 1) {
                Data(listOf(container.data), 1, container.compression)
            } else {
                decodeMultipleFiles(container, fileCount)
            }

            private fun decodeMultipleFiles(container: Container, fileCount: Int): Data {
                val fileSizes = IntArray(fileCount)
                val chunkCount = container.data.getUnsignedByte(container.data.readableBytes() - 1).toInt()
                val chunkFileSizes = Array(chunkCount) { IntArray(fileCount) }
                container.data.readerIndex(container.data.readableBytes() - 1 - chunkCount * fileCount * 4)
                for(chunkId in 0 until chunkCount) {
                    var groupFileSize = 0
                    for(fileId in 0 until fileCount) {
                        val delta = container.data.readInt()
                        groupFileSize += delta
                        chunkFileSizes[chunkId][fileId] = groupFileSize
                        fileSizes[fileId] += groupFileSize
                    }
                }
                val fileData = Array<CompositeByteBuf>(fileCount) { Unpooled.compositeBuffer(chunkCount) }
                container.data.readerIndex(0)
                for(chunkId in 0 until chunkCount) {
                    for(fileId in 0 until fileCount) {
                        val groupFileSize = chunkFileSizes[chunkId][fileId]
                        fileData[fileId].addComponent(
                            true,
                            container.data.slice(container.data.readerIndex(), groupFileSize)
                        )
                        container.data.readerIndex(container.data.readerIndex() + groupFileSize)
                    }
                }
                return Data(
                    fileData.map { it.asReadOnly() },
                    chunkCount,
                    container.compression
                )
            }
        }
    }
}