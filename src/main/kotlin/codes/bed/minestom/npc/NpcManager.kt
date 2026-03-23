package codes.bed.minestom.npc

import codes.bed.minestom.npc.listener.NpcListener
import net.minestom.server.event.EventNode
import net.minestom.server.event.trait.InstanceEvent

class NpcManager(private val eventNode: EventNode<InstanceEvent>) {

    init {
        NpcListener.register(eventNode)
    }

}