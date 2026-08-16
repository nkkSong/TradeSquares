package net.tradesquares.economy;

import net.minecraft.server.level.ServerPlayer;

/**
 * 货币后端 SPI（借鉴 quest-shop 的 CoinsProvider 模式，MIT 可参考实现）。
 * 默认实现为内置虚拟货币（DataAttachment）；二期可换 Lightman's / Magic Coins / SG Economy 等。
 */
public interface CurrencyProvider {
    /** 货币 id，如 "default"。 */
    String id();

    long get(ServerPlayer player);

    long set(ServerPlayer player, long value);

    default long add(ServerPlayer player, long delta) {
        return set(player, Math.max(0, get(player) + delta));
    }
}
