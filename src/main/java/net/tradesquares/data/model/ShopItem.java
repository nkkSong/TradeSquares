package net.tradesquares.data.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** 一个买卖档：给什么（give）换什么（cost）。以物换物 = cost.items 非空；货币买卖 = cost.money。 */
public record ShopItem(String id, ResourceLocation icon, String name, List<String> desc,
                       GiveSpec give, CostSpec cost, int stock, List<String> flags) {
    public static final Codec<ShopItem> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(ShopItem::id),
            ResourceLocation.CODEC.optionalFieldOf("icon", ResourceLocation.withDefaultNamespace("barrier")).forGetter(ShopItem::icon),
            Codec.STRING.optionalFieldOf("name", "").forGetter(ShopItem::name),
            Codec.STRING.listOf().optionalFieldOf("desc", List.of()).forGetter(ShopItem::desc),
            GiveSpec.CODEC.fieldOf("give").forGetter(ShopItem::give),
            CostSpec.CODEC.fieldOf("cost").forGetter(ShopItem::cost),
            Codec.INT.optionalFieldOf("stock", -1).forGetter(ShopItem::stock),
            Codec.STRING.listOf().optionalFieldOf("flags", List.of()).forGetter(ShopItem::flags)
    ).apply(i, ShopItem::new));

    /** 出售型条目：玩家给物品（cost.items）、商店给钱（give.money）。 */
    public boolean isSell() {
        return !cost().items().isEmpty() && give().money() > 0;
    }
}
