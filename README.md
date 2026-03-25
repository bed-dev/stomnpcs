# stomnpcs

Lightweight NPC utilities for Minestom servers. This library provides configurable name displays (vanilla
name tags or holograms) and per-player dialogue holograms that allow multiple players to interact with the same NPC
simultaneously without visual overlap.

Gradle (Kotlin DSL)

```kotlin
implementation("codes.bed.minestom:npc:0.1.0")
```

Maven

```xml

<dependency>
    <groupId>codes.bed.minestom</groupId>
    <artifactId>npc</artifactId>
    <version>0.1.0</version>
</dependency>
```

Minimal examples
----------------


Kotlin

```kotlin
import codes.bed.minestom.npc.builder.NpcBuilder
import codes.bed.minestom.npc.api.NameDisplayMode

val npc = NpcBuilder().apply {
    type = EntityType.PLAYER
    name = "Guide"
    nameTagVisible = false
    textDisplay(Component.text("Guide"), Vec(0.0, 2.1, 0.0))
    interaction(Vec(0.0, 0.9, 0.0))
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

```


Java

```java
NpcBuilder builder = new NpcBuilder();
builder.setType(EntityType.PLAYER);
builder.setName("Guide");
builder.setNameTagVisible(false);
builder.textDisplay(Component.text("Guide"), new Vec(0.0, 2.1, 0.0));
builder.interaction(new Vec(0.0, 0.9, 0.0));

var npc = builder.spawn(instance, pos);

// 2. Create the simple, linear dialogue
DialogueBuilder dialogue = new DialogueBuilder(npc);
    
// 3. Chain your configuration and messages
dialogue.delay(40L)
        .hold(2500L)
        .message("Hello traveler!")
        .message("Welcome to the server.")
        .message("Enjoy your stay!");

// 4. Attach the listener so it triggers when players click the NPC
dialogue.attachOnInteract();```

License
-------

Apache-2.0


