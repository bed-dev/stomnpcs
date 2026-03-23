package codes.bed.minestom.npc.listener

import codes.bed.minestom.npc.NpcManager
import codes.bed.minestom.npc.api.NpcInteraction
import codes.bed.minestom.npc.api.NpcInteractionType
import codes.bed.minestom.npc.types.AbstractNpcEntity
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerHand
import net.minestom.server.event.EventNode
import net.minestom.server.event.entity.EntityAttackEvent
import net.minestom.server.event.entity.EntityDamageEvent
import net.minestom.server.event.entity.EntityDeathEvent
import net.minestom.server.event.player.PlayerChatEvent
import net.minestom.server.event.player.PlayerDeathEvent
import net.minestom.server.event.player.PlayerEntityInteractEvent
import net.minestom.server.event.trait.InstanceEvent

object NpcListener {

    @Volatile
    private var manager: NpcManager? = null

    // Registers event listeners
    fun register(node: EventNode<InstanceEvent>, npcManager: NpcManager) {
        manager = npcManager
        node.addListener(PlayerEntityInteractEvent::class.java, this::onPlayerInteract)
        node.addListener(EntityAttackEvent::class.java, this::onEntityAttack)
        node.addListener(EntityDamageEvent::class.java, this::onEntityDamage)
        node.addListener(EntityDeathEvent::class.java, this::onEntityDeath)
        node.addListener(PlayerChatEvent::class.java, this::onPlayerChat)
        node.addListener(PlayerDeathEvent::class.java, this::onPlayerDeath)
    }

    private fun onPlayerInteract(event: PlayerEntityInteractEvent) {
        if (event.hand != PlayerHand.MAIN) return
        val npc = manager?.byEntityId(event.target.uuid) ?: return
        (npc as? AbstractNpcEntity)?.emitInteraction(
            NpcInteraction(
                npc = npc,
                player = event.player,
                hand = event.hand,
                type = NpcInteractionType.RIGHT_CLICK,
            )
        )
    }

    private fun onEntityAttack(event: EntityAttackEvent) {
        val player = event.entity as? Player ?: return
        val npc = manager?.byEntityId(event.target.uuid) ?: return
        (npc as? AbstractNpcEntity)?.emitInteraction(
            NpcInteraction(
                npc = npc,
                player = player,
                hand = PlayerHand.MAIN,
                type = NpcInteractionType.LEFT_CLICK,
            )
        )
    }

    private fun onEntityDamage(event: EntityDamageEvent) {}

    private fun onEntityDeath(event: EntityDeathEvent) {}

    private fun onPlayerChat(event: PlayerChatEvent) {}

    private fun onPlayerDeath(event: PlayerDeathEvent) {}
}