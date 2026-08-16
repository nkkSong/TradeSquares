package net.tradesquares.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** 全局配置：config/tradesquares-common.toml，开箱即用默认值。 */
public final class TradeSquaresConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<String> MONEY_SYMBOL = BUILDER
            .comment("货币显示符号")
            .define("moneySymbol", "◎");

    public static final ModConfigSpec.ConfigValue<String> MONEY_NAME = BUILDER
            .comment("货币名称")
            .define("moneyName", "金币");

    public static final ModConfigSpec.IntValue INITIAL_MONEY = BUILDER
            .comment("新玩家初始货币")
            .defineInRange("initialMoney", 0, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.LongValue MAX_MONEY = BUILDER
            .comment("单名玩家货币上限")
            .defineInRange("maxMoney", Long.MAX_VALUE, 0, Long.MAX_VALUE);

    public static final ModConfigSpec.BooleanValue ENABLE_SELLING = BUILDER
            .comment("是否允许玩家出售物品给商店")
            .define("enableSelling", true);

    public static final ModConfigSpec.IntValue MAX_ITEMS_PER_PURCHASE = BUILDER
            .comment("单次购买最多获得的物品数（-1 不限）")
            .defineInRange("maxItemsPerPurchase", -1, -1, Integer.MAX_VALUE);

    public static final ModConfigSpec.ConfigValue<String> DEFAULT_CURRENCY = BUILDER
            .comment("商店未指定货币时使用的货币 id")
            .define("defaultCurrency", "default");

    public static final ModConfigSpec SPEC = BUILDER.build();

    private TradeSquaresConfig() {}
}
