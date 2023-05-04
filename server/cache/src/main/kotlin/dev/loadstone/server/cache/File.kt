package dev.loadstone.server.cache

import io.netty.buffer.ByteBuf
import io.netty.buffer.DefaultByteBufHolder

data class File(
    val id: Int,
    val nameHash: Int?,
    val data: ByteBuf
) : DefaultByteBufHolder(data)