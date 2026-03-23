package codes.bed.minestom.npc.types

import codes.bed.minestom.npc.StomNPCs
import codes.bed.minestom.npc.api.NpcInteraction
import codes.bed.minestom.npc.api.Npc
import codes.bed.minestom.npc.listener.NpcInteractListener
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

abstract class AbstractNpcEntity(entityType: EntityType, uuid: UUID = UUID.randomUUID()) : Entity(entityType, uuid),
    Npc {

    private val interactionListeners = CopyOnWriteArrayList<NpcInteractListener>()

    override val entity: Entity get() = this
    override fun onInteract(listener: NpcInteractListener) = apply { interactionListeners += listener }

    override fun movementTick() {
        // NPCs are static by default unless a concrete implementation adds movement.
    }

    override fun spawn() {
        StomNPCs.manager().register(this)
    }

    override fun remove() {
        StomNPCs.managerOrNull()?.unregister(this)
        super.remove()
    }

    internal fun emitInteraction(interaction: NpcInteraction) {
        interactionListeners.forEach { it.onInteract(interaction) }
    }
}