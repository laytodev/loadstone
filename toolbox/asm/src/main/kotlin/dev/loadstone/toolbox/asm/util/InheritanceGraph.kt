package dev.loadstone.toolbox.asm.util

import dev.loadstone.toolbox.asm.tree.ClassGroup

class InheritanceGraph(private val group: ClassGroup) {

    private val nodes = hashMapOf<String, Node>()

    init {
        group.allClasses.forEach { cls ->
            val node = nodes.computeIfAbsent(cls.name) { Node(cls.name) }
            if(cls.superName != null) {
                val superClassNode = nodes.computeIfAbsent(cls.superName) { Node(cls.superName) }
                node.directParents.add(superClassNode)
                superClassNode.directChildren.add(node)
            }
            if(cls.interfaces != null) {
                cls.interfaces.forEach { interf ->
                    val interfNode = nodes.computeIfAbsent(interf) { Node(interf) }
                    node.directParents.add(interfNode)
                    interfNode.directChildren.add(node)
                }
            }
        }
        nodes.values.forEach { it.computeInheritance() }
    }

    operator fun get(name: String) = nodes[name]

    class Node(val name: String) {

        internal val directParents = hashSetOf<Node>()
        internal val directChildren = hashSetOf<Node>()

        val parents = hashSetOf<Node>()
        val children = hashSetOf<Node>()

        val relatives get() = hashSetOf<Node>().also {
            it.addAll(parents)
            it.addAll(children)
        }

        internal fun computeInheritance() {
            val queue = ArrayDeque<Node>()
            queue.addAll(directChildren)
            while(queue.isNotEmpty()) {
                val child = queue.removeFirst()
                children.add(child)
                queue.addAll(child.directChildren)
            }
            queue.addAll(directParents)
            while(queue.isNotEmpty()) {
                val parent = queue.removeFirst()
                parents.add(parent)
                queue.addAll(parent.directParents)
            }
        }
    }
}