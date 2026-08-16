package net.tradesquares.economy;

import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/** 货币门面：当前激活的 Provider 由这里统一入口（换后端 = swapBackend）。 */
public final class CurrencyService {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final AtomicReference<CurrencyProvider> ACTIVE =
            new AtomicReference<>(new DefaultCurrencyProvider());

    private CurrencyService() {}

    public static CurrencyProvider provider() {
        return ACTIVE.get();
    }

    public static void swapBackend(CurrencyProvider provider) {
        Objects.requireNonNull(provider, "provider");
        CurrencyProvider prev = ACTIVE.getAndSet(provider);
        LOGGER.info("TradeSquares 货币后端切换: '{}' -> '{}'",
                prev == null ? "<none>" : prev.id(), provider.id());
    }

    public static long get(ServerPlayer player) {
        return ACTIVE.get().get(player);
    }

    public static long set(ServerPlayer player, long value) {
        return ACTIVE.get().set(player, value);
    }

    public static long add(ServerPlayer player, long delta) {
        return ACTIVE.get().add(player, delta);
    }
}
