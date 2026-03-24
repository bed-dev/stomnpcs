package codes.bed.minestom.npc.display

import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.display.TextDisplayMeta
import net.minestom.server.instance.Instance
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages a private text display per-player. For each player that starts a dialogue, we
 * spawn a separate TEXT_DISPLAY entity and only add that player as a viewer.
 */
class PerPlayerTextDisplayController(
    private var defaultText: Component = Component.empty(),
    private var offset: Vec = Vec(0.0, 2.15, 0.0)
) {
    private val displays = ConcurrentHashMap<UUID, Entity>()

    fun showFor(player: Player, npc: Entity, instance: Instance, text: Component) {
        hideFor(player)

        val entity = Entity(EntityType.TEXT_DISPLAY).apply {
            setNoGravity(true)
            editEntityMeta(TextDisplayMeta::class.java) { meta ->
                meta.text = text
            }
        }
        val pos = npc.position.add(offset)
        entity.setInstance(instance, pos)
        entity.addViewer(player)
        displays[player.uuid] = entity
    }

    fun hideFor(player: Player) {
        displays.remove(player.uuid)?.let { it.remove() }
    }

    fun updateFor(player: Player, newText: Component) {
        displays[player.uuid]?.editEntityMeta(TextDisplayMeta::class.java) { meta ->
            meta.text = newText
        }
    }

    fun detachAll() {
        displays.values.forEach { it.remove() }
        displays.clear()
    }
}

