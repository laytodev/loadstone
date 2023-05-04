package dev.loadstone.server.cache.disk

import dev.loadstone.server.cache.Cache
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import java.io.File
import java.io.FileNotFoundException

class FileStore(val cache: Cache, private val path: File) : AutoCloseable {

    private val datFile = DatFile(path.resolve("main_file_cache.dat2"))
    private val idxFiles = hashMapOf<Int, IdxFile>()

    init {
        idxFiles[255] = IdxFile(255, path.resolve("main_file_cache.idx255"))
        for(i in 0 until 255) {
            val file = path.resolve("main_file_cache.idx$i")
            if(!file.exists()) break
            idxFiles[i] = IdxFile(i, file)
        }
    }

    val archiveCount get() = idxFiles.keys.size - 1

    fun read(idxFileId: Int, containerId: Int): ByteBuf {
        val idxFile = idxFiles[idxFileId] ?: throw FileNotFoundException("Unknown idx file id.")
        val idxEntry = idxFile.read(containerId)
        if(idxEntry.size == 0) {
            println("Could not read idx file ${idxFile.id} container $containerId as it does not exist.")
            return Unpooled.EMPTY_BUFFER
        }
        return datFile.read(idxFile.id, containerId, idxEntry)
    }

    override fun close() {
        datFile.close()
        idxFiles.values.forEach { it.close() }
    }
}