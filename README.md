# stomnpcs

Lightweight NPC utilities for Minestom servers. This library provides configurable name displays (vanilla name tags or holograms) and per-player dialogue holograms, allowing multiple players to interact with the same NPC simultaneously without visual overlap.

## Features
* **Per-Player Holograms:** Dialogue text only appears for the interacting player.
* **Typing Animation:** Smooth, character-by-character text rendering.
* **Branching Dialogue:** Interactive trees with chat-based option selection.
* **Component Support:** Full Adventure API support for colors and formatting.

## Installation

**Gradle (Kotlin DSL)**

```kotlin
implementation("codes.bed.minestom:npc:0.1.2")
```

**Maven**

```xml
<dependency>
    <groupId>codes.bed.minestom</groupId>
    <artifactId>npc</artifactId>
    <version>0.1.2</version>
</dependency>
```

**Setup**

Call the initialization once when your server starts to register the necessary event listeners:
```java
StomNPCs.initialize(eventNode)
```

**Examples**

*Simple Linear Dialogue (Kotlin)*

```kotlin
import codes.bed.minestom.npc.builder.NpcBuilder
import codes.bed.minestom.npc.types.EntityNpc
import codes.bed.minestom.npc.StomNPCs
import net.kyori.adventure.text.Component
import net.minestom.server.entity.EntityType
import net.minestom.server.coordinate.Vec

val npc = NpcBuilder().apply {
    type = EntityType.PLAYER
    name = "Guide"
    textDisplay(Component.text("The Guide"), Vec(0.0, 2.1, 0.0))
    interaction(Vec(0.0, 0.9, 0.0))
}.spawn(instance, pos)

val entityNpc = StomNPCs.manager().byEntityId(npc.uuid) as EntityNpc

entityNpc.dialogue {
    message("Hello! Welcome to the server.")
    message("I have a lot to tell you...")
    delay(40)
    hold(2500)
    offset(Vec(0.0, 2.1, 0.0))
}.attachOnInteract()
```
*Branching Tree Dialogue (Kotlin)*
```kotlin
import net.kyori.adventure.text.format.NamedTextColor

entityNpc.treeDialogue {
    prompt(Component.text("Select an option:", NamedTextColor.GRAY))
    
    root {
        line("Would you like to explore the city?")
        
        option("Yes, please!") {
            line("Great! Follow the path to the North.")
        }
        
        option("Not right now.", onSelect = { player -> 
            player.sendMessage(Component.text("Come back when you are ready.")) 
        })
    }
}.attachOnInteract()
```

*Simple Dialogue (Java)*

```java
import codes.bed.minestom.npc.builder.NpcBuilder;
import codes.bed.minestom.npc.builder.DialogueBuilder;
import codes.bed.minestom.npc.types.EntityNpc;
import codes.bed.minestom.npc.StomNPCs;

NpcBuilder builder = new NpcBuilder();
builder.setType(EntityType.PLAYER);
builder.setName("Guide");
var npc = builder.spawn(instance, pos);

EntityNpc entityNpc = (EntityNpc) StomNPCs.manager().byEntityId(npc.getUuid());

new DialogueBuilder(entityNpc)
    .delay(40L)
    .hold(2500L)
    .message("Hello traveler!")
    .message("Enjoy your stay!")
    .attachOnInteract();
```

cba making a tree example for java, just use kotlin. if you want it do a pr