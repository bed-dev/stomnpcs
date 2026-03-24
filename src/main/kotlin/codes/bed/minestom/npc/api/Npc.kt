package codes.bed.minestom.npc.api

import codes.bed.minestom.npc.display.InteractionController
import codes.bed.minestom.npc.display.PerPlayerTextDisplayController
import codes.bed.minestom.npc.display.TextDisplayController
import codes.bed.minestom.npc.listener.NpcInteractListener
import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity


interface Npc {
    val kind: NpcKind
    val displayName: String

    /**
     * The underlying Minestom entity represented by this NPC. Libraries users can
     * manipulate this entity (teleport, set velocity, etc.) to implement custom
     * movement/behaviour — automatic following/movement was intentionally removed.
     */
    val entity: Entity

    var textDisplayController: TextDisplayController?
    var interactionController: InteractionController?
    var perPlayerDisplayController: PerPlayerTextDisplayController?

    var nameDisplayMode: NameDisplayMode

    fun onInteract(listener: NpcInteractListener): Npc

    fun setNameTagVisible(visible: Boolean): Npc


    fun setTextDisplayController(controller: TextDisplayController?): Npc = apply {
        textDisplayController = controller
    }

    fun setInteractionController(controller: InteractionController?): Npc = apply {
        interactionController = controller
    }


    /**
     * Access the underlying custom name metadata (useful for saving/restoring state).
     * Implementations should use the entity metadata API rather than exposing protected fields.
     */
    fun getMetadataName(): Component?

    fun setMetadataName(name: Component?): Npc

    fun spawn()

    /** Convenience: teleport the underlying entity to [pos]. */
    fun moveTo(pos: Pos): Npc = apply { entity.teleport(pos) }

    /** Convenience: set the underlying entity velocity. */
    fun applyVelocity(velocity: Vec): Npc = apply { entity.setVelocity(velocity) }

}


