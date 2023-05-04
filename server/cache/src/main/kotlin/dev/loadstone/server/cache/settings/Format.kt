package dev.loadstone.server.cache.settings

enum class Format(val opcode: Int) {
    UNVERSIONED(5),
    VERSIONED(6)
}