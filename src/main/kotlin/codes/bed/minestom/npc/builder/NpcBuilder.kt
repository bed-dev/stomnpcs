package codes.bed.minestom.npc.builder

import codes.bed.minestom.npc.api.NameDisplayMode
import codes.bed.minestom.npc.api.NpcInteraction
import codes.bed.minestom.npc.display.InteractionController
import codes.bed.minestom.npc.display.TextDisplayController
import codes.bed.minestom.npc.types.EntityNpc
import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.PlayerSkin
import net.minestom.server.instance.Instance

class NpcBuilder {
    var type: EntityType = EntityType.PLAYER
    var name: String = "npc"
    var skin: PlayerSkin? = null
    var nameTagVisible: Boolean = true

    private var textComponent: Component? = null
    private var textOffset: Vec = Vec(0.0, 2.15, 0.0)

    private var interactionOffset: Vec? = null

    private var interactHandler: ((NpcInteraction) -> Unit)? = null
    private var dialogueInit: (DialogueBuilder.() -> Unit)? = null

    fun textDisplay(text: Component, offset: Vec = Vec(0.0, 2.15, 0.0)) {
        this.textComponent = text
        this.textOffset = offset
    }

    fun interaction(offset: Vec = Vec(0.0, 0.9, 0.0)) {
        this.interactionOffset = offset
    }

    fun onInteract(handler: (NpcInteraction) -> Unit) {
        this.interactHandler = handler
    }

    fun dialogue(init: DialogueBuilder.() -> Unit) {
        this.dialogueInit = init
    }

    fun build(): EntityNpc {
        val npc = EntityNpc(
            npcType = type,
            displayName = name,
            skin = skin
        )

        npc.setNameTagVisible(nameTagVisible)

        textComponent?.let { comp ->
            val controller = TextDisplayController(comp, textOffset)
            npc.setTextDisplayController(controller)
        }

        interactionOffset?.let { off ->
            val controller = InteractionController(off)
            npc.setInteractionController(controller)
        }

        interactHandler?.let { h ->
            npc.onInteract { h(it) }
        }

        // If a text display was configured, prefer using a global hologram for the name
        npc.nameDisplayMode = if (textComponent != null) {
            NameDisplayMode.GLOBAL_HOLOGRAM
        } else {
            NameDisplayMode.VANILLA
        }

        return npc
    }

    fun spawn(instance: Instance, pos: Pos): Entity {
        val npc = build()
        npc.spawn()
        npc.entity.setInstance(instance, pos)

        // Attach controllers after instance set
        npc.textDisplayController?.attachTo(npc.entity, instance)
        npc.interactionController?.attachTo(npc.entity, instance)

        // Attach dialogue if provided
        dialogueInit?.let { init ->
            val entityNpc = npc
            val builder = DialogueBuilder(entityNpc)
            builder.init()
            builder.attachOnInteract()
        }

        return npc.entity
    }
}


/** Convenience DSL function: spawn an NPC with builder lambda */
fun spawnNpc(instance: Instance, pos: Pos, init: NpcBuilder.() -> Unit): Entity {
    return NpcBuilder().apply(init).spawn(instance, pos)
}

/** Convenience DSL function: build an NPC without spawning */
fun createNpc(init: NpcBuilder.() -> Unit): EntityNpc {
    return NpcBuilder().apply(init).build()
}




