# Geolosys Finder (Scanner Addon)

[![Русская версия](https://img.shields.io/badge/lang-Русский-blue)](README_RU.md)

An addon mod for [Geolosys](https://www.curseforge.com/minecraft/mc-mods/geolosys) (Minecraft Forge 1.16.5).
Adds a geological prospecting tool — chunk ore scanning, proximity HUD radar, ore highlighting, and sound indicators.

---

## Features

- **Geo Scanner** — custom craftable item with durability (200 uses) and cooldown (3 sec)
- Chunk scanning for Geolosys ores via right-click (RMB)
- HUD panel on the right side of the screen with ore information
- Proximity radar: heat bar, distance, depth, **rotating compass arrow**, and ore coordinates
- Ore Selection GUI (Shift+RMB) with block icons and deposit density estimates
- **Ore highlighting** — glowing wireframe outline on the nearest ore block when within 5 blocks
- Proximity sound that speeds up as you get closer (toggleable via mute button in GUI)
- Full localization: English and Russian
- Server and client configuration

---

## Installation

1. Install [Minecraft Forge 1.16.5](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.16.5.html)
2. Install [Geolosys](https://www.curseforge.com/minecraft/mc-mods/geolosys) (version 5.0.0+)
3. Download `geolosys-scanner-addon-1.1.1.jar` from [Releases](../../releases)
4. Place the JAR file into the `mods/` folder

**Multiplayer:** the mod must be installed on both server and client.

---

## Crafting the Geo Scanner

```
    [D]
[G] [C] [G]
    [S]
```

- **D** = Diamond (scanning crystal)
- **G** = Gold Ingot (decorative rings)
- **C** = Compass (scanning mechanism)
- **S** = Stick (handle)

Durability: 200 uses. Each scan consumes 1 point.

---

## Usage

Hold the **Geo Scanner** in your hand.

| Action | Key | Description |
|---|---|---|
| Scan | RMB | Scans surrounding chunks. A list of found ores appears on the HUD |
| Select ore | Shift + RMB | Opens a GUI with ore list, block icons, and density estimates |
| Deactivate | LMB | Turns off the HUD and radar |

After selecting an ore, the HUD switches to radar mode:
- **Heat bar** — fills up as you get closer to the ore (pulses when < 5 blocks away)
- **Compass arrow** — a rotating triangle that points toward the ore relative to your position
- **Coordinates** — exact X Y Z of the nearest ore block
- **Distance & depth** — how many blocks to the ore, dig down or up
- **Highlighting** — at < 5 blocks, a glowing wireframe outline appears on the ore block
- **Sound** — ping speeds up as you approach. Toggle it off in the Ore Selection GUI
- **Cooldown** — 3 seconds between scans (configurable)

The HUD stays on screen even if you put the tool away — mine and build freely.

---

## Configuration

### Server Config (`geolosys_scanner-server.toml`)

| Setting | Default | Description |
|---|---|---|
| `allowedItems` | `["geolosys_scanner:geo_scanner"]` | Items that can activate the scanner |
| `scanRadius` | `1` | Scan radius in chunks (1–5) |
| `minDepth` | `1` | Minimum Y level to scan |
| `maxDepth` | `80` | Maximum Y level to scan |
| `updateIntervalTicks` | `20` | Radar update interval in ticks |
| `cooldownSeconds` | `3` | Cooldown between scans in seconds (1–30) |

### Client Config (`geolosys_scanner-client.toml`)

| Setting | Default | Description |
|---|---|---|
| `soundEnabled` | `true` | Enable radar sound |
| `soundVolume` | `0.5` | Volume (0.1–1.0) |
| `hudEnabled` | `true` | Show HUD |

---

## Building from Source

### Requirements

- **Java Development Kit 8** (JDK 8) — [Adoptium Temurin](https://adoptium.net/temurin/releases/?version=8)
- **Git**

### Steps

```bash
git clone https://github.com/Gil4rt/GeolosysFinder.git
cd GeolosysFinder
```

If your system Java is **not** version 8, specify the JDK 8 path in `gradle.properties`:

```properties
org.gradle.java.home=C:/path/to/jdk8
```

Build:

```bash
./gradlew build
```

The output JAR will be in `build/libs/geolosys-scanner-addon-1.1.1.jar`.

---

## Project Structure

```
src/main/java/com/geolosysscanner/
├── GeolosysScanner.java          # Mod entry point
├── config/
│   ├── ScannerConfig.java        # Server config
│   └── ClientConfig.java         # Client config
├── item/
│   ├── ModItems.java             # Item registration
│   └── GeoScannerItem.java       # Geo Scanner item
├── network/
│   ├── NetworkHandler.java       # Packet registration
│   ├── OreEntry.java             # Ore DTO
│   ├── PacketScanRequest.java    # C→S: scan request
│   ├── PacketSelectTarget.java   # C→S: ore selection
│   ├── PacketDeactivate.java     # C→S: deactivation
│   ├── PacketScanResult.java     # S→C: scan results
│   ├── PacketRadarUpdate.java    # S→C: radar update
│   └── PacketScannerDeactivated.java  # S→C: deactivation ack
├── server/
│   ├── ServerScanHandler.java    # Scan logic
│   ├── ServerEventHandler.java   # Tick & logout
│   ├── PlayerScanState.java      # Player state
│   └── ScanResult.java           # Ore block accumulator
└── client/
    ├── ClientEventHandler.java   # Input, sound
    ├── ClientScanData.java       # Client state
    ├── ScannerHudRenderer.java   # HUD overlay
    ├── OreSelectionScreen.java   # Ore selection GUI
    └── OreHighlightRenderer.java # Ore block highlighting
```

---

## License

MIT
