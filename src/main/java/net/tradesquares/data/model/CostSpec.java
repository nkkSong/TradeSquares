package net.tradesquares.data.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/** 玩家支付（结算时扣除）：物品（以物换物）或货币。 */
public record CostSpec(List<ItemSpec> items, int money) {
    public static final Codec<CostSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.list(ItemSpec.CODEC).optionalFieldOf("items", List.of()).forGetter(CostSpec::items),
            Codec.INT.optionalFieldOf("money", 0).forGetter(CostSpec::money)
    ).apply(i, CostSpec::new));
}
