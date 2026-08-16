# TradeSquares · 交易方市

[中文](README.md) | [English](README_EN.md)

> 交易方市（TradeSquares）：NeoForge 1.21.1 商店模组，JSON 配置即用、交易防刷、原版界面。
> 全新实现，参考 SDMShop / ViScriptShop / quest-shop 的思路，不依赖任何重型 UI 库。

## 这是什么 / 为什么做

交易方市诞生于作者自己的游玩需求：想要一个轻量、可配置、防刷的 NeoForge 商店方案，于是从零实现了这个「JSON 配置 + 商店交易 + 原版界面」的模组。它配置即用、不依赖重型 UI 库；如果你也在找类似的方案，可以参考。

## 重要声明

- **AI 辅助开发**：代码以 AI 生成为主，经作者逐项审校与游戏内实测后合入；开源仅记录与分享开发过程，请自行评估使用风险。
- **参考致谢**：设计思路参考 SDMShop（ARR）、ViScriptShop（GPL-3.0）、quest-shop（MIT）等商店模组，但**独立实现，未直接搬运或修改原代码，也未使用其任何资产**。
- **Issue 声明**：欢迎提 Issue 反馈问题与建议，但作者业余维护，不保证及时查看与处理。
- **许可**：MIT License，允许参考、二次开发与整合包使用，要求保留署名。

## 功能

**已实现（M1-M4）**

- 每商店一个 JSON 配置文件（`config/tradesquares/shops/`），改配置后 `/shop reload` 立即生效
- 以物换物 + 货币买卖（内置虚拟余额，余额存进存档）
- 购物车 GUI（左键加购/右键移出/底部结算；交易由服务端校验，防刷物品/刷钱）
- 命令：`/shop open/list/reload/buy/money`
- 全局配置 `config/tradesquares-common.toml`（货币符号/初始余额/上限/出售开关/单次上限）
- **可换皮**：界面贴图走资源包路径 `assets/tradesquares/textures/gui/shop.png`，覆盖即换肤

**规划中**：游戏内可视化编辑器、多货币适配（Lightman's/Magic Coins/SG Economy）、KubeJS/JEI/FTB Quests 联动。

## 安装

1. NeoForge 1.21.1 客户端/服务端
2. 把 `tradesquares-0.1.0.jar` 放进 `mods/`
3. 启动后自动生成 `config/tradesquares/`

## 快速开始（3 步）

1. 复制 `example/示例商店.json` 到 `config/tradesquares/shops/`
2. 游戏内执行 `/shop reload`（应提示“已加载 1 个商店，0 个错误”）
3. `/shop money add <你的名字> 100` 发钱，然后 `/shop open main` 购物

## 商店配置（JSON，快速上手）

一个商店 = 一个 JSON 文件，字段全平铺、可省略。核心规则：**一个条目 = 一个买卖档**，`give` 给什么、`cost` 换什么。

```json
{
  "schema": 1,
  "id": "main",
  "name": "主城商店",
  "currency": "default",
  "categories": [
    {
      "id": "food",
      "name": "食物",
      "icon": "minecraft:cooked_beef",
      "items": [
        {
          "id": "beef",
          "name": "熟牛排",
          "give": { "items": [ { "item": "minecraft:cooked_beef", "count": 4 } ] },
          "cost": { "items": [ { "item": "minecraft:gold_ingot", "count": 2 } ] }
        },
        {
          "id": "golden_apple",
          "icon": "minecraft:golden_apple",
          "name": "金苹果",
          "give": { "items": [ { "item": "minecraft:golden_apple", "count": 1 } ] },
          "cost": { "money": 50 }
        }
      ]
    }
  ]
}
```

| 字段 | 说明 | 默认 |
|---|---|---|
| `schema` | 配置版本号（当前 1），升级自动迁移 | 1 |
| `id` | 商店唯一 id（命令用） | 必填 |
| `name` | 显示名 | id |
| `currency` | 货币 id（当前仅 `default`） | default |
| `categories[].id` | 分类 id | 必填 |
| `categories[].name` | 分类显示名 | id |
| `categories[].icon` | 分类图标物品 | minecraft:barrier |
| `items[].id` | 条目 id（分类内唯一） | 必填 |
| `items[].icon` | 条目图标 | 缺省用 give 首个物品 |
| `items[].name` | 条目显示名 | id |
| `items[].desc` | 描述（tooltip 显示） | 空 |
| `items[].give` | 玩家获得：`items`(物品列表)/`money`/`xp`/`commands` | 必填 |
| `items[].cost` | 玩家付出：`items`(以物换物)/`money`(货币) | 必填 |
| `items[].give.items[].item` | 物品 id（如 minecraft:gold_ingot） | 必填 |
| `items[].give.items[].count` | 数量 | 1 |
| `items[].cost.items[].match` | 匹配规则：`any`(仅 id)/`exact`(id+无组件) | any |
| `items[].stock` | 库存上限（-1 无限，当前内存版重启清零） | -1 |
| `items[].flags` | 阶段解锁占位（二期） | 空 |

**要点**：以物换物 = `cost.items` 非空且 `cost.money` 为 0；货币购买 = `cost.money > 0`；出售给商店 = 把物品放 `cost.items`、钱放 `give.money`（同一结构反向表达）。

## 命令

| 命令 | 说明 |
|---|---|
| `/shop open <商店id>` | 打开商店 GUI |
| `/shop reload` | 重新加载商店配置（改 JSON 后执行） |
| `/shop list` | 列出已加载商店 |
| `/shop buy <商店> <分类> <条目> <数量>` | 直接购买（测试/脚本用） |
| `/shop money get [玩家]` | 查余额 |
| `/shop money add <玩家> <金额>` | 加钱 |
| `/shop money remove <玩家> <金额>` | 扣钱 |
| `/shop money set <玩家> <金额>` | 设余额 |
| `/shop money pay <从> <到> <金额>` | 玩家转账 |

## 全局配置（`config/tradesquares-common.toml`）

| 配置 | 默认 | 说明 |
|---|---|---|
| `moneySymbol` | ◎ | 货币显示符号 |
| `moneyName` | 金币 | 货币名称 |
| `initialMoney` | 0 | 新玩家初始货币 |
| `maxMoney` | 9223372036854775807 | 余额上限 |
| `enableSelling` | true | 是否允许玩家出售物品给商店 |
| `maxItemsPerPurchase` | -1 | 单次购买最多获得的物品数（-1 不限） |
| `defaultCurrency` | default | 商店未指定货币时的货币 id |

## 换肤（DIY）

界面背景贴图路径：`assets/tradesquares/textures/gui/shop.png`（256x256 内）。
做一个资源包，覆盖该路径即换肤；没有贴图时自动降级为纯色背景。

## 开发状态

| 里程碑 | 状态 |
|---|---|
| M1 数据层 | ✅ 完成 |
| M2 经济层 | ✅ 完成 |
| M3 交易服务端 | ✅ 完成 |
| M4 GUI+购物车 | ✅ 完成 |
| M5 收尾（命令/文档/内测 jar） | ✅ 完成 |
| M6 发布准备 | 待定 |
| M7 可视化编辑器 | 二期 |
| M8 联动 | 二期 |

详见 `docs/设计草案.md`、`docs/对标表.md`、`docs/参考调研.md`；进度见 `进度.md`。

## 构建

需要 JDK 21 与 NeoForge 1.21.1 开发环境：

```bash
export JAVA_HOME='/c/Program Files/Java/jdk-21.0.10'
./gradlew build
```

产物在 `build/libs/tradesquares-<版本>.jar`。Windows 可用 `build.bat build`（自动设置 JAVA_HOME）。
