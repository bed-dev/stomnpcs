package codes.bed.minestom.npcs

import net.minestom.server.event.EventNode
import net.minestom.server.event.trait.InstanceEvent

class NpcManager(private val eventNode: EventNode<InstanceEvent>) {

    init {
        NpcListener.register(eventNode)
    }

}