package net.tradesquares;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.tradesquares.command.ShopCommand;
import net.tradesquares.config.TradeSquaresConfig;
import net.tradesquares.data.ShopReloader;
import net.tradesquares.economy.MoneyAttachments;
import net.tradesquares.registry.ModMenuTypes;
import org.slf4j.Logger;

/**
 * 交易方市（TradeSquares）：购物车商店模组主入口。
 * M1：JSON 数据层 + /shop 命令骨架；M2：货币 SPI + 默认虚拟货币 + 全局配置。
 */
@Mod(TradeSquares.MOD_ID)
public class TradeSquares {
    public static final String MOD_ID = "tradesquares";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TradeSquares(IEventBus modEventBus, ModContainer modContainer) {
        MoneyAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModMenuTypes.MENU_TYPES.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, TradeSquaresConfig.SPEC);

        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        ShopCommand.register(event.getDispatcher());
    }

    private void onServerStarting(ServerStartingEvent event) {
        ShopReloader.reload();
    }

    /** 货币显示：符号 + 千分位数字。 */
    public static String moneyString(long money) {
        return TradeSquaresConfig.MONEY_SYMBOL.get() + " " + String.format("%,d", money);
    }
}
