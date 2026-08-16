package net.tradesquares.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.tradesquares.TradeSquares;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * CSV 批量导入（A/C 方案）：
 * 文件 config/tradesquares/shops/import/&lt;名&gt;.csv
 * 表头 + 每行 分类,物品,数量,买价,卖价（卖价可留空=只买不卖；兼容旧 4 列=只有买价）。
 * 物品列：完整 id / path / 英文显示名（忽略大小写+模糊）。
 * 买价&gt;0 → 购买条目（id=物品 path）；卖价&gt;0 → 出售条目（id=path_sell，name 加“（出售）”）。
 */
public final class CsvImporter {

    public record ImportResult(int added, int skipped, List<String> errors) {}

    private CsvImporter() {}

    public static ImportResult importCsv(Path csvFile, Path shopFile) {
        List<String> errors = new ArrayList<>();
        int added = 0;
        int skipped = 0;
        try {
            JsonObject root;
            try (BufferedReader reader = Files.newBufferedReader(shopFile, StandardCharsets.UTF_8)) {
                root = JsonParser.parseReader(reader).getAsJsonObject();
            }
            JsonArray cats = root.has("categories") ? root.getAsJsonArray("categories") : new JsonArray();

            List<String> lines = Files.readAllLines(csvFile, StandardCharsets.UTF_8);
            for (int lineNo = 0; lineNo < lines.size(); lineNo++) {
                String line = lines.get(lineNo).trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] cols = line.split(",", -1);
                if (cols.length < 4 || cols.length > 5) {
                    errors.add("第" + (lineNo + 1) + "行: 列数应为 4 或 5（分类,物品,数量,买价[,卖价]）");
                    skipped++;
                    continue;
                }
                String catId = cols[0].trim();
                String itemInput = cols[1].trim();
                if (catId.contains("分类")) continue; // 表头行（提前判断）
                int count;
                long price;
                try {
                    count = Integer.parseInt(cols[2].trim());
                } catch (Exception e) {
                    errors.add("第" + (lineNo + 1) + "行: 数量不是数字");
                    skipped++;
                    continue;
                }
                try {
                    price = Long.parseLong(cols[3].trim());
                } catch (Exception e) {
                    errors.add("第" + (lineNo + 1) + "行: 买价不是数字");
                    skipped++;
                    continue;
                }
                if (count <= 0 || price < 0) {
                    errors.add("第" + (lineNo + 1) + "行: 数量/买价非法");
                    skipped++;
                    continue;
                }
                // 卖价（可选）
                long sellPrice = -1;
                if (cols.length > 4 && !cols[4].trim().isEmpty()) {
                    try {
                        sellPrice = Long.parseLong(cols[4].trim());
                    } catch (Exception e) {
                        errors.add("第" + (lineNo + 1) + "行: 卖价不是数字");
                        skipped++;
                        continue;
                    }
                    if (sellPrice < 0) {
                        errors.add("第" + (lineNo + 1) + "行: 卖价非法");
                        skipped++;
                        continue;
                    }
                }

                Item item = resolveItem(itemInput);
                if (item == null || item == Items.AIR) {
                    errors.add("第" + (lineNo + 1) + "行: 找不到物品 " + itemInput);
                    skipped++;
                    continue;
                }
                String itemId = BuiltInRegistries.ITEM.getKey(item).toString();
                String entryId = BuiltInRegistries.ITEM.getKey(item).getPath();
                String displayName = new ItemStack(item).getHoverName().getString();

                // 购买条目（买价 > 0）
                if (price > 0) {
                    JsonObject give = new JsonObject();
                    JsonArray giArr = new JsonArray();
                    JsonObject gi = new JsonObject();
                    gi.addProperty("item", itemId);
                    gi.addProperty("count", count);
                    giArr.add(gi);
                    give.add("items", giArr);
                    JsonObject cost = new JsonObject();
                    cost.addProperty("money", price);
                    if (addEntry(cats, catId, itemId, entryId, displayName, give, cost, errors, lineNo)) {
                        added++;
                    } else {
                        skipped++;
                    }
                }
                // 出售条目（卖价 > 0）
                if (sellPrice > 0) {
                    JsonObject give = new JsonObject();
                    give.addProperty("money", sellPrice);
                    JsonObject cost = new JsonObject();
                    JsonArray ciArr = new JsonArray();
                    JsonObject ci = new JsonObject();
                    ci.addProperty("item", itemId);
                    ci.addProperty("count", count);
                    ciArr.add(ci);
                    cost.add("items", ciArr);
                    if (addEntry(cats, catId, itemId, entryId + "_sell", displayName + "（出售）", give, cost, errors, lineNo)) {
                        added++;
                    } else {
                        skipped++;
                    }
                }
            }

            if (!root.has("categories")) {
                root.add("categories", cats);
            }
            Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
            Files.writeString(shopFile, gson.toJson(root), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            errors.add("导入失败: " + ex.getMessage());
            TradeSquares.LOGGER.error("CSV 导入失败", ex);
        }
        return new ImportResult(added, skipped, errors);
    }

    /** 在分类下追加一个条目（分类自动创建；同分类重名返回 false 并记录错误）。 */
    private static boolean addEntry(JsonArray cats, String catId, String iconId, String entryId,
                                    String name, JsonObject give, JsonObject cost,
                                    List<String> errors, int lineNo) {
        JsonObject catObj = null;
        for (JsonElement ce : cats) {
            JsonObject c = ce.getAsJsonObject();
            if (catId.equals(c.has("id") ? c.get("id").getAsString() : "")) {
                catObj = c;
                break;
            }
        }
        if (catObj == null) {
            catObj = new JsonObject();
            catObj.addProperty("id", catId);
            catObj.addProperty("name", catId);
            catObj.addProperty("icon", iconId);
            catObj.add("items", new JsonArray());
            cats.add(catObj);
        }
        boolean hadItems = catObj.has("items");
        JsonArray items = hadItems ? catObj.getAsJsonArray("items") : new JsonArray();
        for (JsonElement ie : items) {
            JsonObject io = ie.getAsJsonObject();
            if (entryId.equals(io.has("id") ? io.get("id").getAsString() : "")) {
                errors.add("第" + (lineNo + 1) + "行: 条目已存在 " + entryId + "（分类 " + catId + "）");
                return false;
            }
        }
        JsonObject entry = new JsonObject();
        entry.addProperty("id", entryId);
        entry.addProperty("icon", iconId);
        entry.addProperty("name", name);
        entry.add("give", give);
        entry.add("cost", cost);
        items.add(entry);
        if (!hadItems) {
            catObj.add("items", items);
        }
        return true;
    }

    private static Item resolveItem(String input) {
        String s = input.trim();
        if (s.isEmpty()) return null;
        if (s.contains(":")) {
            ResourceLocation rl = ResourceLocation.tryParse(s);
            if (rl != null) return BuiltInRegistries.ITEM.get(rl);
        } else {
            Item byPath = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("minecraft", s));
            if (byPath != Items.AIR) return byPath;
        }
        String lower = s.toLowerCase(Locale.ROOT);
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) continue;
            if (new ItemStack(item).getHoverName().getString().equalsIgnoreCase(s)) return item;
        }
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) continue;
            String name = new ItemStack(item).getHoverName().getString().toLowerCase(Locale.ROOT);
            if (name.contains(lower)) return item;
        }
        return null;
    }
}
