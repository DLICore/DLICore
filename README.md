# DLIAssets — Oraxen-Style Custom Assets Plugin for Paper

[![Paper](https://img.shields.io/badge/Paper-1.20.6%2B-blue)](https://papermc.io)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-purple)](https://kotlinlang.org)
[![Gradle](https://img.shields.io/badge/Gradle-8.5-green)](https://gradle.org)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

> **Create custom items, blocks, mobs, GUIs, recipes, and resource packs — all in YAML config files.**

DLIAssets is a **Paper plugin** that brings Oraxen/ItemsAdder style asset management to modern Minecraft (1.20.6+). Define everything in YAML, and the plugin handles registration, resource pack generation, and distribution automatically.

---

## ✨ Features

| Category | Features |
|----------|----------|
| **Items** | Custom weapons, tools, food, armor, consumables with lore, glint, attributes, mechanics |
| **Blocks** | Custom blocks with models, blockstates, properties (hardness, light, crops, fences, lamps) |
| **Mobs** | Custom entities (bosses, pets) with ModelEngine support, custom AI, attributes, loot tables |
| **GUIs** | Chest GUIs with pagination, dynamic content, actions (give, command, sound, page nav) |
| **Recipes** | Shaped, shapeless, smithing, stonecutting, smelting — all via config |
| **Resource Pack** | **Auto-generated** from configs, hosted via Paper's builtin HTTP server |
| **ModelEngine** | Optional 3D models (.geo.json) for items/blocks/entities |
| **WorldGen** | Custom biomes, dimensions, structures (config-driven) |
| **Commands** | `/dliassets give`, `reload`, `pack`, `list`, `spawn`, `gui`, `debug` |

---

## 🚀 Quick Start

### Requirements
- **Paper 1.20.6+** (1.21 recommended)
- **Java 21**
- **Gradle 8.5+** (for building)

### Installation (Pre-built)
1. Download `DLIAssets-1.0.0.jar` from [Releases](https://github.com/your-repo/releases)
2. Place in `plugins/` folder
3. Start server → configs generate in `plugins/DLIAssets/`
4. Edit `items.yml`, `blocks.yml`, `mobs.yml` to add content
5. Run `/dliassets reload` or restart

### Building from Source
```bash
git clone https://github.com/your-repo/DLIAssets.git
cd DLIAssets
./gradlew shadowJar
# Output: build/libs/DLIAssets-1.0.0-SNAPSHOT.jar
```

---

## ⚙️ Configuration Files

```
plugins/DLIAssets/
├── config.yml           # Main settings (namespace, CMD start, pack settings)
├── items.yml            # Custom items (see below)
├── blocks.yml           # Custom blocks
├── mobs.yml             # Custom mobs/entities
├── recipes.yml          # Crafting recipes
├── guis.yml             # GUI menus
├── pack.yml             # Resource pack generation & distribution
├── model_engine.yml     # ModelEngine integration
├── messages.yml         # All user-facing messages (MiniMessage)
└── textures/            # Put your .png files here!
    ├── items/
    │   ├── ruby.png
    │   └── ruby_sword.png
    ├── blocks/
    │   └── ruby_block.png
    └── entity/
        └── ruby_golem.png
```

---

## 📝 Example: Creating a Custom Item

**`items.yml`**
```yaml
items:
  ruby:
    displayname: "<gradient:#ff0000:#aa0000>Ruby</gradient>"
    material: DIAMOND
    model:
      textures:
        - "items/ruby.png"  # Place in textures/items/ruby.png
    lore:
      - "<gray>A precious red gemstone."
      - "<dark_gray>Used for crafting magical tools."
    glint: true
    max-stack-size: 64
```

**`recipes.yml`**
```yaml
recipes:
  ruby_block:
    type: "shaped"
    pattern:
      - "RRR"
      - "RRR"
      - "RRR"
    key:
      R: "dliassets:ruby"
    result:
      item: "dliassets:ruby_block"
      count: 1
```

**In-game:**
```
/dliassets give Steve dliassets:ruby 64
/dliassets give Steve dliassets:ruby_block
```

---

## 🧱 Example: Custom Block

**`blocks.yml`**
```yaml
blocks:
  ruby_lamp:
    displayname: "<red>Ruby Lamp</red>"
    material: REDSTONE_LAMP
    model:
      custom-model-data: 1001
      parent: "block/cube_all"
      textures:
        on: "blocks/ruby_lamp_on.png"
        off: "blocks/ruby_lamp_off.png"
    properties:
      hardness: 0.3
      light-emission: 15
      sound-group: "glass"
    states:
      lit:
        type: "boolean"
        values:
          "true":
            light-emission: 15
            model:
              textures:
                all: "blocks/ruby_lamp_on.png"
          "false":
            light-emission: 0
            model:
              textures:
                all: "blocks/ruby_lamp_off.png"
```

---

## 👾 Example: Custom Mob (Boss)

**`mobs.yml`**
```yaml
mobs:
  ruby_golem:
    displayname: "<bold><gradient:#ff0000:#aa0000>Ruby Golem</gradient></bold>"
    base-entity: IRON_GOLEM
    model:
      custom-model-data: 2001
      texture: "entity/dliassets/ruby_golem.png"
      scale: 1.2
    attributes:
      max-health: 300.0
      movement-speed: 0.25
      attack-damage: 18.0
      armor: 10.0
    ai:
      remove: ["minecraft:look_at_target"]
      add:
        - type: "melee_attack"
          priority: 1
        - type: "move_towards_target"
          priority: 2
    loot-table:
      pools:
        - rolls: 1
          entries:
            - type: "item"
              name: "dliassets:ruby"
              weight: 10
              functions:
                - type: "minecraft:set_count"
                  count: { min: 5, max: 15 }
    spawn:
      enabled: false  # Enable via worldgen or /dliassets spawn
```

**With ModelEngine (3D models):**
```yaml
model-engine:
  enabled: true
  use-for-entities: true
entity-model-map:
  ruby_golem: "modelengine:ruby_golem"  # Your .geo.json model ID
```

---

## 🎨 Resource Pack System

DLIAssets **generates the entire resource pack automatically** from your configs.

### How it works:
1. On `/dliassets reload` or startup, plugin reads `items.yml`, `blocks.yml`, `mobs.yml`
2. Creates `models/item/`, `models/block/`, `blockstates/`, `lang/` JSON files
3. Copies textures from `textures/` folder
4. Zips everything → `pack.zip`
5. Hosts via **Paper's builtin HTTP server** at `/dliassets/pack.zip`
6. Players auto-prompted to download on join

### `pack.yml` Key Settings:
```yaml
resource-pack:
  auto-generate: true
  host-on-server: true      # Uses Paper's builtin server (no external hosting needed!)
  host-url: ""              # Optional: external CDN URL
  pack-format: 15           # 15 for 1.21, 12 for 1.20.5-1.20.6
  description: "DLIAssets Custom Content Pack"
```

### SHA-256 Hash
The pack hash is calculated and sent to clients for integrity verification.

---

## 🖥️ Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/dliassets reload` | `dliassets.admin.reload` | Reload all configs + regenerate pack |
| `/dliassets give <player> <id> [amount]` | `dliassets.give` | Give custom item/block |
| `/dliassets giveall [player]` | `dliassets.admin.give` | Give all custom items |
| `/dliassets pack regenerate` | `dliassets.admin.pack` | Force pack regeneration |
| `/dliassets pack hash` | `dliassets.admin.pack` | Show pack SHA-256 |
| `/dliassets list [items/blocks/mobs/recipes]` | `dliassets.admin` | List registered assets |
| `/dliassets info <id>` | `dliassets.admin` | Show asset details |
| `/dliassets spawn <mob_id> [player]` | `dliassets.admin.spawn` | Spawn custom mob |
| `/dliassets gui <gui_id>` | `dliassets.admin` | Open GUI |
| `/dliassets debug [config/pack/registry]` | `dliassets.admin.debug` | Debug info |

**Aliases:** `/dli`, `/assets`

---

## 🎮 GUI System

Create menus in `guis.yml`:

```yaml
guis:
  main_menu:
    title: "<gradient:#ff0000:#ff8800>DLI Assets</gradient>"
    size: 54
    filler:
      material: GRAY_STAINED_GLASS_PANE
    items:
      10:
        material: DIAMOND_SWORD
        name: "<red>Custom Items</red>"
        action:
          type: "open_gui"
          gui: "items_category"
      28:
        material: WRITTEN_BOOK
        name: "<aqua>Guide Book</aqua>"
        action:
          type: "give_item"
          item: "dliassets:guide_book"
          close: true
```

**Dynamic GUI (auto-populates from items.yml):**
```yaml
items_category:
  title: "<red>Items</red>"
  size: 54
  dynamic:
    enabled: true
    source: "items"
    template:
      material: "{material}"
      name: "{displayname}"
      lore:
        - "<gray>ID: <white>{id}</white>"
    actions:
      left_click:
        type: "give_item"
        item: "{id}"
        amount: 1
```

---

## 🔧 Mechanics System

Add interactive behaviors to items in `items.yml`:

```yaml
mechanics:
  on_right_click:
    - type: "particle"
      particle: "redstone"
      color: "#ff0000"
      count: 20
    - type: "sound"
      sound: "entity.player.attack.strong"
      pitch: 1.2
    - type: "message"
      message: "<gold>You feel the ruby's power!"
      actionbar: true
    - type: "command"
      command: "effect give {player} minecraft:strength 5 1"
      console: true
  on_hit:
    - type: "particle"
      particle: "redstone"
      color: "#ff0000"
      count: 10
```

**Supported Types:** `particle`, `sound`, `message`, `command`, `give_item`, `damage_item`, `consume`, `cooldown`

---

## 🧩 ModelEngine Integration

For **3D models** (Geo/Anim files):

1. Install **ModelEngine** plugin
2. Create models with **Blockbench** (Geo format)
3. Enable in `model_engine.yml`:
   ```yaml
   model-engine:
     enabled: true
     use-for-entities: true
   entity-model-map:
     ruby_golem: "modelengine:ruby_golem"
   ```
4. DLIAssets registers the model and applies it to your custom mob

---

## 🌍 World Generation (WIP)

```yaml
world-gen:
  enabled: true
  dimensions:
    ruby_dimension:
      type: "overworld"
      generator: "noise"
      biomes:
        - "dliassets:ruby_fields"
```

---

## 📦 Namespace & IDs

- **Default namespace:** `dliassets`
- **Full ID format:** `namespace:id` (e.g., `dliassets:ruby_sword`)
- **CustomModelData range:** Starts at `1,000,000` (configurable)

---

## 🐛 Troubleshooting

| Issue | Solution |
|-------|----------|
| Pack not downloading | Check `host-on-server: true` and Paper version ≥ 1.20.5 |
| Items invisible | Ensure texture files exist in `textures/items/` |
| CustomModelData conflicts | Increase `custom-model-data-start` in `config.yml` |
| Mobs not spawning | Check `spawn.enabled: true` and biome conditions |
| GUI not opening | Verify `guis.yml` syntax and GUI ID |
| Recipes not working | Run `/dliassets reload` after adding recipes |

---

## 📄 License

MIT License — Feel free to use, modify, and distribute.

---

## 🤝 Contributing

1. Fork the repo
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📞 Support

- **Issues:** [GitHub Issues](https://github.com/your-repo/DLIAssets/issues)
- **Discord:** [Join our server](https://discord.gg/your-server)
- **Wiki:** [Documentation](https://github.com/your-repo/DLIAssets/wiki)

---

> Made with ❤️ for the PaperMC community.  
> Inspired by Oraxen, ItemsAdder, and ModelEngine.