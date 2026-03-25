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

// Build and spawn an NPC
val npc = NpcBuilder().apply {
  type = EntityType.PLAYER
  name = "Guide"
  nameTagVisible = false
  textDisplay(Component.text("Guide"), Vec(0.0, 2.1, 0.0))
  interaction(Vec(0.0, 0.9, 0.0))
}.spawn(instance, pos)

npc.onInteract { player ->
    npc.dialogue(player) {
        line("Hello ${'$'}{player.username}!")
        line("Welcome to the server.")
    }
}
```

Java
```java
import codes.bed.minestom.npc.builder.NpcBuilder;
import net.minestom.server.entity.EntityType;
import net.minestom.server.coordinate.Vec;
import net.kyori.adventure.text.Component;

// Build and spawn an NPC
NpcBuilder builder = new NpcBuilder();
builder.setType(EntityType.PLAYER);
builder.setName("Guide");
builder.setNameTagVisible(false);
builder.textDisplay(Component.text("Guide"), new Vec(0.0, 2.1, 0.0));
builder.interaction(new Vec(0.0, 0.9, 0.0));

var npc = builder.spawn(instance, pos);

npc.onInteract(player -> {
    npc.dialogue(player, dialog -> {
        dialog.line("Hello " + player.getUsername() + "!");
        dialog.line("Welcome to the server.");
    });
});
```

License
-------

Apache-2.0


