package codes.bed.minestom.npc.listener

import codes.bed.minestom.npc.api.NpcInteraction

fun interface NpcInteractListener {
    fun onInteract(interaction: NpcInteraction)
}

