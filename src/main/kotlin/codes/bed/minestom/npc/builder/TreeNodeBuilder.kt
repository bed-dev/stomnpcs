package codes.bed.minestom.npc.builder

import codes.bed.minestom.npc.dialogue.TreeDialogueNode
import codes.bed.minestom.npc.dialogue.TreeDialogueOption
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player

class TreeNodeBuilder {
    private val lines = mutableListOf<Component>()
    private val options = mutableListOf<TreeDialogueOption>()

    /** Adds a line of text for the NPC to speak. */
    fun line(text: Component) = apply { lines += text }
    fun line(text: String) = apply { lines += Component.text(text) }

    fun option(
        text: Component,
        onSelect: ((Player) -> Unit)? = null,
        nextNodeConfig: (TreeNodeBuilder.() -> Unit)? = null
    ) {
        val childNode = if (nextNodeConfig != null) {
            val childBuilder = TreeNodeBuilder()
            childBuilder.nextNodeConfig()
            childBuilder.build()
        } else {
            null
        }

        options += TreeDialogueOption(text, childNode, onSelect)
    }

    fun option(
        text: String,
        onSelect: ((Player) -> Unit)? = null,
        nextNodeConfig: (TreeNodeBuilder.() -> Unit)? = null
    ) = option(Component.text(text), onSelect, nextNodeConfig)

    internal fun build(): TreeDialogueNode = TreeDialogueNode(lines.toList(), options.toList())
}
