package net.tradesquares.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.tradesquares.TradeSquares;
import net.tradesquares.client.screen.ShopScreen;
import net.tradesquares.registry.ModMenuTypes;

@EventBusSubscriber(modid = TradeSquares.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ClientInit {
    private ClientInit() {}

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.SHOP_MENU.get(), ShopScreen::new);
    }
}
