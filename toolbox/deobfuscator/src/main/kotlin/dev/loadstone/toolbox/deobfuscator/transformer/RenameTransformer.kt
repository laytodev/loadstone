package dev.loadstone.toolbox.deobfuscator.transformer

import dev.loadstone.toolbox.asm.tree.*
import dev.loadstone.toolbox.asm.util.InheritanceGraph
import dev.loadstone.toolbox.deobfuscator.Transformer
import dev.loadstone.toolbox.deobfuscator.util.isObfuscatedName
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.SimpleRemapper
import org.objectweb.asm.tree.ClassNode
import org.tinylog.kotlin.Logger

class RenameTransformer : Transformer {

    private var classCount = 0
    private var methodCount = 0
    private var fieldCount = 0

    private val mappings = hashMapOf<String, String>()

    override fun run(group: ClassGroup) {
        generateNameMappings(group)
        applyNameMappings(group)
    }

    private fun generateNameMappings(group: ClassGroup) {
        val inheritanceGraph = InheritanceGraph(group)

        /*
         * Generate class names.
         */
        group.classes.forEach { cls ->
            if(cls.name.isObfuscatedName()) {
                mappings[cls.id] = "class${++classCount}"
            }
        }

        /*
         * Generate method names.
         */
        group.classes.forEach { cls ->
            cls.methods.forEach { method ->
                if(method.name.isObfuscatedName() && !mappings.containsKey(method.id)) {
                    val newName = "method${++methodCount}"
                    mappings[method.id] = newName
                    inheritanceGraph[method.owner.name]!!.children.forEach {
                        mappings["${it.name}.${method.name}${method.desc}"] = newName
                    }
                }
            }
        }

        /*
         * Generate field names
         */
        group.classes.forEach { cls ->
            cls.fields.forEach { field ->
                if(field.name.isObfuscatedName() && !mappings.containsKey(field.id)) {
                    val newName = "field${++fieldCount}"
                    mappings[field.id] = newName
                    inheritanceGraph[field.owner.name]!!.children.forEach {
                        mappings["${it.name}.${field.name}"] = newName
                    }
                }
            }
        }
    }

    private fun applyNameMappings(group: ClassGroup) {
        val remapper = SimpleRemapper(mappings)
        val newClasses = mutableListOf<Pair<ClassNode, ClassNode>>()
        group.allClasses.forEach { cls ->
            val newCls = ClassNode()
            cls.accept(ClassRemapper(newCls, remapper))
            newCls.ignored = cls.ignored
            newClasses.add(cls to newCls)
        }
        newClasses.forEach {
            group.replaceClass(it.first, it.second)
        }
    }

    override fun postRun(group: ClassGroup) {
        Logger.info("Renamed $classCount classes.")
        Logger.info("Renamed $methodCount methods.")
        Logger.info("Renamed $fieldCount fields.")
    }
}