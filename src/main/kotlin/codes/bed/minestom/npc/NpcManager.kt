package codes.bed.minestom.npc

import codes.bed.minestom.npc.api.Npc
import codes.bed.minestom.npc.listener.NpcListener
import net.minestom.server.event.EventNode
import net.minestom.server.event.trait.InstanceEvent
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class NpcManager(eventNode: EventNode<InstanceEvent>) {

    private val npcs = ConcurrentHashMap<UUID, Npc>()

    init {
        NpcListener.register(eventNode, this)
    }

    fun register(npc: Npc): NpcManager = apply {
        npcs[npc.entity.uuid] = npc
    }

    fun unregister(npc: Npc): NpcManager = apply {
        npcs.remove(npc.entity.uuid)
    }

    fun byEntityId(entityId: UUID): Npc? {
        return npcs[entityId]
    }
}