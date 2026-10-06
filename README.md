# Color Names

A server-side Fabric mod for Minecraft 26.2 that lets players choose their name color.

## Usage

- `/name color` opens a clickable menu in chat with all 16 Minecraft colors. Hover a color to
  preview your name in it, and click it to apply.
- `/name color <color>` sets a color directly, e.g. `/name color dark_aqua`.
- `/name color reset` removes your name color.

Any player can use the commands. The color shows in chat, in the tab list and on the name tag
above your head.

## How it works

Each color is a scoreboard team named `namecolor_<color>`. Teams are saved with the world, so
colors stay after a restart. Because a player can only be on one team, picking a color moves the
player off any other team they were on.

Only the server needs the mod. Players can join with an unmodded client.

## Requirements

- Minecraft 26.2
- Fabric Loader 0.18.4 or newer
- Fabric API
- Java 25

## Building

```
./gradlew build
```

The jar is written to `build/libs/`.
