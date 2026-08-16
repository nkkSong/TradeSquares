package net.tradesquares.data.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

/** 一个物品规格：item + count + 匹配规则。 */
public record ItemSpec(ResourceLocation item, int count, MatchRule match) {
    public static final Codec<ItemSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("item").forGetter(ItemSpec::item),
            Codec.INT.optionalFieldOf("count", 1).forGetter(ItemSpec::count),
            MatchRule.CODEC.optionalFieldOf("match", MatchRule.ANY).forGetter(ItemSpec::match)
    ).apply(i, ItemSpec::new));
}
