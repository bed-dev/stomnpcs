package codes.bed.minestom.npc

import net.minestom.server.event.EventNode
import net.minestom.server.event.trait.InstanceEvent

object StomNPCs {
    @Volatile
    private var manager: NpcManager? = null

    @JvmStatic
    @Synchronized
    fun initialize(eventNode: EventNode<InstanceEvent>): NpcManager {
        manager?.let { return it }

        val created = NpcManager(eventNode)
        manager = created
        return created
    }

    @JvmStatic
    fun manager(): NpcManager {
        return manager
            ?: error("StomNPCs is not initialized. Call StomNPCs.initialize(eventNode) first.")
    }
}