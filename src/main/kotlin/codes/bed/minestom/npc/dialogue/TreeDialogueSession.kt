package codes.bed.minestom.npc.dialogue

import codes.bed.minestom.npc.api.NameDisplayMode
import codes.bed.minestom.npc.display.TextDisplayController
import codes.bed.minestom.npc.types.EntityNpc
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta
import net.minestom.server.entity.metadata.display.TextDisplayMeta
import net.minestom.server.event.EventFilter
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerChatEvent
import net.minestom.server.event.trait.PlayerEvent
import net.minestom.server.timer.Task
import net.minestom.server.timer.TaskSchedule
import java.util.concurrent.atomic.AtomicReference

class TreeDialogueSession(
    private val npc: EntityNpc,
    private val player: Player,
    private val initialNode: TreeDialogueNode,
    private val delayMillis: Long,
    private val holdDurationMillis: Long,
    private val displayOffset: Vec,
    private val optionsPrompt: Component // Added to session properties
) {
    private val playerId = player.uuid
    private val instance = player.instance!!
    private val mode = npc.nameDisplayMode

    private var dialogueController: TextDisplayController? = null
    private var scheduledTaskRef = AtomicReference<Task?>(null)
    private var chatListenerNode: EventNode<PlayerEvent>? = null

    private var hadTextController = false
    private var originalName: Component? = null
    private var originalOffset: Vec? = null

    private enum class State { TYPING, HOLDING, WAITING_FOR_INPUT }

    fun start() {
        setupHologram()
        runNode(initialNode)
    }

    private fun setupHologram() {
        val baseOffset = npc.textDisplayController?.getOffset()?.let { off ->
            Vec(off.x(), off.y() - 0.3, off.z())
        } ?: displayOffset

        dialogueController = TextDisplayController(Component.empty(), baseOffset).apply {
            attachTo(npc.entity, instance)
            getEntity()?.editEntityMeta(TextDisplayMeta::class.java) { meta ->
                meta.billboardRenderConstraints = AbstractDisplayMeta.BillboardConstraints.CENTER
            }
        }

        hadTextController = npc.textDisplayController != null
        originalName = npc.textDisplayController?.getText()
        originalOffset = npc.textDisplayController?.getOffset()

        if (mode == NameDisplayMode.GLOBAL_HOLOGRAM && hadTextController && originalOffset != null) {
            if (npc.activeDialoguePlayers.size == 1) {
                npc.savedTextDisplayOffset.compareAndSet(null, originalOffset)
                val lifted = Vec(originalOffset!!.x(), originalOffset!!.y() + 0.5, originalOffset!!.z())
                npc.textDisplayController?.updateOffset(lifted)
            }
        }
        if (mode == NameDisplayMode.PER_PLAYER_HOLOGRAM) {
            npc.textDisplayController?.hideFrom(player)
            npc.perPlayerDisplayController?.showFor(player, npc, instance, Component.text(player.username))
        }
    }

    private fun runNode(node: TreeDialogueNode) {
        var index = 0
        var charIdx = 0
        var state = State.TYPING
        var nextTickAt = System.currentTimeMillis() + delayMillis
        var holdUntil = 0L

        val plainSerializer = PlainTextComponentSerializer.plainText()

        val task = MinecraftServer.getSchedulerManager().buildTask {
            if (dialogueController?.getEntity() == null || !npc.entity.isActive || !player.isActive || player.getDistanceSquared(
                    npc.entity
                ) > 100
            ) {
                endSession()
                return@buildTask
            }

            val currentBaseOffset = npc.textDisplayController?.getOffset()?.let { off ->
                Vec(off.x(), off.y() - 0.3, off.z())
            } ?: displayOffset
            dialogueController?.updateOffset(currentBaseOffset)

            if (state == State.WAITING_FOR_INPUT) return@buildTask

            val now = System.currentTimeMillis()
            val currentLine: Component? = node.lines.getOrNull(index)

            if (currentLine != null) {
                val totalLength = plainSerializer.serialize(currentLine).length

                if (state == State.TYPING) {
                    if (now >= nextTickAt) {
                        while (charIdx < totalLength && now >= nextTickAt) {
                            charIdx++
                            nextTickAt += delayMillis
                        }
                        if (charIdx > totalLength) charIdx = totalLength

                        dialogueController?.updateText(truncateComponent(currentLine, charIdx))

                        if (charIdx >= totalLength) {
                            state = State.HOLDING
                            holdUntil = now + holdDurationMillis
                        }
                    }
                } else if (state == State.HOLDING) {
                    if (now >= holdUntil) {
                        index++
                        if (index < node.lines.size) {
                            charIdx = 0
                            state = State.TYPING
                            nextTickAt = now + delayMillis
                            dialogueController?.updateText(Component.empty())
                        }
                    }
                }
            }

            if (index >= node.lines.size) {
                if (node.options.isEmpty()) {
                    endSession()
                } else {
                    state = State.WAITING_FOR_INPUT
                    presentOptions(node)
                }
            }
        }

        val scheduled = task.repeat(TaskSchedule.millis(10)).schedule()
        scheduledTaskRef.set(scheduled)
        npc.activeDialogueTasks[playerId] = scheduledTaskRef
    }

    private fun presentOptions(node: TreeDialogueNode) {
        // Send the customizable prompt instead of the hardcoded string
        player.sendMessage(optionsPrompt)

        node.options.forEachIndexed { i, opt ->
            val msg = Component.text("${i + 1}. ", NamedTextColor.YELLOW)
                .append(opt.text.colorIfAbsent(NamedTextColor.WHITE))
            player.sendMessage(msg)
        }

        chatListenerNode = EventNode.type("dialogue-chat-${playerId}", EventFilter.PLAYER)
        chatListenerNode?.addListener(PlayerChatEvent::class.java) { event ->
            if (event.player.uuid != playerId) return@addListener

            val input = event.rawMessage.trim()
            val selectedIndex = input.toIntOrNull()?.minus(1)

            if (selectedIndex != null && selectedIndex in node.options.indices) {
                event.isCancelled = true
                val choice = node.options[selectedIndex]

                MinecraftServer.getGlobalEventHandler().removeChild(chatListenerNode!!)
                chatListenerNode = null
                dialogueController?.updateText(Component.empty())

                choice.onSelect?.invoke(player)
                scheduledTaskRef.get()?.cancel()

                if (choice.nextNode != null) {
                    runNode(choice.nextNode)
                } else {
                    endSession()
                }
            }
        }
        MinecraftServer.getGlobalEventHandler().addChild(chatListenerNode!!)
    }

    private fun endSession() {
        scheduledTaskRef.get()?.cancel()
        npc.activeDialogueTasks.remove(playerId)

        chatListenerNode?.let {
            MinecraftServer.getGlobalEventHandler().removeChild(it)
            chatListenerNode = null
        }

        dialogueController?.detach()

        if (mode == NameDisplayMode.PER_PLAYER_HOLOGRAM) {
            npc.perPlayerDisplayController?.hideFor(player)
            npc.textDisplayController?.showTo(player)
        } else if (mode == NameDisplayMode.GLOBAL_HOLOGRAM) {
            if (hadTextController && originalName != null && originalOffset != null) {
                npc.activeDialoguePlayers.remove(playerId)
                if (npc.activeDialoguePlayers.isEmpty()) {
                    val saved = npc.savedTextDisplayOffset.getAndSet(null) ?: originalOffset
                    npc.textDisplayController?.updateText(originalName!!)
                    npc.textDisplayController?.updateOffset(saved!!)
                }
            } else {
                npc.activeDialoguePlayers.remove(playerId)
            }
        } else {
            npc.activeDialoguePlayers.remove(playerId)
        }
    }

    private fun truncateComponent(component: Component, maxLength: Int): Component {
        if (maxLength <= 0) return Component.empty().style(component.style())
        var remaining = maxLength

        fun process(c: Component): Component {
            if (remaining <= 0) return Component.empty().style(c.style())

            var newC = c.children(emptyList())

            if (newC is TextComponent) {
                val content = newC.content()
                if (content.length > remaining) {
                    newC = newC.content(content.substring(0, remaining))
                    remaining = 0
                } else {
                    remaining -= content.length
                }
            }

            if (remaining > 0 && c.children().isNotEmpty()) {
                val newChildren = mutableListOf<Component>()
                for (child in c.children()) {
                    if (remaining <= 0) break
                    newChildren.add(process(child))
                }
                newC = newC.children(newChildren)
            }

            return newC
        }

        return process(component)
    }
}