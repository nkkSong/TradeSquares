package net.tradesquares.client;

import net.tradesquares.data.model.ShopFile;
import net.tradesquares.trade.TradeService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 客户端当前打开的商店、余额与购物车（由 S2C 推送/本地交互更新；结算权威在服务端）。 */
public final class ClientShopState {
    public static ShopFile currentShop;
    public static long balance;

    /** 购物车：key = categoryId|itemId -> 数量。 */
    private static final Map<String, Integer> CART = new HashMap<>();

    private ClientShopState() {}

    public static void open(ShopFile shop, long bal) {
        currentShop = shop;
        balance = bal;
    }

    public static void addCart(String categoryId, String itemId, int delta) {
        String key = categoryId + "|" + itemId;
        int v = CART.getOrDefault(key, 0) + delta;
        if (v <= 0) {
            CART.remove(key);
        } else {
            CART.put(key, v);
        }
    }

    /** 直接设购物车数量（Tab 加满用：设为上限，不累加）。 */
    public static void setCart(String categoryId, String itemId, int count) {
        String key = categoryId + "|" + itemId;
        if (count <= 0) {
            CART.remove(key);
        } else {
            CART.put(key, count);
        }
    }

    public static int cartCount(String categoryId, String itemId) {
        return CART.getOrDefault(categoryId + "|" + itemId, 0);
    }

    public static boolean cartEmpty() {
        return CART.isEmpty();
    }

    public static int cartKinds() {
        return CART.size();
    }

    public static int cartTotalCount() {
        return CART.values().stream().mapToInt(Integer::intValue).sum();
    }

    public static void clearCart() {
        CART.clear();
    }

    public static List<TradeService.PurchaseEntry> buildCartEntries() {
        List<TradeService.PurchaseEntry> list = new ArrayList<>();
        for (Map.Entry<String, Integer> e : CART.entrySet()) {
            String key = e.getKey();
            int sep = key.indexOf('|');
            list.add(new TradeService.PurchaseEntry(
                    key.substring(0, sep), key.substring(sep + 1), e.getValue()));
        }
        return list;
    }
}
