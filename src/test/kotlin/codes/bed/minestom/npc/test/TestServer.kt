package codes.bed.minestom.npc.test

import codes.bed.minestom.npc.StomNPCs
import codes.bed.minestom.npc.api.NpcInteractionType
import codes.bed.minestom.npc.types.EntityNpc
import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.*
import net.minestom.server.entity.metadata.avatar.PlayerMeta
import net.minestom.server.entity.metadata.display.TextDisplayMeta
import net.minestom.server.entity.metadata.other.InteractionMeta
import net.minestom.server.event.EventFilter
import net.minestom.server.event.EventNode
import net.minestom.server.event.entity.EntityAttackEvent
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerEntityInteractEvent
import net.minestom.server.event.player.PlayerPacketOutEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.LightingChunk
import net.minestom.server.instance.block.Block
import net.minestom.server.network.packet.server.play.PlayerInfoRemovePacket
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket
import net.minestom.server.utils.chunk.ChunkSupplier
import revxrsal.commands.annotation.Command
import revxrsal.commands.minestom.MinestomLamp
import java.util.*

fun main() {
    val minecraftServer = MinecraftServer.init()
    val instanceManager = MinecraftServer.getInstanceManager()
    val instanceContainer = instanceManager.createInstanceContainer()

    instanceContainer.setGenerator { unit ->
        unit.modifier().fillHeight(0, 40, Block.GRASS_BLOCK)
    }
    instanceContainer.chunkSupplier = ChunkSupplier { instance, x, z -> LightingChunk(instance, x, z) }

    val globalEventHandler = MinecraftServer.getGlobalEventHandler()
    val npcEventNode = EventNode.type("npcs", EventFilter.INSTANCE)
    globalEventHandler.addChild(npcEventNode)
    StomNPCs.initialize(npcEventNode)

    globalEventHandler.addListener(AsyncPlayerConfigurationEvent::class.java) { event ->
        event.spawningInstance = instanceContainer
        event.player.respawnPoint = Pos(0.0, 42.0, 0.0)
    }

    globalEventHandler.addListener(PlayerPacketOutEvent::class.java) { event ->
        println("[PACKET OUT] -> ${event.packet.javaClass.simpleName} to ${event.player.username}")
    }

    MinecraftServer.getExceptionManager().setExceptionHandler { e ->
        println("[MINESTOM EXCEPTION] Caught an internal error:")
        e.printStackTrace()
    }

    minecraftServer.start("0.0.0.0", 25565)

    val lamp = MinestomLamp.builder().build()
    lamp.register(TestCommands())
}

fun spawnNpc(instance: Instance, position: Pos, name: String, skin: PlayerSkin? = null): Entity {
    val npc = EntityNpc(
        npcType = EntityType.PLAYER,
        displayName = name,
        skin = skin,
    )

    npc.onInteract { interaction ->
        when (interaction.type) {
            NpcInteractionType.RIGHT_CLICK -> interaction.player.sendMessage(Component.text("You interacted with $name"))
            NpcInteractionType.LEFT_CLICK -> interaction.player.sendMessage(Component.text("You attacked $name"))
        }
    }

    npc.spawn()
    npc.setInstance(instance, position)
    return npc
}

fun spawnTestNpc(instance: Instance, position: Pos, skin: PlayerSkin, name: String): Entity {
    val nameTagOffset = Vec(0.0, 2.15, 0.0)
    val hitboxOffset = Vec(0.0, 0.9, 0.0)
    val profileName = name.filter { it.isLetterOrDigit() || it == '_' }.ifBlank { "npc" }.take(16)
    val npcUuid = UUID.randomUUID()

    lateinit var nameTag: Entity
    lateinit var hitbox: Entity

    val npc = object : Entity(EntityType.PLAYER, npcUuid) {
        override fun movementTick() {}

        override fun updateNewViewer(player: Player) {
            val properties = listOf(
                PlayerInfoUpdatePacket.Property("textures", skin.textures(), skin.signature())
            )
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
            super.updateNewViewer(player)
        }

        override fun updateOldViewer(player: Player) {
            super.updateOldViewer(player)
            player.sendPacket(PlayerInfoRemovePacket(uuid))
        }

        override fun update(time: Long) {
            super.update(time)
            if (nameTag.instance != null) nameTag.teleport(this.position.add(nameTagOffset))
            if (hitbox.instance != null) hitbox.teleport(this.position.add(hitboxOffset))
        }

        override fun remove() {
            if (nameTag.instance != null) nameTag.remove()
            if (hitbox.instance != null) hitbox.remove()
            super.remove()
        }
    }

    npc.setNoGravity(false)
    npc.setCustomName(Component.text(name))
    npc.isCustomNameVisible = true

    npc.editEntityMeta(PlayerMeta::class.java) { meta ->
        meta.isCapeEnabled = true
        meta.isJacketEnabled = true
        meta.isLeftSleeveEnabled = true
        meta.isRightSleeveEnabled = true
        meta.isLeftLegEnabled = true
        meta.isRightLegEnabled = true
        meta.isHatEnabled = false
    }

    nameTag = Entity(EntityType.TEXT_DISPLAY).apply {
        setNoGravity(true)
        editEntityMeta(TextDisplayMeta::class.java) { meta ->
            meta.text = Component.text(name)
        }
    }

    hitbox = Entity(EntityType.INTERACTION).apply {
        setNoGravity(true)
        editEntityMeta(InteractionMeta::class.java) { meta ->
            meta.width = 0.75f
            meta.height = 1.0f
            meta.response = true
        }
    }

    npc.setInstance(instance, position)
    nameTag.setInstance(instance, position.add(nameTagOffset))
    hitbox.setInstance(instance, position.add(hitboxOffset))

    val global = MinecraftServer.getGlobalEventHandler()

    global.addListener(PlayerEntityInteractEvent::class.java) { event ->
        if (event.target.uuid == hitbox.uuid || event.target.uuid == npc.uuid) {
            event.player.sendMessage(Component.text("You interacted with $name"))
        }
    }

    global.addListener(EntityAttackEvent::class.java) { event ->
        val player = event.entity as? Player ?: return@addListener
        if (event.target.uuid == hitbox.uuid || event.target.uuid == npc.uuid) {
            player.sendMessage(Component.text("You attacked $name"))
        }
    }

    return npc
}


fun getSkin(username: String): PlayerSkin? {
    return PlayerSkin.fromUsername(username)
}

class TestCommands {

    @Command("npc")
    fun npc(actor: Player) {
        spawnNpc(actor.instance, actor.position, "Notch", getSkin("Notch"))
    }

    @Command("gmc")
    fun gmc(actor: Player) {
        actor.gameMode = GameMode.CREATIVE
    }

    @Command("gms")
    fun gmsp(actor: Player) {
        actor.gameMode = GameMode.SURVIVAL
    }

    @Command("stop")
    fun stop(actor: Player) {
        MinecraftServer.stopCleanly()
    }
}