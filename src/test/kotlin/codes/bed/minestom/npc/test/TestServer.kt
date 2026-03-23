package codes.bed.minestom.npc.test

import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.*
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta
import net.minestom.server.entity.metadata.display.TextDisplayMeta
import net.minestom.server.entity.metadata.other.InteractionMeta
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerEntityInteractEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.LightingChunk
import net.minestom.server.instance.block.Block
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket
import net.minestom.server.utils.chunk.ChunkSupplier
import revxrsal.commands.annotation.Command
import revxrsal.commands.minestom.MinestomLamp


fun main() {
    // Initialization
    val minecraftServer = MinecraftServer.init()

    // Create the instance
    val instanceManager = MinecraftServer.getInstanceManager()
    val instanceContainer = instanceManager.createInstanceContainer()

    // Set the ChunkGenerator
    instanceContainer.setGenerator { unit ->
        unit.modifier().fillHeight(0, 40, Block.GRASS_BLOCK)
    }
    instanceContainer.chunkSupplier = ChunkSupplier { instance, x, z -> LightingChunk(instance, x, z) }

    // Add an event callback to specify the spawning instance (and the spawn position)
    val globalEventHandler = MinecraftServer.getGlobalEventHandler()
    globalEventHandler.addListener(AsyncPlayerConfigurationEvent::class.java) { event ->
        event.spawningInstance = instanceContainer
        event.player.respawnPoint = Pos(0.0, 42.0, 0.0)
    }

    minecraftServer.start("0.0.0.0", 25565)


    val lamp = MinestomLamp.builder().build()
    lamp.register(TestCommands())
}

fun spawnTestNpc(instance: Instance, pos: Pos, skin: PlayerSkin) {
    val npc = object : Entity(EntityType.PLAYER) {
        override fun updateNewViewer(player: Player) {
            val entry = PlayerInfoUpdatePacket.Entry(
                uuid, "NPC",
                listOf(PlayerInfoUpdatePacket.Property("textures", skin.textures(), skin.signature())),
                false, 0, GameMode.SURVIVAL, null, null, 0, false
            )
            player.sendPacket(PlayerInfoUpdatePacket(PlayerInfoUpdatePacket.Action.ADD_PLAYER, entry))
            super.updateNewViewer(player)
        }
    }

    val nameTag = Entity(EntityType.TEXT_DISPLAY)
    nameTag.editEntityMeta(TextDisplayMeta::class.java) { meta ->
        meta.text = Component.text("TEXT DISPLAY")
        meta.billboardRenderConstraints = AbstractDisplayMeta.BillboardConstraints.CENTER
        meta.backgroundColor = 0
        meta.translation = Vec(0.0, 2.3, 0.0)
    }


    val hitbox = Entity(EntityType.INTERACTION)
    hitbox.editEntityMeta(InteractionMeta::class.java) { meta ->
        meta.width = 0.6f
        meta.height = 1.8f
    }

    instance.eventNode().addListener(PlayerEntityInteractEvent::class.java) { event ->
        if (event.target == hitbox) {
            event.player.sendMessage(Component.text("You interacted with the NPC!"))
        }
    }

    npc.setInstance(instance, pos).thenRun {
        nameTag.setInstance(instance, pos).thenRun { npc.addPassenger(nameTag) }
        hitbox.setInstance(instance, pos).thenRun { npc.addPassenger(hitbox) }
    }
}

fun getSkin(username: String): PlayerSkin? {
    return PlayerSkin.fromUsername(username)
}

class TestCommands {

    @Command("npc")
    fun npc(actor: Player) {
        spawnTestNpc(actor.instance!!, actor.position, getSkin("Notch")!!)
    }

    @Command("gmc")
    fun gmc(actor: Player) {
        actor.gameMode = net.minestom.server.entity.GameMode.CREATIVE
    }

    @Command("gms")
    fun gmsp(actor: Player) {
        actor.gameMode = net.minestom.server.entity.GameMode.SURVIVAL
    }

    @Command("stop")
    fun stop(actor: Player) {
        MinecraftServer.stopCleanly()
    }


}