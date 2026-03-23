package codes.bed.minestom.npc.types

import codes.bed.minestom.npc.api.NpcKind
import net.kyori.adventure.text.Component
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerSkin
import net.minestom.server.entity.metadata.avatar.PlayerMeta
import net.minestom.server.network.packet.server.play.PlayerInfoRemovePacket
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket
import java.util.*

class EntityNpc @JvmOverloads constructor(
    private val npcType: EntityType,
    override val displayName: String = npcType.name(),
    private val skin: PlayerSkin? = null,
    private val profileName: String = sanitizeProfileName(displayName),
    uuid: UUID = UUID.randomUUID(),
) : AbstractNpcEntity(npcType, uuid) {
    override val kind: NpcKind = NpcKind.ENTITY

    init {
        setNoGravity(false)
        setCustomName(Component.text(displayName))
        isCustomNameVisible = true

        if (npcType == EntityType.PLAYER) {
            editEntityMeta(PlayerMeta::class.java) { meta ->
                meta.isCapeEnabled = true
                meta.isJacketEnabled = true
                meta.isLeftSleeveEnabled = true
                meta.isRightSleeveEnabled = true
                meta.isLeftLegEnabled = true
                meta.isRightLegEnabled = true
                meta.isHatEnabled = false
            }
        }
    }

    override fun updateNewViewer(player: Player) {
        if (npcType == EntityType.PLAYER) {
            val properties = skin?.let {
                listOf(PlayerInfoUpdatePacket.Property("textures", it.textures(), it.signature()))
            } ?: emptyList()

            val entry = PlayerInfoUpdatePacket.Entry(
                uuid,
                profileName,
                properties,
                false,
                0,
                GameMode.SURVIVAL,
                null,
                null,
                0,
                true
            )

            player.sendPacket(PlayerInfoUpdatePacket(PlayerInfoUpdatePacket.Action.ADD_PLAYER, entry))
        }
        super.updateNewViewer(player)
    }

    override fun updateOldViewer(player: Player) {
        super.updateOldViewer(player)
        if (npcType == EntityType.PLAYER) {
            player.sendPacket(PlayerInfoRemovePacket(uuid))
        }
    }

    companion object {
        private fun sanitizeProfileName(name: String): String {
            return name.filter { it.isLetterOrDigit() || it == '_' }
                .ifBlank { "npc" }
                .take(16)
        }
    }
}
