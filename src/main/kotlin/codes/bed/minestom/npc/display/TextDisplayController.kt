package codes.bed.minestom.npc.display

import codes.bed.minestom.npc.StomNPCs
import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.metadata.display.TextDisplayMeta
import net.minestom.server.instance.Instance
import java.util.*

class TextDisplayController(
    private var text: Component = Component.empty(),
    private var offset: Vec = Vec(0.0, 2.15, 0.0)
) {
    private var entity: Entity? = null
    private var ownerUuid: UUID? = null

    /**
     * Attach or move the text display to the provided npc entity in the given instance.
     * This will create the display entity on first attach and register it with the
     * NpcManager so interactions on the display are forwarded to the owning NPC.
     */
    fun attachTo(npc: Entity, instance: Instance) {
        if (entity == null) {
            entity = Entity(EntityType.TEXT_DISPLAY).apply {
                setNoGravity(true)
                editEntityMeta(TextDisplayMeta::class.java) { meta ->
                    meta.text = text
                }
            }
        }

        // store the owner so we can forward interactions
        ownerUuid = npc.uuid

        val pos = npc.position.add(offset)
        entity?.setInstance(instance, pos)

        // register the helper entity with the manager so NpcListener will map interactions
        ownerUuid?.let { ownerId ->
            val owner = StomNPCs.manager().byEntityId(ownerId)
            if (owner != null && entity != null) {
                StomNPCs.manager().registerEntity(entity!!.uuid, owner)
            }
        }
    }

    fun detach() {
        entity?.let { e ->
            // unregister helper mapping
            StomNPCs.manager().unregisterEntity(e.uuid)
            e.remove()
        }
        entity = null
        ownerUuid = null
    }

    fun updateText(newText: Component) {
        text = newText
        entity?.editEntityMeta(TextDisplayMeta::class.java) { meta ->
            meta.text = text
        }
    }

    fun updateOffset(newOffset: Vec) {
        offset = newOffset
        // move the display to the new offset relative to the owner
        ownerUuid?.let { ownerId ->
            val owner = StomNPCs.manager().byEntityId(ownerId)?.entity
            val e = entity
            if (owner != null && e != null) {
                e.teleport(owner.position.add(offset))
            }
        }
    }

    fun syncWithNpc(npc: Entity) {
        entity?.teleport(npc.position.add(offset))
    }

    fun getEntity(): Entity? = entity
}

