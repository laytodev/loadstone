package dev.loadstone.toolbox.asm.tree

import dev.loadstone.toolbox.asm.util.field
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import java.lang.reflect.Modifier

fun MethodNode.init(owner: ClassNode) {
    this.owner = owner
}

var MethodNode.owner: ClassNode by field()

val MethodNode.group get() = owner.group
val MethodNode.id get() = "${owner.id}.$name$desc"
val MethodNode.type get() = Type.getMethodType(desc)

fun MethodNode.isStatic() = Modifier.isStatic(access)
fun MethodNode.isAbstract() = Modifier.isAbstract(access)
fun MethodNode.isPublic() = Modifier.isPublic(access)

fun MethodNode.isConstructor() = name == "<init>"
fun MethodNode.isInitializer() = name == "<clinit>"