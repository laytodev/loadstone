package dev.loadstone.toolbox.deobfuscator.transformer

import dev.loadstone.toolbox.asm.tree.ClassGroup
import dev.loadstone.toolbox.deobfuscator.Transformer
import org.objectweb.asm.tree.TryCatchBlockNode
import org.tinylog.kotlin.Logger

class RuntimeExceptionTransformer : Transformer {

    private var count = 0

    override fun run(group: ClassGroup) {
        group.classes.forEach { cls ->
            cls.methods.forEach { method ->
                val toRemove = mutableListOf<TryCatchBlockNode>()
                method.tryCatchBlocks.forEach { tcb ->
                    if(tcb.type == "java/lang/RuntimeException") {
                        toRemove.add(tcb)
                        count++
                    }
                }
                method.tryCatchBlocks.removeAll(toRemove)
            }
        }
    }

    override fun postRun(group: ClassGroup) {
        Logger.info("Removed $count 'RuntimeException' try-catch blocks.")
    }
}