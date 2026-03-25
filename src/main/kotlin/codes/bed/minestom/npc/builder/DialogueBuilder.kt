package codes.bed.minestom.npc.builder

import codes.bed.minestom.npc.api.NameDisplayMode
import codes.bed.minestom.npc.display.TextDisplayController
import codes.bed.minestom.npc.listener.NpcInteractListener
import codes.bed.minestom.npc.types.EntityNpc
import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta
import net.minestom.server.entity.metadata.display.TextDisplayMeta
import java.util.concurrent.atomic.AtomicReference

class DialogueBuilder(private val npc: EntityNpc) {
    private val messages = ArrayList<Component>()
    private var delayMillis: Long = 50L
    private var holdDurationMillis: Long = 2500L
    private var displayOffset: Vec = Vec(0.0, 2.15, 0.0)

    fun message(line: String) = apply { messages += Component.text(line) }
    fun message(component: Component) = apply { messages += component }
    fun delay(ms: Long) = apply { delayMillis = ms }
    fun hold(ms: Long) = apply { holdDurationMillis = ms }
    fun offset(vec: Vec) = apply { displayOffset = vec }

    fun attachOnInteract() {
        // Prevent attaching multiple onInteract listeners for the same NPC
        if (npc.dialogueAttached) return
        npc.dialogueAttached = true

        val activePlayers = npc.activeDialoguePlayers

        npc.setDialogueListener(NpcInteractListener { interaction ->
            val player = interaction.player
            val playerId = player.uuid
            val instance = player.instance ?: return@NpcInteractListener
            if (!activePlayers.add(playerId)) return@NpcInteractListener

            // Cancel any previously scheduled dialogue task for this player (no reflection)
            npc.activeDialogueTasks.remove(playerId)?.get()?.cancel()

            val mode = npc.nameDisplayMode

            // Place the dialogue slightly below the NPC's name hologram so the name remains on top.
            val baseOffset: Vec = npc.textDisplayController?.getOffset()?.let { off ->
                Vec(off.x(), off.y() - 0.3, off.z())
            } ?: displayOffset

            val dialogue = TextDisplayController(Component.empty(), baseOffset)
            dialogue.attachTo(npc.entity, instance)
            // Ensure centered billboard rendering if supported
            dialogue.getEntity()?.editEntityMeta(TextDisplayMeta::class.java) { meta ->
                meta.billboardRenderConstraints = AbstractDisplayMeta.BillboardConstraints.CENTER
            }

            val hadTextController = npc.textDisplayController != null
            val originalName: Component? = npc.textDisplayController?.getText()
            val originalOffset: Vec? = npc.textDisplayController?.getOffset()
            val liftAmount = 0.5
            if (mode == NameDisplayMode.GLOBAL_HOLOGRAM && hadTextController && originalOffset != null) {
                if (activePlayers.size == 1) {
                    npc.savedTextDisplayOffset.compareAndSet(null, originalOffset)
                    val lifted = Vec(originalOffset.x(), originalOffset.y() + liftAmount, originalOffset.z())
                    npc.textDisplayController?.updateOffset(lifted)
                }
            }
            if (mode == NameDisplayMode.PER_PLAYER_HOLOGRAM) {
                npc.textDisplayController?.hideFrom(player)
                npc.perPlayerDisplayController?.showFor(player, npc, instance, Component.text(player.username))
            }

            var index = 0
            var charIdx = 0
            var nextTickAt = System.currentTimeMillis() + delayMillis
            var holdUntil = 0L
            var typing = true

            val scheduledRef = AtomicReference<net.minestom.server.timer.Task?>(null)
            npc.activeDialogueTasks[playerId] = scheduledRef

            val task = MinecraftServer.getSchedulerManager().buildTask {
                if (dialogue.getEntity() == null || !npc.entity.isActive) {
                    dialogue.detach()
                    if (mode == NameDisplayMode.PER_PLAYER_HOLOGRAM) {
                        npc.perPlayerDisplayController?.hideFor(player)
                        npc.textDisplayController?.showTo(player)
                    } else if (mode == NameDisplayMode.GLOBAL_HOLOGRAM) {
                        if (hadTextController && originalName != null && originalOffset != null) {
                            // Restore original global hologram text/offset only when no other dialogues are active
                            activePlayers.remove(playerId)
                            if (activePlayers.isEmpty()) {
                                val saved = npc.savedTextDisplayOffset.getAndSet(null) ?: originalOffset
                                npc.textDisplayController?.updateText(originalName)
                                npc.textDisplayController?.updateOffset(saved)
                            }
                        }
                    }
                    // ensure player removed if not already
                    activePlayers.remove(playerId)
                    npc.activeDialogueTasks.remove(playerId)?.get()?.cancel()
                    return@buildTask
                }

                val current = messages[index]
                val now = System.currentTimeMillis()

                // Ensure the dialogue display follows any runtime changes to the NPC's name offset
                val currentBaseOffset: Vec = npc.textDisplayController?.getOffset()?.let { off ->
                    Vec(off.x(), off.y() - 0.3, off.z())
                } ?: displayOffset
                dialogue.updateOffset(currentBaseOffset)

                if (typing) {
                    if (now >= nextTickAt) {
                        while (charIdx < current.toString().length && now >= nextTickAt) {
                            charIdx++
                            nextTickAt += delayMillis
                        }
                        if (charIdx > current.toString().length) charIdx = current.toString().length
                        dialogue.updateText(Component.text(current.toString().substring(0, charIdx)))
                        if (charIdx >= current.toString().length) {
                            typing = false
                            holdUntil = now + holdDurationMillis
                        }
                    }
                } else {
                    if (now >= holdUntil) {
                        index++
                        if (index >= messages.size) {
                            dialogue.detach()
                            if (mode == NameDisplayMode.PER_PLAYER_HOLOGRAM) {
                                npc.perPlayerDisplayController?.hideFor(player)
                                npc.textDisplayController?.showTo(player)
                            } else if (mode == NameDisplayMode.GLOBAL_HOLOGRAM) {
                                if (hadTextController && originalName != null && originalOffset != null) {
                                    activePlayers.remove(playerId)
                                    if (activePlayers.isEmpty()) {
                                        val saved = npc.savedTextDisplayOffset.getAndSet(null) ?: originalOffset
                                        npc.textDisplayController?.updateText(originalName)
                                        npc.textDisplayController?.updateOffset(saved)
                                    }
                                } else {
                                    activePlayers.remove(playerId)
                                }
                            }
                            npc.activeDialogueTasks.remove(playerId)?.get()?.cancel()
                            return@buildTask
                        }
                        charIdx = 0
                        typing = true
                        nextTickAt = now + delayMillis
                        dialogue.updateText(Component.text(""))
                    }
                }
            }

            val scheduled = task.repeat(net.minestom.server.timer.TaskSchedule.millis(10)).schedule()
            scheduledRef.set(scheduled)
        })
    }
}

fun EntityNpc.dialogue(init: DialogueBuilder.() -> Unit): DialogueBuilder {
    val b = DialogueBuilder(this)
    b.init()
    return b
}
