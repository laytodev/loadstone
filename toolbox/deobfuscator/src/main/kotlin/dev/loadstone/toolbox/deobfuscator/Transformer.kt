package dev.loadstone.toolbox.deobfuscator

import dev.loadstone.toolbox.asm.tree.ClassGroup

interface Transformer {

    fun run(group: ClassGroup)

    fun postRun(group: ClassGroup)

}