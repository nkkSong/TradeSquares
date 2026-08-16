package net.tradesquares.data.model;

import com.mojang.serialization.Codec;

/** 物品匹配规则：EXACT=数据组件全匹配（默认），ANY=只匹配物品 id。 */
public enum MatchRule {
    EXACT, ANY;

    public static final Codec<MatchRule> CODEC = Codec.STRING.xmap(
            s -> "any".equalsIgnoreCase(s) ? ANY : EXACT,
            r -> r == ANY ? "any" : "exact"
    );
}
