package codes.bed.minestom.npc.test

import codes.bed.minestom.npc.StomNPCs
import codes.bed.minestom.npc.api.NameDisplayMode
import codes.bed.minestom.npc.api.NpcInteractionType
import codes.bed.minestom.npc.builder.NpcBuilder
import codes.bed.minestom.npc.builder.dialogue
import codes.bed.minestom.npc.builder.treeDialogue
import codes.bed.minestom.npc.display.InteractionController
import codes.bed.minestom.npc.display.TextDisplayController
import codes.bed.minestom.npc.types.EntityNpc
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.*
import net.minestom.server.entity.metadata.avatar.PlayerMeta
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta
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

fun spawnNpc(instance: Instance, position: Pos, name: String, npcType: EntityType): Entity {
    val textController = TextDisplayController(Component.text(name))
    val interactController = InteractionController()
    val npc = EntityNpc(
        npcType = npcType,
        displayName = name,
    )
        .setNameTagVisible(true)
        .setTextDisplayController(textController)
        .setInteractionController(interactController)
    // This NPC uses a text display controller for its name; prefer the global hologram mode
    npc.nameDisplayMode = NameDisplayMode.GLOBAL_HOLOGRAM

    npc.onInteract { interaction ->
        when (interaction.type) {
            NpcInteractionType.RIGHT_CLICK -> interaction.player.sendMessage(Component.text("You interacted with $name"))
            NpcInteractionType.LEFT_CLICK -> interaction.player.sendMessage(Component.text("You attacked $name"))
        }
    }

    npc.spawn()
    npc.entity.setInstance(instance, position)
    textController.attachTo(npc.entity, instance)
    interactController.attachTo(npc.entity, instance)
    return npc.entity
}

