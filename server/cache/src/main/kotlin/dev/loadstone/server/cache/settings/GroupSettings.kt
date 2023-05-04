package dev.loadstone.server.cache.settings

import dev.loadstone.server.cache.container.Container
import java.util.SortedMap

data class GroupSettings(
    var id: Int,
    var version: Int,
    var compressedCrc: Int,
    var nameHash: Int? = null,
    var uncompressedCrc: Int? = null,
    var sizes: Container.Size? = null,
    val fileSettings: SortedMap<Int, FileSettings>
) : SortedMap<Int, FileSettings> by fileSettings {

}