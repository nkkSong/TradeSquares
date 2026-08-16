package net.tradesquares.trade;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.tradesquares.TradeSquares;
import net.tradesquares.config.TradeSquaresConfig;
import net.tradesquares.data.StockSavedData;
import net.tradesquares.data.model.ItemSpec;
import net.tradesquares.data.model.MatchRule;
import net.tradesquares.data.model.ShopCategory;
import net.tradesquares.data.model.ShopFile;
import net.tradesquares.data.model.ShopItem;
import net.tradesquares.economy.CurrencyService;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 交易服务端核心（M3）：
 * 购物车清单 → 按商店目录权威重算 cost/gain → 全量校验（物品/货币/库存/上限/出售开关）→ 统一结算。
 * 思路借鉴 ViScriptShop BuyMerchantPayload（权威重算+统一扣发）与 quest-shop（条目重查）。
 * 库存为临时内存版（服务端重启清零），M5 换 SavedData 持久化。
 */
public final class TradeService {
    private static final Logger LOGGER = LogUtils.getLogger();

    public enum ResultCode {
        OK, EMPTY_CART, ENTRY_NOT_FOUND, NOT_ENOUGH_ITEMS, NOT_ENOUGH_MONEY,
        OUT_OF_STOCK, TOO_MANY_ITEMS, SELLING_DISABLED
    }