fun spawnTestNpc(instance: Instance, position: Pos, skin: PlayerSkin, name: String): Entity {
    val nameTagOffset = Vec(0.0, 2.15, 0.0)
    val hitboxOffset = Vec(0.0, 0.9, 0.0)
    val profileName = name.filter { it.isLetterOrDigit() || it == '_' }.ifBlank { "npc" }.take(16)
    val npcUuid = UUID.randomUUID()

    lateinit var nameTag: Entity
    lateinit var hitbox: Entity

    val npc = object : EntityNpc(
        EntityType.PLAYER,
        displayName = name,
        skin = skin,
        profileName = profileName,
        uuid = npcUuid
    ) {
        override fun movementTick() {}

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
    // Use Npc API to set visible name and metadata
    npc.setNameTagVisible(true)
    npc.setMetadataName(Component.text(name))

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

    MinecraftServer.getGlobalEventHandler().addListener(PlayerEntityInteractEvent::class.java) { event ->
        if (event.target.uuid == hitbox.uuid || event.target.uuid == npc.uuid) {
            event.player.sendMessage(Component.text("You interacted with $name"))
        }
    }

    MinecraftServer.getGlobalEventHandler().addListener(EntityAttackEvent::class.java) { event ->
        val player = event.entity as? Player ?: return@addListener
        if (event.target.uuid == hitbox.uuid || event.target.uuid == npc.uuid) {
            player.sendMessage(Component.text("You attacked $name"))
        }
    }

    return npc
}


class TestCommands {

    @Command("npc")
    fun npc(actor: Player) {
        val npc = EntityNpc(
            npcType = EntityType.PLAYER,
            displayName = "Notch (Nametag)",
        )
            .setNameTagVisible(true)
        npc.onInteract { interaction ->
            when (interaction.type) {
                NpcInteractionType.RIGHT_CLICK -> interaction.player.sendMessage(Component.text("You interacted with Notch (Nametag)"))
                NpcInteractionType.LEFT_CLICK -> interaction.player.sendMessage(Component.text("You attacked Notch (Nametag)"))
            }
        }
        npc.spawn()
        npc.entity.setInstance(actor.instance, actor.position)
    }

    @Command("npc text")
    fun npcText(actor: Player) {
        val textController = TextDisplayController(Component.text("Custom TextDisplay for Notch"))
        val npc = EntityNpc(
            npcType = EntityType.PLAYER,
            displayName = "   ",
        )
            .setNameTagVisible(false)
            .setTextDisplayController(textController)
        npc.nameDisplayMode = NameDisplayMode.GLOBAL_HOLOGRAM
        npc.onInteract { interaction ->
            when (interaction.type) {
                NpcInteractionType.RIGHT_CLICK -> interaction.player.sendMessage(Component.text("You interacted with Notch (TextDisplay)"))
                NpcInteractionType.LEFT_CLICK -> interaction.player.sendMessage(Component.text("You attacked Notch (TextDisplay)"))
            }
        }

        npc.spawn()
        npc.entity.setInstance(actor.instance, actor.position)
        textController.attachTo(npc.entity, actor.instance)
        npc.setNameTagVisible(false)
        npc.setMetadataName(null)
    }

    @Command("npc typewriter")
    fun npcTypewriter(actor: Player) {
        val instance = actor.instance ?: return

        val npc = EntityNpc(
            npcType = EntityType.PLAYER,
            displayName = "   ",
        ).setNameTagVisible(false)

        npc.spawn()
        npc.entity.setInstance(instance, actor.position)
        npc.onInteract { interaction ->
            when (interaction.type) {
                NpcInteractionType.RIGHT_CLICK -> interaction.player.sendMessage(Component.text("You interacted with the Typewriter NPC"))
                NpcInteractionType.LEFT_CLICK -> interaction.player.sendMessage(Component.text("You attacked the Typewriter NPC"))
            }

            val textDisplay = Entity(EntityType.TEXT_DISPLAY).apply {
                setNoGravity(true)
                setInstance(instance, npc.position.add(0.0, 2.3, 0.0))
            }

            val message = "Hello Player, Welcome to the server."
            val delayMillis = 40L
            val holdDurationMillis = 4000L

            var charIndex = 0
            var nextTickAt = System.currentTimeMillis() + delayMillis
            var holdUntil = 0L
            var isTypingFinished = false

            MinecraftServer.getSchedulerManager().buildTask {
                if (!textDisplay.isActive || !npc.entity.isActive) {
                    return@buildTask
                }

                val now = System.currentTimeMillis()

                if (!isTypingFinished) {
                    if (now >= nextTickAt) {
                        while (charIndex < message.length && now >= nextTickAt) {
                            charIndex++
                            nextTickAt += delayMillis
                        }

                        if (charIndex > message.length) {
                            charIndex = message.length
                        }

                        textDisplay.editEntityMeta(TextDisplayMeta::class.java) { meta ->
                            meta.text = Component.text(message.substring(0, charIndex))
                            meta.billboardRenderConstraints = AbstractDisplayMeta.BillboardConstraints.CENTER
                        }

                        if (charIndex >= message.length) {
                            isTypingFinished = true
                            holdUntil = now + holdDurationMillis
                        }
                    }
                } else {
                    if (now >= holdUntil) {
                        textDisplay.remove()
                        return@buildTask
                    }
                }
            }.repeat(net.minestom.server.timer.TaskSchedule.millis(10)).schedule()
        }



        actor.sendMessage(Component.text("Spawned tagless typewriter NPC!"))
    }


    @Command("npc dialogue")
    fun npcDialogue(actor: Player) {
        val instance = actor.instance ?: return
        val pos = actor.position

        val npcName = "Guide"
        val npc = EntityNpc(
            npcType = EntityType.PLAYER,
            displayName = "   ",
        ).setNameTagVisible(false)

        npc.spawn()
        npc.entity.setInstance(instance, pos)

        val idleNameHeight = 2.1
        val speakingNameHeight = 2.4

        val nameController = TextDisplayController(Component.text(npcName), Vec(0.0, idleNameHeight, 0.0))
        val interactController = InteractionController(Vec(0.0, 0.9, 0.0))

        npc.setTextDisplayController(nameController)
        npc.nameDisplayMode = NameDisplayMode.GLOBAL_HOLOGRAM
        npc.setInteractionController(interactController)

        nameController.attachTo(npc.entity, instance)
        interactController.attachTo(npc.entity, instance)

        npc.onInteract {
            val dialogueController = TextDisplayController(Component.empty(), Vec(0.0, idleNameHeight, 0.0))
            dialogueController.attachTo(npc.entity, instance)
            // ensure centered billboard rendering if supported
            dialogueController.getEntity()?.editEntityMeta(TextDisplayMeta::class.java) { meta ->
                meta.billboardRenderConstraints = AbstractDisplayMeta.BillboardConstraints.CENTER
            }

            val messages = listOf(
                "Hello Player",
                "Welcome to the server.",
                "I have a lot to tell you.",
                "But let's start with the basics."
            )

            val delayMillis = 40L
            val holdDurationMillis = 2500L

            var messageIndex = 0
            var charIndex = 0
            var nextTickAt = System.currentTimeMillis() + delayMillis
            var holdUntil = 0L
            var isTyping = true

            nameController.updateOffset(Vec(0.0, speakingNameHeight, 0.0))

            MinecraftServer.getSchedulerManager().buildTask {
                val now = System.currentTimeMillis()
                if (!npc.entity.isActive || dialogueController.getEntity() == null) {
                    dialogueController.detach()
                    return@buildTask
                }

                val currentMessage = messages[messageIndex]

                if (isTyping) {
                    if (now >= nextTickAt) {
                        while (charIndex < currentMessage.length && now >= nextTickAt) {
                            charIndex++
                            nextTickAt += delayMillis
                        }

                        if (charIndex > currentMessage.length) charIndex = currentMessage.length

                        dialogueController.updateText(Component.text(currentMessage.substring(0, charIndex)))

                        if (charIndex >= currentMessage.length) {
                            isTyping = false
                            holdUntil = now + holdDurationMillis
                        }
                    }
                } else {
                    if (now >= holdUntil) {
                        messageIndex++
                        if (messageIndex >= messages.size) {
                            dialogueController.detach()
                            nameController.updateOffset(Vec(0.0, idleNameHeight, 0.0))
                            return@buildTask
                        } else {
                            charIndex = 0
                            isTyping = true
                            nextTickAt = now + delayMillis
                            dialogueController.updateText(Component.text(""))
                        }
                    }
                }
            }.repeat(net.minestom.server.timer.TaskSchedule.millis(10)).schedule()

            actor.sendMessage(Component.text("Started multi-line dialogue sequence!"))
        }
    }

    @Command("npc dsl")
    fun npcDialogueSimple(actor: Player) {
        val instance = actor.instance ?: return
        val pos = actor.position

        val npc = NpcBuilder().apply {
            type = EntityType.PLAYER
            name = "Guide"
            nameTagVisible = false
            textDisplay(Component.text("Guide"), Vec(0.0, 2.1, 0.0))
            interaction(Vec(0.0, 0.9, 0.0))
            setLookAtNearestPlayer(true, 5.0)
        }.spawn(instance, pos)

        val entityNpc = StomNPCs.manager().byEntityId(npc.uuid) as EntityNpc


        entityNpc.dialogue {
            message("Hello Player")
            message("Welcome to the server.")
            message("I have a lot to tell you.")
            message("But let's start with the basics.")
            delay(40)
            hold(2500)
            offset(Vec(0.0, 2.1, 0.0))
        }
            .attachOnInteract()

        actor.sendMessage(Component.text("Spawned simple dialogue NPC"))
    }


    @Command("npc tree")
    fun npcDialogueTree(actor: Player) {
        val instance = actor.instance ?: return
        val pos = actor.position

        val npc = NpcBuilder().apply {
            type = EntityType.PLAYER
            name = "Guide"
            nameTagVisible = false
            textDisplay(Component.text("Guide"), Vec(0.0, 2.1, 0.0))
            interaction(Vec(0.0, 0.9, 0.0))
        }.spawn(instance, pos)

        val entityNpc = StomNPCs.manager().byEntityId(npc.uuid) as EntityNpc

        entityNpc.treeDialogue {
            delay(30)
            hold(2000)

            // Using simple text (defaults to gray):
//            prompt("Please respond with a number:")

            // OR using an Adventure Component for full formatting:
            prompt(Component.text("Make a choice!").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD))

            root {
                line("Welcome to the server!")
                option("Tell me more") {
                    line("It's a great place.")
                }
            }
        }.attachOnInteract()

        entityNpc.onInteract { }
        actor.sendMessage(Component.text("Spawned simple dialogue NPC"))
    }
    @Command("npc follower")
    fun npcFollower(actor: Player) {
        val instance = actor.instance ?: return
        val pos = actor.position

        val npc = EntityNpc(
            npcType = EntityType.PLAYER,
            displayName = "Follower",
        ).setNameTagVisible(true)

        // If you want the NPC to move, manipulate `npc.entity` directly (e.g. teleport or set velocity).
        npc.spawn()
        npc.entity.setInstance(instance, pos)
        actor.sendMessage(Component.text("Spawned follower NPC. Move it manually using the NPC's entity API if desired."))
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