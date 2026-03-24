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

    /**
     * Register an additional entity id as belonging to the given NPC. This is used for
     * helper entities like text displays or interaction hitboxes so interactions on those
     * entities are forwarded to the owning NPC.
     */
    fun registerEntity(entityId: UUID, npc: Npc): NpcManager = apply {
        npcs[entityId] = npc
    }

    /** Unregister a previously registered additional entity id. */
    fun unregisterEntity(entityId: UUID): NpcManager = apply {
        npcs.remove(entityId)
    }

    fun unregister(npc: Npc): NpcManager = apply {
        npcs.remove(npc.entity.uuid)
    }

    fun byEntityId(entityId: UUID): Npc? {
        return npcs[entityId]
    }
}