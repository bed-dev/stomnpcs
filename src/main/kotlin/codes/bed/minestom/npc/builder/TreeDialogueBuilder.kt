package codes.bed.minestom.npc.builder

import codes.bed.minestom.npc.dialogue.TreeDialogueNode
import codes.bed.minestom.npc.dialogue.TreeDialogueSession
import codes.bed.minestom.npc.listener.NpcInteractListener
import codes.bed.minestom.npc.types.EntityNpc
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.coordinate.Vec


class TreeDialogueBuilder(private val npc: EntityNpc) {
    var delayMillis: Long = 40L
    var holdDurationMillis: Long = 2500L
    var displayOffset: Vec = Vec(0.0, 2.15, 0.0)

    var optionsPrompt: Component = Component.text("Choose an option in chat:", NamedTextColor.GRAY)

    private var rootNode: TreeDialogueNode? = null

    fun delay(ms: Long) = apply { delayMillis = ms }
    fun hold(ms: Long) = apply { holdDurationMillis = ms }
    fun offset(vec: Vec) = apply { displayOffset = vec }

    fun prompt(component: Component) = apply { optionsPrompt = component }
    fun prompt(text: String) = apply { optionsPrompt = Component.text(text, NamedTextColor.GRAY) }

    fun root(init: TreeNodeBuilder.() -> Unit) {
        val b = TreeNodeBuilder()
        b.init()
        rootNode = b.build()
    }

    fun attachOnInteract() {
        val root = rootNode ?: return
        if (npc.dialogueAttached) return
        npc.dialogueAttached = true

        val activePlayers = npc.activeDialoguePlayers

        npc.setDialogueListener(NpcInteractListener { interaction ->
            val player = interaction.player
            val playerId = player.uuid

            if (!activePlayers.add(playerId)) return@NpcInteractListener
            npc.activeDialogueTasks.remove(playerId)?.get()?.cancel()

            val session = TreeDialogueSession(
                npc = npc,
                player = player,
                initialNode = root,
                delayMillis = delayMillis,
                holdDurationMillis = holdDurationMillis,
                displayOffset = displayOffset,
                optionsPrompt = optionsPrompt
            )
            session.start()
        })
    }
}

fun EntityNpc.treeDialogue(init: TreeDialogueBuilder.() -> Unit): TreeDialogueBuilder {
    val b = TreeDialogueBuilder(this)
    b.init()
    return b
}
