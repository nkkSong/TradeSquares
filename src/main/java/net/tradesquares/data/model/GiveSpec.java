package net.tradesquares.data.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/** 玩家获得（结算后发出）：物品 + 货币 + 经验 + 命令。 */
public record GiveSpec(List<ItemSpec> items, int money, int xp, List<String> commands) {
    public static final Codec<GiveSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.list(ItemSpec.CODEC).optionalFieldOf("items", List.of()).forGetter(GiveSpec::items),
            Codec.INT.optionalFieldOf("money", 0).forGetter(GiveSpec::money),
            Codec.INT.optionalFieldOf("xp", 0).forGetter(GiveSpec::xp),
            Codec.STRING.listOf().optionalFieldOf("commands", List.of()).forGetter(GiveSpec::commands)
    ).apply(i, GiveSpec::new));
}
