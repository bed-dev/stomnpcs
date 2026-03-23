package codes.bed.minestom.npcs.api

import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerHand

data class NpcInteraction(
    val npc: Npc,
    val player: Player,
    val hand: PlayerHand,
    val type: NpcInteractionType,
)
