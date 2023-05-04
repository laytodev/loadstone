package dev.loadstone.server.cache.settings

import dev.loadstone.server.cache.container.Container
import java.io.IOException
import java.util.SortedMap

data class ArchiveSettings(
    var version: Int?,
    val containsNameHash: Boolean,
    val containsSizes: Boolean,
    val containsUncompressedCrc: Boolean,
    val groupSettings: SortedMap<Int, GroupSettings> = sortedMapOf()
) {
    companion object {

        private const val MASK_NAME_HASH = 0x01
        private const val MASK_SIZES = 0x04
        private const val MASK_UNC_CRC = 0x08

        fun decode(container: Container): ArchiveSettings {
            val buf = container.data
            val formatOpcode = buf.readUnsignedByte().toInt()
            val format = Format.values().firstOrNull { it.opcode == formatOpcode } ?: throw IOException(
                "Archive settings format $formatOpcode not found."
            )
            val version = if(format == Format.VERSIONED) buf.readInt() else null
            val flags = buf.readUnsignedByte().toInt()
            val containsNameHash = flags and MASK_NAME_HASH != 0
            val containsSizes = flags and MASK_SIZES != 0
            val containsUncompressedCrc = flags and MASK_UNC_CRC != 0

            val groupCount = buf.readUnsignedShort()
            val groupIds = IntArray(groupCount)
            var groupAccumulator = 0
            for(archiveIndex in groupIds.indices) {
                val delta = buf.readUnsignedShort()
                groupAccumulator += delta
                groupIds[archiveIndex] = groupAccumulator
            }
            val nameHashes = if(containsNameHash) IntArray(groupCount) {
                buf.readInt()
            } else null
            val compressedCrcs = IntArray(groupCount) { buf.readInt() }
            val uncompressedCrcs = if(containsUncompressedCrc) IntArray(groupCount) {
                buf.readInt()
            } else null
            val sizes = if(containsSizes) Array(groupCount) {
                Container.Size(compressed = buf.readInt(), uncompressed = buf.readInt())
            } else null
            val versions = Array(groupCount) { buf.readInt() }
            val fileIds = Array(groupCount) {
                IntArray(buf.readUnsignedShort())
            }
            for(group in fileIds) {
                var fileAccumulator = 0
                for(fileIndex in group.indices) {
                    val delta = buf.readUnsignedShort()
                    fileAccumulator += delta
                    group[fileIndex] = fileAccumulator
                }
            }
            val groupFileNameHashes = if(containsNameHash) {
                Array(groupCount) {
                    IntArray(fileIds[it].size) { buf.readInt() }
                }
            } else null

            val groupSettings = sortedMapOf<Int, GroupSettings>()
            for(groupIndex in groupIds.indices) {
                val fileSettings = sortedMapOf<Int, FileSettings>()
                for(fileIndex in fileIds[groupIndex].indices) {
                    fileSettings[fileIds[groupIndex][fileIndex]] = FileSettings(
                        fileIds[groupIndex][fileIndex],
                        groupFileNameHashes?.get(groupIndex)?.get(fileIndex)
                    )
                }
                groupSettings[groupIds[groupIndex]] = GroupSettings(
                    groupIds[groupIndex],
                    versions[groupIndex],
                    compressedCrcs[groupIndex],
                    nameHashes?.get(groupIndex),
                    uncompressedCrcs?.get(groupIndex),
                    sizes?.get(groupIndex),
                    fileSettings
                )
            }
            return ArchiveSettings(version, containsNameHash, containsSizes, containsUncompressedCrc, groupSettings)
        }
    }
}