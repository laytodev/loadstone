package dev.loadstone.toolbox.deobfuscator.util

fun String.isObfuscatedName(): Boolean {
    return (this.length <= 2) || (this.length == 3 && this.startsWith("a") && this != "add")
}