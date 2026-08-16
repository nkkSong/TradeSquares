# TradeSquares · Trading Plaza

[中文](README.md) | [English](README_EN.md)

> A cart-based shop mod for Minecraft NeoForge 1.21.1: JSON config + server-authoritative trading + vanilla UI.
> Built from scratch, inspired by the design ideas of SDMShop / ViScriptShop / quest-shop, with no heavy UI library dependencies.

## What is this / Why

TradeSquares was born from the author's own server needs: existing shop mods update slowly and their interactions don't fit how we actually play, so this shop mod ("JSON config + shopping cart + server-authoritative trading") was implemented from scratch. If you also need a lightweight, configurable, anti-dupe NeoForge shop solution, it might suit you too.

## Important notices

- **AI-assisted development**: the code is mostly AI-generated, reviewed item by item and play-tested in-game by the author before merging; open-sourced only to record and share the development process — use at your own risk.
- **Credits**: design inspired by SDMShop (ARR), ViScriptShop (GPL-3.0), quest-shop (MIT) and other shop mods, but **independently implemented — no code copied or modified from the originals, and none of their assets are used**.
- **Issues**: issues and suggestions are welcome, but the author maintains this in spare time and does not guarantee timely review or response.
- **License**: MIT License — reference, derivative works and modpack use are allowed, with attribution retained.

## Features

**Implemented (M1–M4)**

- One JSON config file per shop (`config/tradesquares/shops/`), hot-reload via `/shop reload`
- Item-for-item trading + currency buy/sell (built-in virtual balance, persisted per player)
- Shopping cart GUI (left-click to add / right-click to remove / checkout at the bottom; server-authoritative validation to prevent duping)
- Commands: `/shop open/list/reload/buy/money`
- Global config `config/tradesquares-common.toml` (currency symbol / initial balance / cap / selling toggle / per-purchase cap)
- **Reskinnable**: the GUI texture is served from the resource pack path `assets/tradesquares/textures/gui/shop.png` — override it to reskin

**Planned**: in-game visual editor, multi-currency support (Lightman's / Magic Coins / SG Economy), KubeJS / JEI / FTB Quests integration.

## Installation

1. NeoForge 1.21.1 client/server
2. Put `tradesquares-0.1.0.jar` into `mods/`
3. `config/tradesquares/` is generated automatically on first launch

## Quick start (3 steps)

1. Copy `example/示例商店.json` to `config/tradesquares/shops/`
2. Run `/shop reload` in-game (it should say "Loaded 1 shop, 0 errors")
3. `/shop money add <your name> 100` to give yourself money, then `/shop open main` to shop

## Shop config (JSON, quick reference)

One shop = one JSON file; all fields are flat and optional. Core rule: **one entry = one trade slot** — `give` is what the player receives, `cost` is what it costs.

```json
{
  "schema": 1,
  "id": "main",
  "name": "Main Shop",
  "currency": "default",
  "categories": [
    {
      "id": "food",
      "name": "Food",
      "icon": "minecraft:cooked_beef",
      "items": [
        {
          "id": "beef",
          "name": "Steak",
          "give": { "items": [ { "item": "minecraft:cooked_beef", "count": 4 } ] },
          "cost": { "items": [ { "item": "minecraft:gold_ingot", "count": 2 } ] }
        },
        {
          "id": "golden_apple",
          "icon": "minecraft:golden_apple",
          "name": "Golden Apple",
          "give": { "items": [ { "item": "minecraft:golden_apple", "count": 1 } ] },
          "cost": { "money": 50 }
        }
      ]
    }
  ]
}
```

| Field | Description | Default |
|---|---|---|
| `schema` | Config schema version (currently 1); auto-migrated on upgrade | 1 |
| `id` | Unique shop id (used by commands) | required |
| `name` | Display name | id |
| `currency` | Currency id (currently only `default`) | default |
| `categories[].id` | Category id | required |
| `categories[].name` | Category display name | id |
| `categories[].icon` | Category icon item | minecraft:barrier |
| `items[].id` | Entry id (unique within a category) | required |
| `items[].icon` | Entry icon | first item of `give` |
| `items[].name` | Entry display name | id |
| `items[].desc` | Description (shown in tooltip) | empty |
| `items[].give` | What the player gets: `items`(item list)/`money`/`xp`/`commands` | required |
| `items[].cost` | What the player pays: `items`(item-for-item)/`money`(currency) | required |
| `items[].give.items[].item` | Item id (e.g. minecraft:gold_ingot) | required |
| `items[].give.items[].count` | Count | 1 |
| `items[].cost.items[].match` | Matching rule: `any`(id only)/`exact`(id + no components) | any |
| `items[].stock` | Stock cap (-1 = unlimited; in-memory for now, resets on restart) | -1 |
| `items[].flags` | Stage unlock placeholder (phase 2) | empty |

**Key points**: item-for-item trading = `cost.items` non-empty and `cost.money` is 0; currency purchase = `cost.money > 0`; selling to the shop = put the item in `cost.items` and the money in `give.money` (the same structure expressed in reverse).

## Commands

| Command | Description |
|---|---|
| `/shop open <shop id>` | Open the shop GUI |
| `/shop reload` | Reload shop configs (run after editing JSON) |
| `/shop list` | List loaded shops |
| `/shop buy <shop> <category> <entry> <amount>` | Buy directly (for testing / scripts) |
| `/shop money get [player]` | Check balance |
| `/shop money add <player> <amount>` | Add money |
| `/shop money remove <player> <amount>` | Remove money |
| `/shop money set <player> <amount>` | Set balance |
| `/shop money pay <from> <to> <amount>` | Transfer between players |

## Global config (`config/tradesquares-common.toml`)

| Setting | Default | Description |
|---|---|---|
| `moneySymbol` | ◎ | Currency display symbol |
| `moneyName` | Coin | Currency name |
| `initialMoney` | 0 | Initial currency for new players |
| `maxMoney` | 9223372036854775807 | Balance cap |
| `enableSelling` | true | Whether players can sell items to shops |
| `maxItemsPerPurchase` | -1 | Max items received per purchase (-1 = unlimited) |
| `defaultCurrency` | default | Currency id used when a shop doesn't specify one |

## Reskinning (DIY)

GUI background texture path: `assets/tradesquares/textures/gui/shop.png` (within 256×256).
Make a resource pack and override that path to reskin; the UI falls back to a solid-color background when no texture is present.

## Development status

| Milestone | Status |
|---|---|
| M1 Data layer | ✅ Done |
| M2 Economy layer | ✅ Done |
| M3 Trading server | ✅ Done |
| M4 GUI + cart | ✅ Done |
| M5 Wrap-up (commands/docs/internal-test jar) | ✅ Done |
| M6 Release prep | Pending |
| M7 Visual editor | Phase 2 |
| M8 Integrations | Phase 2 |

See `docs/设计草案.md`, `docs/对标表.md`, `docs/参考调研.md` for details; progress in `进度.md`.

## Building

Requires JDK 21 and a NeoForge 1.21.1 dev environment:

```bash
export JAVA_HOME='/c/Program Files/Java/jdk-21.0.10'
./gradlew build
```

Artifacts at `build/libs/tradesquares-<version>.jar`. On Windows, `build.bat build` sets JAVA_HOME automatically.
