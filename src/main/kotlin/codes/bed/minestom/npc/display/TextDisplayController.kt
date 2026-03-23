package codes.bed.minestom.npc.display

import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.metadata.display.TextDisplayMeta
import net.minestom.server.instance.Instance

class TextDisplayController(
    private var text: Component = Component.empty(),
    private var offset: Vec = Vec(0.0, 2.15, 0.0)
) {
    private var entity: Entity? = null

    fun attachTo(npc: Entity, instance: Instance) {
        if (entity == null) {
            entity = Entity(EntityType.TEXT_DISPLAY).apply {
                setNoGravity(true)
                editEntityMeta(TextDisplayMeta::class.java) { meta ->
                    meta.text = text
                }
            }
        }
        val pos = npc.position.add(offset)
        entity?.setInstance(instance, pos)
    }

    fun detach() {
        entity?.remove()
        entity = null
    }

    fun updateText(newText: Component) {
        text = newText
        entity?.editEntityMeta(TextDisplayMeta::class.java) { meta ->
            meta.text = text
        }
    }

    fun updateOffset(newOffset: Vec) {
        offset = newOffset
        entity?.let { e ->
            e.teleport(e.position.withX(offset.x()).withY(offset.y()).withZ(offset.z()))
        }
    }

    fun syncWithNpc(npc: Entity) {
        entity?.teleport(npc.position.add(offset))
    }

    fun getEntity(): Entity? = entity
}

