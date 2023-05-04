package dev.loadstone.server.cache

import dev.loadstone.server.cache.container.Compression
import dev.loadstone.server.cache.container.Container
import dev.loadstone.server.cache.container.XTEA_ZERO_KEY
import dev.loadstone.server.cache.settings.GroupSettings
import java.util.SortedMap

data class Archive(
    val id: Int,
    var version: Int? = null,
    val containsNameHash: Boolean = false,
    val containsSizes: Boolean = false,
    val containsUncompressedCrc: Boolean = false,
    var compression: Compression = Compression.NONE,
    val groupSettings: SortedMap<Int, GroupSettings> = sortedMapOf(),
    private val cache: Cache
) {

    fun readGroup(groupId: Int, xteaKey: IntArray = XTEA_ZERO_KEY): Group {
        val settings = groupSettings.getOrElse(groupId) {
            throw IllegalArgumentException("Failed to read group $groupId as it does not exist.")
        }
        return readGroup(id, settings, xteaKey)
    }

    fun readGroup(
        archiveId: Int,
        groupSettings: GroupSettings,
        xteaKey: IntArray = XTEA_ZERO_KEY
    ): Group {
        val groupData = Group.Data.decode(
            Container.decode(cache.fileStore.read(id, groupSettings.id), xteaKey),
            groupSettings.fileSettings.size
        )
        var i = 0
        val files = sortedMapOf<Int, File>()
        groupSettings.fileSettings.forEach { (fileId, fileSettings) ->
            files[fileId] = File(fileId, fileSettings.nameHash, groupData.fileData[i])
            i++
        }
        return Group(
            groupSettings.id,
            groupSettings.version,
            groupData.chunkCount,
            groupSettings.nameHash,
            groupData.compression,
            groupSettings.compressedCrc,
            groupSettings.uncompressedCrc,
            groupSettings.sizes,
            files
        )
    }
}