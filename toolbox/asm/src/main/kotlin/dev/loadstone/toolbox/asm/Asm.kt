package dev.loadstone.toolbox.asm

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*

fun AbstractInsnNode.isInstruction(): Boolean = this !is LineNumberNode && this !is FrameNode && this !is LabelNode

fun AbstractInsnNode.next(amount: Int): AbstractInsnNode {
    var cur = this
    repeat(amount) {
        cur = cur.next
    }
    return cur
}

fun AbstractInsnNode.previous(amount: Int): AbstractInsnNode {
    var cur = this
    repeat(amount) {
        cur = cur.previous
    }
    return cur
}

fun AbstractInsnNode.isTerminating(): Boolean = when(this.opcode) {
    Opcodes.RETURN,
    Opcodes.ARETURN,
    Opcodes.IRETURN,
    Opcodes.FRETURN,
    Opcodes.DRETURN,
    Opcodes.LRETURN,
    Opcodes.ATHROW,
    Opcodes.TABLESWITCH,
    Opcodes.LOOKUPSWITCH,
    Opcodes.GOTO -> true
    else -> false
}

fun InsnList.copy(): InsnList {
    val newInsnList = InsnList()
    var insn = this.first
    while(insn != null) {
        newInsnList.add(insn)
        insn = insn.next
    }
    return newInsnList
}

fun InsnList.clone(): InsnList {
    val newInsnList = InsnList()
    val labels = LabelMap()
    var insn = this.first
    while(insn != null) {
        newInsnList.add(insn.clone(labels))
        insn = insn.next
    }
    return newInsnList
}

inline fun <reified T : AbstractInsnNode> Collection<AbstractInsnNode>.findFirst(): AbstractInsnNode {
    this.forEach { insn ->
        if(insn::class == T::class) {
            return insn
        }
    }
    throw NullPointerException()
}

class LabelMap : AbstractMap<LabelNode, LabelNode>() {
    private val map = hashMapOf<LabelNode, LabelNode>()
    override val entries get() = throw IllegalStateException()
    override fun get(key: LabelNode) = map.getOrPut(key) { LabelNode() }
}

fun isJdkMethod(owner: String, name: String, desc: String): Boolean {
    try {
        var classes = listOf(Class.forName(Type.getObjectType(owner).className))
        while(classes.isNotEmpty()) {
            for(cls in classes) {
                if(cls.declaredMethods.any { it.name == name && Type.getMethodDescriptor(it) == desc }) {
                    return true
                }
            }
            classes = classes.flatMap {
                mutableListOf<Class<*>>().apply {
                    addAll(it.interfaces)
                    if(it.superclass != null) {
                        add(it.superclass)
                    }
                }
            }
        }
    } catch(e: Exception) { /* Do Nothing */ }
    return false
}

fun InsnList.createLabel(insn: AbstractInsnNode, forceCreate: Boolean = false): LabelNode {
    if(insn is LabelNode) return insn
    val idx = this.indexOf(insn)
    if(idx > 0) {
        val before = this[idx - 1]
        if(!forceCreate && before is LabelNode) {
            return before
        }
    }
    val labelMap = LabelMap()
    val label = LabelNode()
    this.insert(this[idx], label)
    return label
}