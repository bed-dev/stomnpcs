package codes.bed.minestom.npc.test

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.*
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
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

    npc.setInstance(instance, pos).thenRun {
        // player.sendPacket(PlayerInfoRemovePacket(npc.uuid))
    }
}

class TestCommands {

    @Command("npc")
    fun npc(actor: Player) {
        spawnTestNpc(
            instance = actor.instance!!,
            pos = actor.position,
            skin = actor.skin!!
        )
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