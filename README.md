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
val npc = NpcBuilder("Guide")
    .position(instance, Pos(0.0, 65.0, 0.0))
    .nameDisplayMode(NameDisplayMode.PER_PLAYER_HOLOGRAM)
    .spawn()

// On player interact, start a per-player dialogue
npc.onInteract { player ->
    npc.dialogue(player) {
        line("Hello ${'$'}{player.username}!")
        line("Welcome to the server.")
    }
}
```

Java
import codes.bed.minestom.npc.builder.NpcBuilder;
import codes.bed.minestom.npc.StomNPCs;
import codes.bed.minestom.npc.api.NameDisplayMode;
import codes.bed.minestom.npc.types.EntityNpc;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.instance.Instance;

/** Minimal Java example wrapped in a class and method. */
public class NpcExample {
    public void createNpc(Instance instance) {
        NpcBuilder builder = new NpcBuilder("Guide");
        builder.position(instance, new Pos(0.0, 65.0, 0.0));
        builder.nameDisplayMode(NameDisplayMode.PER_PLAYER_HOLOGRAM);

        Entity spawned = builder.spawn();

        EntityNpc npc = (EntityNpc) StomNPCs.manager().byEntityId(spawned.getUuid());

        npc.onInteract(player -> npc.dialogue(player, d -> {
            d.line("Hello " + player.getUsername() + "!");
            d.line("Welcome to the server.");
        }));
    }
}
// Obtain the library helper and cast to the library EntityNpc type
EntityNpc npc = (EntityNpc) StomNPCs.manager().byEntityId(spawned.getUuid());

// On player interact, start a per-player dialogue
npc.onInteract(player -> npc.dialogue(player, d -> {
    d.line("Hello " + player.getUsername() + "!");
    d.line("Welcome to the server.");
}));
```

License
-------

Apache-2.0


