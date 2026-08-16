package net.tradesquares.data.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** 商店分类。 */
public record ShopCategory(String id, String name, ResourceLocation icon, List<ShopItem> items) {
    public static final Codec<ShopCategory> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(ShopCategory::id),
            Codec.STRING.optionalFieldOf("name", "").forGetter(ShopCategory::name),
            ResourceLocation.CODEC.optionalFieldOf("icon", ResourceLocation.withDefaultNamespace("barrier")).forGetter(ShopCategory::icon),
            Codec.list(ShopItem.CODEC).optionalFieldOf("items", List.of()).forGetter(ShopCategory::items)
    ).apply(i, ShopCategory::new));
}
