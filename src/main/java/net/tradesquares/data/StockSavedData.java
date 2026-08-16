package net.tradesquares.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/**
 * 库存持久化（M5）：sold 数量按 "shopId|categoryId|itemId" 记录，服务端重启不丢。
 * 由 TradeService 在结算时读写。
 */
public final class StockSavedData extends SavedData {
    public static final String NAME = "tradesquares_stock";
    private static final String TAG_KEY = "sold";

    private final Map<String, Integer> sold = new HashMap<>();

    public static StockSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new Factory<>(StockSavedData::new, StockSavedData::load), NAME);
    }

    public int getSold(String key) {
        return sold.getOrDefault(key, 0);
    }

    public void addSold(String key, int amount) {
        sold.merge(key, amount, Integer::sum);
        setDirty();
    }

    public static StockSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        StockSavedData data = new StockSavedData();
        CompoundTag soldTag = tag.getCompound(TAG_KEY);
        for (String key : soldTag.getAllKeys()) {
            data.sold.put(key, soldTag.getInt(key));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        CompoundTag soldTag = new CompoundTag();
        for (Map.Entry<String, Integer> e : sold.entrySet()) {
            soldTag.putInt(e.getKey(), e.getValue());
        }
        tag.put(TAG_KEY, soldTag);
        return tag;
    }
}
