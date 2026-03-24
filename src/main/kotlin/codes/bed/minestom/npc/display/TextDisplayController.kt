package codes.bed.minestom.npc.display

import codes.bed.minestom.npc.StomNPCs
import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta
import net.minestom.server.entity.metadata.display.TextDisplayMeta
import net.minestom.server.instance.Instance
import java.util.*

/**
 * Manages a global TEXT_DISPLAY entity used as a holographic name tag for an NPC.
 * The controller creates and positions the display and allows updating text and offset.
 */
class TextDisplayController(
    private var text: Component = Component.empty(),
    private var offset: Vec = Vec(0.0, 2.15, 0.0)
) {
    private var entity: Entity? = null
    private var ownerUuid: UUID? = null

    /** Attach or move the text display to the provided NPC in the given instance. */
    fun attachTo(npc: Entity, instance: Instance) {
        // Create the global text display entity if needed, otherwise update its meta
        val e = entity ?: Entity(EntityType.TEXT_DISPLAY).also {
            it.setNoGravity(true)
            it.editEntityMeta(TextDisplayMeta::class.java) { meta ->
                meta.text = text
                meta.billboardRenderConstraints = AbstractDisplayMeta.BillboardConstraints.CENTER
            }
            entity = it
        }

        ownerUuid = npc.uuid
        val pos = npc.position.add(offset)
        e.setInstance(instance, pos)

        // Register entity with NPC manager if we can resolve the owner
        val owner = StomNPCs.manager().byEntityId(ownerUuid!!)
        if (owner != null) {
            StomNPCs.manager().registerEntity(e.uuid, owner)
        }
    }

    /** Detach and remove the global display entity. */
    fun detach() {
        entity?.let { e ->
            try {
                StomNPCs.manager().unregisterEntity(e.uuid)
            } catch (_: Exception) {
                // ignore errors during unregister
            }
            e.remove()
        }
        entity = null
        ownerUuid = null
    }

    /** Update the displayed text. */
    fun updateText(newText: Component) {
        text = newText
        entity?.editEntityMeta(TextDisplayMeta::class.java) { meta ->
            meta.text = text
            meta.billboardRenderConstraints = AbstractDisplayMeta.BillboardConstraints.CENTER
        }
    }

    /** Update the offset used to position the display relative to the NPC. */
    fun updateOffset(newOffset: Vec) {
        offset = newOffset
        val owner = ownerUuid?.let { StomNPCs.manager().byEntityId(it)?.entity }
        val e = entity
        if (owner != null && e != null) {
            e.teleport(owner.position.add(offset))
        }
    }

    /** Teleport the display to follow the NPC position. */
    fun syncWithNpc(npc: Entity) {
        entity?.teleport(npc.position.add(offset))
    }

    fun getEntity(): Entity? = entity
    fun getText(): Component = text
    fun getOffset(): Vec = offset

    fun showTo(player: Player) {
        entity?.addViewer(player)
    }

    fun hideFrom(player: Player) {
        entity?.removeViewer(player)
    }
}

