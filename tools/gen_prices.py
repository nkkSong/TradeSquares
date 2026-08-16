#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
TradeSquares 商店生成器：从物品列表批量生成商店 JSON（适合其他 MOD 的物品）。
用法:
    python tools/gen_prices.py items.txt [默认价格] [商店id] [分类id] [分类名] > out.json
    python tools/gen_prices.py items.txt --out shops.json
输入 items.txt 每行一个物品，格式二选一:
    minecraft:iron_ingot              # 使用默认价格
    minecraft:netherite_ingot,500     # 指定价格
"""
import argparse
import json
import sys


def parse_items(path, default_price):
    items = []
    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            if "," in line:
                item_id, price = line.split(",", 1)
                price = int(price.strip())
            else:
                item_id, price = line, default_price
            item_id = item_id.strip()
            if ":" not in item_id:
                item_id = "minecraft:" + item_id
            path_part = item_id.split(":", 1)[1]
            items.append({"id": path_part, "icon": item_id, "name": path_part,
                          "give": {"items": [{"item": item_id, "count": 1}]},
                          "cost": {"money": price}})
    return items


def build_shop(shop_id, shop_name, category_id, category_name, items):
    return {
        "schema": 1,
        "id": shop_id,
        "name": shop_name,
        "currency": "default",
        "categories": [
            {"id": category_id, "name": category_name,
             "icon": items[0]["icon"] if items else "minecraft:barrier",
             "items": items}
        ]
    }


def main():
    ap = argparse.ArgumentParser(description="TradeSquares 商店 JSON 生成器")
    ap.add_argument("items_file", help="物品列表文件（每行 itemid 或 itemid,价格）")
    ap.add_argument("default_price", nargs="?", type=int, default=10, help="未指定价格时的默认价（默认 10）")
    ap.add_argument("shop_id", nargs="?", default="mod_shop", help="商店 id（默认 mod_shop）")
    ap.add_argument("category_id", nargs="?", default="items", help="分类 id（默认 items）")
    ap.add_argument("category_name", nargs="?", default="物品", help="分类显示名（默认 物品）")
    ap.add_argument("--out", help="输出文件路径（缺省打印到 stdout）")
    args = ap.parse_args()

    items = parse_items(args.items_file, args.default_price)
    if not items:
        print("没有解析到任何物品", file=sys.stderr)
        sys.exit(1)
    shop = build_shop(args.shop_id, args.shop_id, args.category_id, args.category_name, items)
    out = json.dumps(shop, ensure_ascii=False, indent=2)
    if args.out:
        with open(args.out, "w", encoding="utf-8") as f:
            f.write(out + "\n")
        print(f"已生成 {len(items)} 个条目 -> {args.out}", file=sys.stderr)
    else:
        print(out)


if __name__ == "__main__":
    main()
