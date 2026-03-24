package codes.bed.minestom.npc.types

import codes.bed.minestom.npc.StomNPCs
import codes.bed.minestom.npc.api.Npc
import codes.bed.minestom.npc.api.NpcInteraction
import codes.bed.minestom.npc.display.TextDisplayController
import codes.bed.minestom.npc.listener.NpcInteractListener
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.MetadataDef
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

abstract class AbstractNpcEntity(entityType: EntityType, uuid: UUID = UUID.randomUUID()) : Entity(entityType, uuid),
    Npc {

    private val interactionListeners = CopyOnWriteArrayList<NpcInteractListener>()
    override var textDisplayController: TextDisplayController? = null
    override var interactionController: codes.bed.minestom.npc.display.InteractionController? = null

    override val entity: Entity get() = this
    override fun onInteract(listener: NpcInteractListener) = apply { interactionListeners += listener }

    /**
     * Show or hide the name tag. When hiding, we clear the custom name and ensure it's not visible
     * so that the name is completely hidden (not just empty). When showing, restore the displayName.
     */
    override fun setNameTagVisible(visible: Boolean) = apply {
        if (!visible) {
            metadata.set(MetadataDef.CUSTOM_NAME, null)
            isCustomNameVisible = false
        } else {
            metadata.set(MetadataDef.CUSTOM_NAME, Component.text(displayName))
            isCustomNameVisible = true
        }
    }

    override fun movementTick() {
        // NPCs are static by default unless a concrete implementation adds movement.
    }

    override fun update(time: Long) {
        super.update(time)
        textDisplayController?.syncWithNpc(this)
        interactionController?.syncWithNpc(this)
    }

    override fun spawn() {
        StomNPCs.manager().register(this)
    }

    override fun remove() {
        textDisplayController?.detach()
        interactionController?.detach()
        StomNPCs.manager().unregister(this)
        super.remove()
    }

    internal fun emitInteraction(interaction: NpcInteraction) {
        interactionListeners.forEach { it.onInteract(interaction) }
    }
}