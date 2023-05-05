package dev.loadstone.toolbox.deobfuscator.transformer

import dev.loadstone.toolbox.asm.tree.ClassGroup
import dev.loadstone.toolbox.deobfuscator.Transformer
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.BasicInterpreter
import org.tinylog.kotlin.Logger

class DeadCodeTransformer : Transformer {

    private var count = 0

    override fun run(group: ClassGroup) {
        group.classes.forEach { cls ->
            cls.methods.forEach { method ->
                val insns = method.instructions.toArray()
                val frames = Analyzer(BasicInterpreter()).analyze(cls.name, method)
                for(i in frames.indices) {
                    if(frames[i] == null) {
                        method.instructions.remove(insns[i])
                        count++
                    }
                }
            }
        }
    }

    override fun postRun(group: ClassGroup) {
        Logger.info("Removed $count dead instructions in methods.")
    }
}