    public record PurchaseEntry(String categoryId, String itemId, int count) {
        public static final StreamCodec<ByteBuf, PurchaseEntry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, PurchaseEntry::categoryId,
                ByteBufCodecs.STRING_UTF8, PurchaseEntry::itemId,
                ByteBufCodecs.VAR_INT, PurchaseEntry::count,
                PurchaseEntry::new
        );
    }

    public record TradeResult(ResultCode code, long moneyCharged, long moneyGained, int itemsGained, int xpGained) {}

    private TradeService() {}

    public static TradeResult execute(ServerPlayer player, ShopFile shop, List<PurchaseEntry> cart) {
        if (cart == null || cart.isEmpty()) {
            return new TradeResult(ResultCode.EMPTY_CART, 0, 0, 0, 0);
        }

        // ===== 1. 权威重算 cost/gain =====
        Map<ItemStack, Integer> gainItems = new LinkedHashMap<>();
        List<ItemSpec> costItems = new ArrayList<>();
        long costMoney = 0;
        long gainMoney = 0;
        int gainXp = 0;
        List<String> commands = new ArrayList<>();

        for (PurchaseEntry entry : cart) {
            if (entry.count() <= 0) continue;
            ShopCategory cat = findCategory(shop, entry.categoryId());
            if (cat == null) return fail(ResultCode.ENTRY_NOT_FOUND);
            ShopItem item = findItem(cat, entry.itemId());
            if (item == null) return fail(ResultCode.ENTRY_NOT_FOUND);
            int count = entry.count();

            for (ItemSpec g : item.give().items()) addGain(gainItems, g, count);
            gainMoney += (long) item.give().money() * count;
            gainXp += item.give().xp() * count;
            commands.addAll(item.give().commands());

            for (ItemSpec c : item.cost().items()) {
                costItems.add(new ItemSpec(c.item(), c.count() * count, c.match()));
            }
            costMoney += (long) item.cost().money() * count;
        }

        boolean isSell = !costItems.isEmpty() && gainMoney > 0;
        if (isSell && !TradeSquaresConfig.ENABLE_SELLING.get()) {
            return fail(ResultCode.SELLING_DISABLED);
        }

        // ===== 2. 全量校验 =====
        StockSavedData stock = StockSavedData.get(player.serverLevel());
        for (PurchaseEntry entry : cart) {
            if (entry.count() <= 0) continue;
            ShopCategory cat = findCategory(shop, entry.categoryId());
            ShopItem item = cat == null ? null : findItem(cat, entry.itemId());
            if (item == null || item.stock() < 0) continue;
            int sold = stock.getSold(stockKey(shop.id(), entry.categoryId(), entry.itemId()));
            if (sold + entry.count() > item.stock()) {
                return fail(ResultCode.OUT_OF_STOCK);
            }
        }
        for (ItemSpec spec : costItems) {
            if (countItems(player, spec) < spec.count()) {
                return fail(ResultCode.NOT_ENOUGH_ITEMS);
            }
        }
        if (CurrencyService.get(player) < costMoney) {
            return fail(ResultCode.NOT_ENOUGH_MONEY);
        }
        int maxPerPurchase = TradeSquaresConfig.MAX_ITEMS_PER_PURCHASE.get();
        if (maxPerPurchase >= 0) {
            long totalGain = gainItems.values().stream().mapToLong(Integer::longValue).sum();
            if (totalGain > maxPerPurchase) {
                return fail(ResultCode.TOO_MANY_ITEMS);
            }
        }

        // ===== 3. 统一结算（全部校验通过后才扣发） =====
        for (ItemSpec spec : costItems) {
            removeItems(player, spec, spec.count());
        }
        if (costMoney > 0) {
            CurrencyService.add(player, -costMoney);
        }
        if (gainMoney > 0) {
            CurrencyService.add(player, gainMoney);
        }
        for (Map.Entry<ItemStack, Integer> e : gainItems.entrySet()) {
            ItemStack stack = e.getKey().copy();
            stack.setCount(e.getValue());
            ItemHandlerHelper.giveItemToPlayer(player, stack);
        }
        if (gainXp > 0) {
            player.giveExperiencePoints(gainXp);
        }
        for (String cmd : commands) {
            executeCommands(player, cmd);
        }
        for (PurchaseEntry entry : cart) {
            if (entry.count() <= 0) continue;
            ShopCategory cat = findCategory(shop, entry.categoryId());
            ShopItem item = cat == null ? null : findItem(cat, entry.itemId());
            if (item != null && item.stock() >= 0) {
                stock.addSold(stockKey(shop.id(), entry.categoryId(), entry.itemId()), entry.count());
            }
        }

        int totalItems = gainItems.values().stream().mapToInt(Integer::intValue).sum();
        return new TradeResult(ResultCode.OK, costMoney, gainMoney, totalItems, gainXp);
    }

    // ---------- 查询 ----------

    private static ShopCategory findCategory(ShopFile shop, String categoryId) {
        for (ShopCategory cat : shop.categories()) {
            if (cat.id().equals(categoryId)) return cat;
        }
        return null;
    }

    private static ShopItem findItem(ShopCategory cat, String itemId) {
        for (ShopItem item : cat.items()) {
            if (item.id().equals(itemId)) return item;
        }
        return null;
    }

    private static String stockKey(String shopId, String categoryId, String itemId) {
        return shopId + "/" + categoryId + "/" + itemId;
    }

    // ---------- 物品操作 ----------

    private static boolean matches(ItemStack stack, ItemSpec spec) {
        Item item = BuiltInRegistries.ITEM.get(spec.item());
        if (stack.isEmpty() || item == Items.AIR || !stack.is(item)) return false;
        return spec.match() == MatchRule.ANY || stack.getComponents().isEmpty();
    }

    private static int countItems(ServerPlayer player, ItemSpec spec) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (matches(stack, spec)) total += stack.getCount();
        }
        return total;
    }

    private static void removeItems(ServerPlayer player, ItemSpec spec, int need) {
        int left = need;
        for (ItemStack stack : player.getInventory().items) {
            if (left <= 0) break;
            if (matches(stack, spec)) {
                int take = Math.min(stack.getCount(), left);
                stack.shrink(take);
                left -= take;
            }
        }
    }

    private static void addGain(Map<ItemStack, Integer> map, ItemSpec spec, int buyCount) {
        int total = spec.count() * buyCount;
        if (total <= 0) return;
        ItemStack key = new ItemStack(BuiltInRegistries.ITEM.get(spec.item()), 1);
        for (ItemStack k : map.keySet()) {
            if (ItemStack.isSameItemSameComponents(k, key)) {
                map.merge(k, total, Integer::sum);
                return;
            }
        }
        map.put(key, total);
    }

    // ---------- 命令执行 ----------

    private static void executeCommands(ServerPlayer player, String value) {
        for (String command : value.split(";")) {
            command = command.trim();
            if (command.isBlank()) continue;
            if (command.startsWith("/")) {
                command = command.substring(1);
            }
            try {
                MinecraftServer server = player.getServer();
                if (server == null) return;
                CommandSourceStack source = player.createCommandSourceStack()
                        .withPermission(2)
                        .withSuppressedOutput();
                server.getCommands().getDispatcher().execute(
                        server.getCommands().getDispatcher().parse(command, source));
            } catch (Exception ex) {
                LOGGER.error("TradeSquares 购买命令执行失败: {}", command, ex);
            }
        }
    }

    private static TradeResult fail(ResultCode code) {
        return new TradeResult(code, 0, 0, 0, 0);
    }
}
