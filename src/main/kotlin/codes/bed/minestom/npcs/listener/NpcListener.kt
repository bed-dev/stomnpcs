package codes.bed.minestom.npcs.listener

import net.minestom.server.event.EventNode
import net.minestom.server.event.entity.EntityAttackEvent
import net.minestom.server.event.entity.EntityDamageEvent
import net.minestom.server.event.entity.EntityDeathEvent
import net.minestom.server.event.player.PlayerChatEvent
import net.minestom.server.event.player.PlayerDeathEvent
import net.minestom.server.event.player.PlayerEntityInteractEvent
import net.minestom.server.event.trait.InstanceEvent

object NpcListener {

    // Registers event listeners
    fun register(node: EventNode<InstanceEvent>) {
        node.addListener(PlayerEntityInteractEvent::class.java, this::onPlayerInteract)
        node.addListener(EntityAttackEvent::class.java, this::onEntityAttack)
        node.addListener(EntityDamageEvent::class.java, this::onEntityDamage)
        node.addListener(EntityDeathEvent::class.java, this::onEntityDeath)
        node.addListener(PlayerChatEvent::class.java, this::onPlayerChat)
        node.addListener(PlayerDeathEvent::class.java, this::onPlayerDeath)
    }

    private fun onPlayerInteract(event: PlayerEntityInteractEvent) {}

    private fun onEntityAttack(event: EntityAttackEvent) {}

    private fun onEntityDamage(event: EntityDamageEvent) {}

    private fun onEntityDeath(event: EntityDeathEvent) {}

    private fun onPlayerChat(event: PlayerChatEvent) {}

    private fun onPlayerDeath(event: PlayerDeathEvent) {}
}