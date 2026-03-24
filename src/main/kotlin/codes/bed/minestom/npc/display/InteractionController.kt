package codes.bed.minestom.npc.display

import codes.bed.minestom.npc.StomNPCs
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.metadata.other.InteractionMeta
import net.minestom.server.instance.Instance
import java.util.*

class InteractionController(private var offset: Vec = Vec(0.0, 0.9, 0.0)) {
    private var entity: Entity? = null
    private var ownerUuid: UUID? = null

    fun attachTo(npc: Entity, instance: Instance) {
        if (entity == null) {
            entity = Entity(EntityType.INTERACTION).apply {
                setNoGravity(true)
                editEntityMeta(InteractionMeta::class.java) { meta ->
                    meta.width = 0.75f
                    meta.height = 1.0f
                    meta.response = true
                }
            }
        }

        ownerUuid = npc.uuid
        entity?.setInstance(instance, npc.position.add(offset))

        ownerUuid?.let { ownerId ->
            val owner = StomNPCs.manager().byEntityId(ownerId)
            if (owner != null && entity != null) {
                StomNPCs.manager().registerEntity(entity!!.uuid, owner)
            }
        }
    }

    fun detach() {
        entity?.let { e ->
            StomNPCs.manager().unregisterEntity(e.uuid)
            e.remove()
        }
        entity = null
        ownerUuid = null
    }

    fun syncWithNpc(npc: Entity) {
        entity?.teleport(npc.position.add(offset))
    }

    fun getEntity(): Entity? = entity
}

