package codes.bed.minestom.npc.types

import codes.bed.minestom.npc.api.Npc
import codes.bed.minestom.npc.listener.NpcInteractListener
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import java.util.*

abstract class AbstractNpcEntity(entityType: EntityType, uuid: UUID = UUID.randomUUID()) : Entity(entityType, uuid),
    Npc {

    private val interactionListeners = mutableListOf<NpcInteractListener>()

    override val entity: Entity get() = this
    override fun onInteract(listener: NpcInteractListener) = apply { interactionListeners += listener }
}