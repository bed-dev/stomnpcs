package codes.bed.minestom.npc.types

import codes.bed.minestom.npc.api.NpcKind
import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.*
import net.minestom.server.entity.metadata.avatar.PlayerMeta
import net.minestom.server.network.packet.server.play.*
import net.minestom.server.timer.Task
import net.minestom.server.timer.TaskSchedule
import java.util.*

@Suppress("UnstableApiUsage")
open class EntityNpc @JvmOverloads constructor(
    private val npcType: EntityType,
    override val displayName: String = npcType.name(),
    private val skin: PlayerSkin? = null,
    private val profileName: String = sanitizeProfileName(displayName),
    uuid: UUID = UUID.randomUUID(),
) : AbstractNpcEntity(npcType, uuid) {
    override val kind: NpcKind = NpcKind.ENTITY

    private val team = MinecraftServer.getTeamManager().createBuilder("hidden_tag")
        .nameTagVisibility(TeamsPacket.NameTagVisibility.NEVER)
        .build()

    private var lookAtNearestPlayerEnabled: Boolean = false
    private var lookAtNearestPlayerDistance: Double = 8.0
    private var lookTask: Task? = null

    init {
        setNoGravity(false)

        if (npcType == EntityType.PLAYER) {
            editEntityMeta(PlayerMeta::class.java) { meta ->
                meta.isCapeEnabled = true
                meta.isJacketEnabled = true
                meta.isLeftSleeveEnabled = true
                meta.isRightSleeveEnabled = true
                meta.isLeftLegEnabled = true
                meta.isRightLegEnabled = true
                meta.isHatEnabled = true
            }

        }
    }

    override fun setNameTagVisible(visible: Boolean) = apply {
        if (!visible) {
            metadata.set(MetadataDef.CUSTOM_NAME, null)
            isCustomNameVisible = false
        } else {
            metadata.set(MetadataDef.CUSTOM_NAME, Component.text(displayName))
            isCustomNameVisible = true
        }

        if (npcType == EntityType.PLAYER) {
            if (!visible) {
                if (!team.members.contains(profileName)) team.addMember(profileName)
            } else {
                if (team.members.contains(profileName)) team.removeMember(profileName)
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

            player.sendPacket(
                PlayerInfoUpdatePacket(
                    EnumSet.of(
                        PlayerInfoUpdatePacket.Action.ADD_PLAYER,
                        PlayerInfoUpdatePacket.Action.UPDATE_LISTED
                    ), listOf(entry)
                )
            )

        }
        super.updateNewViewer(player)

        // ensure player is not in reporting menu
        if (npcType == EntityType.PLAYER) {
            player.sendPacket(PlayerInfoRemovePacket(uuid))
        }
    }

    override fun updateOldViewer(player: Player) {
        super.updateOldViewer(player)

        if (npcType == EntityType.PLAYER) {
            player.sendPacket(PlayerInfoRemovePacket(uuid))
        }
    }

    /**
     * Enable or disable the look-at-nearest-player behavior for this NPC.
     * @param enabled Whether to enable the behavior.
     * @param distance The max distance to look for players.
     */
    fun setLookAtNearestPlayer(enabled: Boolean, distance: Double = 8.0) {
        lookTask?.cancel()
        lookAtNearestPlayerEnabled = enabled
        lookAtNearestPlayerDistance = distance
        
        if (enabled) {
            lookTask = MinecraftServer.getSchedulerManager().buildTask {
                val instance = this@EntityNpc.entity.instance ?: return@buildTask
                val npcPos = this@EntityNpc.entity.position
                val players = instance.players
                val closest = players
                    .filter { it.position.distance(npcPos) <= lookAtNearestPlayerDistance }
                    .minByOrNull { it.position.distance(npcPos) }
                if (closest != null) {
                    val lookPos = npcPos.withLookAt(closest.position)
                    val yaw = lookPos.yaw()
                    val pitch = lookPos.pitch()
                    for (player in players) {
                        player.sendPackets(
                            EntityHeadLookPacket(this@EntityNpc.entity.entityId, yaw),
                            EntityRotationPacket(
                                this@EntityNpc.entity.entityId,
                                yaw,
                                pitch,
                                this@EntityNpc.entity.isOnGround
                            )
                        )
                    }
                }
            }.repeat(TaskSchedule.nextTick()).schedule()
        }
    }

    override fun remove() {
        lookTask?.cancel()
        lookTask = null
        super.remove()
    }

    companion object {
        private fun sanitizeProfileName(name: String): String {
            return name.filter { it.isLetterOrDigit() || it == '_' }
                .take(16)
        }
    }
}
