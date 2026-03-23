package codes.bed.minestom.npc.types

import codes.bed.minestom.npc.StomNPCs
import codes.bed.minestom.npc.api.Npc
import codes.bed.minestom.npc.api.NpcInteraction
import codes.bed.minestom.npc.listener.NpcInteractListener
import codes.bed.minestom.npc.display.TextDisplayController
import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.metadata.display.TextDisplayMeta
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

abstract class AbstractNpcEntity(entityType: EntityType, uuid: UUID = UUID.randomUUID()) : Entity(entityType, uuid),
    Npc {

    private val interactionListeners = CopyOnWriteArrayList<NpcInteractListener>()
    override var textDisplayController: TextDisplayController? = null

    override val entity: Entity get() = this
    override fun onInteract(listener: NpcInteractListener) = apply { interactionListeners += listener }
    override fun setNameTagVisible(visible: Boolean) = apply { isCustomNameVisible = visible }

    override fun movementTick() {
        // NPCs are static by default unless a concrete implementation adds movement.
    }

    override fun update(time: Long) {
        super.update(time)
        textDisplayController?.syncWithNpc(this)
    }

    override fun spawn() {
        StomNPCs.manager().register(this)
    }

    override fun remove() {
        textDisplayController?.detach()
        StomNPCs.manager().unregister(this)
        super.remove()
    }
    internal fun emitInteraction(interaction: NpcInteraction) {
        interactionListeners.forEach { it.onInteract(interaction) }
    }
}