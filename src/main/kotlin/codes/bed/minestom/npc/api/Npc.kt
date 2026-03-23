package codes.bed.minestom.npc.api

import codes.bed.minestom.npc.listener.NpcInteractListener
import net.minestom.server.entity.Entity
import net.minestom.server.entity.metadata.display.TextDisplayMeta


interface Npc {
    val kind: NpcKind
    val displayName: String
    val entity: Entity

    fun onInteract(listener: NpcInteractListener): Npc

    fun setNameTagVisible(visible: Boolean): Npc

    fun setHologram(text: String?): Npc

    fun setHologramOffset(x: Double, y: Double, z: Double): Npc

    fun setTextDisplayMeta(meta: TextDisplayMeta): Npc

    fun setTypewriterEffect(enabled: Boolean, intervalMillis: Long = 40L): Npc

    fun setShowDialogueOnHologram(enabled: Boolean): Npc

}


