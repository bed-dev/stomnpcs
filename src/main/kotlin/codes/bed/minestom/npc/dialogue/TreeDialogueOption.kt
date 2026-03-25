package codes.bed.minestom.npc.dialogue

import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player

class TreeDialogueOption(
    val text: Component,
    val nextNode: TreeDialogueNode?,
    val onSelect: ((Player) -> Unit)?
)