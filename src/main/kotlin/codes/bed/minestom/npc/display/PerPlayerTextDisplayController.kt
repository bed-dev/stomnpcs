package codes.bed.minestom.npc.display

import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta
import net.minestom.server.entity.metadata.display.TextDisplayMeta
import net.minestom.server.instance.Instance
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Per-player text display manager. Creates a private TEXT_DISPLAY entity for each player
 * that should see a client-only hologram and keeps it synchronized with the NPC.
 */
class PerPlayerTextDisplayController(
    private var offset: Vec = Vec(0.0, 2.15, 0.0)
) {
    private val displays = ConcurrentHashMap<UUID, Entity>()

    /** Show a private hologram to [player] positioned relative to [npc] in [instance]. */
    fun showFor(player: Player, npc: Entity, instance: Instance, text: Component) {
        hideFor(player)

        val entity = Entity(EntityType.TEXT_DISPLAY).apply {
            setNoGravity(true)
            editEntityMeta(TextDisplayMeta::class.java) { meta ->
                meta.text = text
                meta.billboardRenderConstraints = AbstractDisplayMeta.BillboardConstraints.CENTER
            }
        }

        val pos = npc.position.add(offset)
        entity.setInstance(instance, pos)
        entity.addViewer(player)
        displays[player.uuid] = entity
    }

    /** Hide and remove any private hologram shown to [player]. */
    fun hideFor(player: Player) {
        displays.remove(player.uuid)?.remove()
    }

    /** Update the private hologram text for [player], if present. */
    fun updateFor(player: Player, newText: Component) {
        displays[player.uuid]?.editEntityMeta(TextDisplayMeta::class.java) { meta ->
            meta.text = newText
            meta.billboardRenderConstraints = AbstractDisplayMeta.BillboardConstraints.CENTER
        }
    }

    /** Remove all private holograms managed by this controller. */
    fun detachAll() {
        displays.values.forEach { it.remove() }
        displays.clear()
    }
}

