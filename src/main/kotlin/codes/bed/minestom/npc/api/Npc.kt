package codes.bed.minestom.npc.api

import codes.bed.minestom.npc.listener.NpcInteractListener
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Entity


interface Npc {
    val kind: NpcKind
    val displayName: String
    val entity: Entity

    var textDisplayController: codes.bed.minestom.npc.display.TextDisplayController?

    fun onInteract(listener: NpcInteractListener): Npc

    fun setNameTagVisible(visible: Boolean): Npc


    fun setTextDisplay(text: Component?): Npc = apply {
        textDisplayController?.updateText(text ?: Component.empty())
    }

    fun setTextDisplayOffset(x: Double, y: Double, z: Double): Npc = apply {
        textDisplayController?.updateOffset(net.minestom.server.coordinate.Vec(x, y, z))
    }

    fun setTextDisplayController(controller: codes.bed.minestom.npc.display.TextDisplayController?): Npc = apply {
        textDisplayController = controller
    }

    fun spawn()

}


