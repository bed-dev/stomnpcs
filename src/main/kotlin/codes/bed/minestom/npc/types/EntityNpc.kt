package codes.bed.minestom.npc.types

import codes.bed.minestom.npc.api.NpcKind
import net.minestom.server.entity.EntityType
import java.util.*

class EntityNpc @JvmOverloads constructor(
    private val npcType: EntityType,
    override val displayName: String = npcType.name(),
    uuid: UUID = UUID.randomUUID(),
) : AbstractNpcEntity(npcType, uuid) {
    override val kind: NpcKind = NpcKind.ENTITY

    init {
        setNoGravity(true)
    }


}

