package dev.loadstone.toolbox.asm.tree

import dev.loadstone.toolbox.asm.util.field
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.FieldNode
import java.lang.reflect.Modifier

fun FieldNode.init(owner: ClassNode) {
    this.owner = owner
}

var FieldNode.owner: ClassNode by field()

val FieldNode.group get() = owner.group
val FieldNode.id get() = "${owner.id}.$name"
val FieldNode.type get() = Type.getType(desc)

fun FieldNode.isStatic() = Modifier.isStatic(access)
fun FieldNode.isPublic() = Modifier.isPublic(access)
