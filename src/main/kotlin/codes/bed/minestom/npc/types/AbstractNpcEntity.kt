package codes.bed.minestom.npc.types

import codes.bed.minestom.npc.StomNPCs
import codes.bed.minestom.npc.api.NameDisplayMode
import codes.bed.minestom.npc.api.Npc
import codes.bed.minestom.npc.api.NpcInteraction
import codes.bed.minestom.npc.display.InteractionController
import codes.bed.minestom.npc.display.PerPlayerTextDisplayController
import codes.bed.minestom.npc.display.TextDisplayController
import codes.bed.minestom.npc.listener.NpcInteractListener
import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.MetadataDef
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

@Suppress("UnstableApiUsage")
abstract class AbstractNpcEntity(entityType: EntityType, uuid: UUID = UUID.randomUUID()) : Entity(entityType, uuid),
    Npc {

    private val interactionListeners = CopyOnWriteArrayList<NpcInteractListener>()
    private var dialogueListener: NpcInteractListener? = null
    override var textDisplayController: TextDisplayController? = null
    override var interactionController: InteractionController? = null
    override var perPlayerDisplayController: PerPlayerTextDisplayController? = null
    override var nameDisplayMode: NameDisplayMode = NameDisplayMode.VANILLA


    @Volatile
    var dialogueAttached: Boolean = false

    val activeDialoguePlayers: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

    val activeDialogueTasks: ConcurrentHashMap<UUID, AtomicReference<net.minestom.server.timer.Task?>> =
        ConcurrentHashMap()

    /** When dialogues lift the global hologram, the original offset is stored here. */
    val savedTextDisplayOffset: AtomicReference<Vec?> = AtomicReference(null)


    override val entity: Entity get() = this
    override fun onInteract(listener: NpcInteractListener) = apply { interactionListeners += listener }

    /**
     * Replace any existing dialogue listener with the provided one. This prevents attaching
     * multiple dialogue listeners which would cause duplicate dialogue starts.
     */
    fun setDialogueListener(listener: NpcInteractListener) = apply {
        dialogueListener?.let { interactionListeners.remove(it) }
        dialogueListener = listener
        interactionListeners += listener
    }

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
    }


    override fun update(time: Long) {
        super.update(time)
        textDisplayController?.syncWithNpc(this)
        interactionController?.syncWithNpc(this)
    }

    override fun spawn() {
        StomNPCs.manager().register(this)
        when (nameDisplayMode) {
            NameDisplayMode.GLOBAL_HOLOGRAM -> instance?.let { textDisplayController?.attachTo(this, it) }
            NameDisplayMode.VANILLA -> {
                if (isCustomNameVisible) metadata.set(MetadataDef.CUSTOM_NAME, Component.text(displayName))
            }

            NameDisplayMode.PER_PLAYER_HOLOGRAM -> instance?.let { textDisplayController?.attachTo(this, it) }
        }
    }


    override fun getMetadataName(): Component? {
        return metadata.get(MetadataDef.CUSTOM_NAME)
    }

    override fun setMetadataName(name: Component?): Npc = apply {
        metadata.set(MetadataDef.CUSTOM_NAME, name)
    }


    override fun remove() {
        textDisplayController?.detach()
        interactionController?.detach()
        dialogueAttached = false
        StomNPCs.manager().unregister(this)
        super.remove()
    }

    internal fun emitInteraction(interaction: NpcInteraction) {
        interactionListeners.forEach { it.onInteract(interaction) }
    }
}