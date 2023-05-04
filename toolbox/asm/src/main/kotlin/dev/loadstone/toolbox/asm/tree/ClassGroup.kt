package dev.loadstone.toolbox.asm.tree

import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

class ClassGroup {

    private val classList = mutableListOf<ClassNode>()

    val classes get() = classList.filter { !it.ignored }.toList()
    val ignoredClasses get() = classList.filter { it.ignored }.toList()
    val allClasses get() = classList.toList()

    val size get() = classes.size

    fun addClass(cls: ClassNode) {
        cls.init(this)
        classList.add(cls)
    }

    fun removeClass(cls: ClassNode) {
        classList.remove(cls)
    }

    fun replaceClass(old: ClassNode, new: ClassNode) {
        removeClass(old)
        addClass(new)
    }

    fun getClass(name: String) = classes.firstOrNull { it.name == name }
    fun getIgnoredClass(name: String) = ignoredClasses.firstOrNull { it.name == name }

    fun clear() {
        classList.clear()
    }

    fun readJar(file: File, action: (ClassNode) -> Unit = {}) {
        JarFile(file).use { jar ->
            jar.entries().asSequence().forEach { entry ->
                if(entry.name.endsWith(".class")) {
                    val bytes = jar.getInputStream(entry).readAllBytes()
                    val cls = ClassNode().fromBytes(bytes)
                    addClass(cls)
                    action(cls)
                }
            }
        }
    }

    fun writeJar(file: File) {
        if(file.exists()) file.deleteRecursively()
        file.parentFile.mkdirs()
        JarOutputStream(file.outputStream()).use { jos ->
            classes.forEach { cls ->
                jos.putNextEntry(JarEntry("${cls.name}.class"))
                jos.write(cls.toBytes())
                jos.closeEntry()
            }
        }
    }
}