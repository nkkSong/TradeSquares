package net.tradesquares.economy;

import net.minecraft.server.level.ServerPlayer;
import net.tradesquares.config.TradeSquaresConfig;

/** 内置虚拟货币（默认后端）：余额存玩家 DataAttachment。 */
public final class DefaultCurrencyProvider implements CurrencyProvider {
    public static final String ID = "default";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public long get(ServerPlayer player) {
        return player.getData(MoneyAttachments.MONEY);
    }

    @Override
    public long set(ServerPlayer player, long value) {
        long max = TradeSquaresConfig.MAX_MONEY.get();
        long clamped = Math.max(0, Math.min(value, max));
        player.setData(MoneyAttachments.MONEY, clamped);
        return clamped;
    }
}
