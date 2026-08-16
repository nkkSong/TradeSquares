package net.tradesquares.data;

import net.tradesquares.data.model.ShopFile;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 已加载商店的线程安全只读快照（借鉴 quest-shop 的 ShopCatalog 模式，MIT）。
 * 服务端是权威数据源；客户端同步数据在 M3/M4 引入。
 */
public final class ShopCatalog {
    public static final ShopCatalog INSTANCE = new ShopCatalog();

    private record Snapshot(Map<String, ShopFile> byId, List<String> errors) {}

    private final AtomicReference<Snapshot> ref = new AtomicReference<>(new Snapshot(Map.of(), List.of()));

    private ShopCatalog() {}

    /** 原子替换整个目录。 */
    public void replace(List<ShopFile> shops, List<String> errors) {
        Map<String, ShopFile> byId = new LinkedHashMap<>();
        for (ShopFile shop : shops) {
            byId.put(shop.id(), shop);
        }
        ref.set(new Snapshot(Collections.unmodifiableMap(byId), List.copyOf(errors)));
    }

    public ShopFile get(String id) {
        return ref.get().byId().get(id);
    }

    public Map<String, ShopFile> all() {
        return ref.get().byId();
    }

    public List<String> errors() {
        return ref.get().errors();
    }
}
