package dev.loadstone.server.cache

import dev.loadstone.server.cache.container.Container
import dev.loadstone.server.cache.container.XTEA_ZERO_KEY
import dev.loadstone.server.cache.disk.FileStore
import dev.loadstone.server.cache.settings.ArchiveSettings
import io.netty.buffer.Unpooled
import java.io.File
import java.io.IOException

class Cache private constructor(val path: File) : AutoCloseable {

    lateinit var fileStore: FileStore private set

    val archiveCount get() = fileStore.archiveCount

    private fun init() {
        fileStore = FileStore(this, path)
    }

    override fun close() {
        fileStore.close()
    }

    fun readArchive(archiveId: Int, xteaKey: IntArray = XTEA_ZERO_KEY): Archive {
        val data = fileStore.read(255, archiveId)
        if(data == Unpooled.EMPTY_BUFFER) throw IOException(
            "Archive settings for archive $archiveId does not exist."
        )
        val container = Container.decode(data, xteaKey)
        val archiveSettings = ArchiveSettings.decode(container)
        return Archive(
            archiveId,
            archiveSettings.version,
            archiveSettings.containsNameHash,
            archiveSettings.containsSizes,
            archiveSettings.containsUncompressedCrc,
            container.compression,
            archiveSettings.groupSettings,
            this
        )
    }

    companion object {

        /**
         * Opens a new instance of a disk dat2 format cache from the input directory
         * [File] object.
         *
         * @param path The directory of the cache
         * @return [Cache]
         */
        fun open(path: File): Cache {
            val cache = Cache(path)
            cache.init()
            return cache
        }
    }
}

fun main() {
    println("Opening cache.")

    val cache = Cache.open(File("data/"))
    println("Archive count: ${cache.archiveCount}")

    val archive1 = cache.readArchive(2)
    val group1 = archive1.readGroup(6)
    println()
}