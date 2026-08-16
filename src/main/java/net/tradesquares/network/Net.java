package net.tradesquares.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.tradesquares.TradeSquares;
import net.tradesquares.client.ClientShopState;
import net.tradesquares.client.screen.ShopScreen;
import net.tradesquares.data.ShopCatalog;
import net.tradesquares.data.model.ShopFile;
import net.tradesquares.economy.CurrencyService;
import net.tradesquares.network.payload.BalancePayload;
import net.tradesquares.network.payload.BuyCartPayload;
import net.tradesquares.network.payload.BuyResultPayload;
import net.tradesquares.network.payload.ShopSyncPayload;
import net.tradesquares.trade.TradeService;

import java.util.Locale;

/** 网络注册与收发（NeoForge 1.21.1 CustomPacketPayload 模式，参照 quest-shop Net，MIT）。 */
@EventBusSubscriber(modid = TradeSquares.MOD_ID)
public final class Net {
    private Net() {}

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar reg = event.registrar(TradeSquares.MOD_ID).versioned("1");

        reg.playToClient(BalancePayload.TYPE, BalancePayload.CODEC, (payload, ctx) ->
                ctx.enqueueWork(() -> {
                    ClientShopState.balance = payload.balance();
                    if (Minecraft.getInstance().screen instanceof ShopScreen s) {
                        s.reload();
                    }
                }));

        reg.playToClient(BuyResultPayload.TYPE, BuyResultPayload.CODEC, (payload, ctx) ->
                ctx.enqueueWork(() -> {
                    var player = Minecraft.getInstance().player;
                    if (player != null) {
                        if (payload.code() == TradeService.ResultCode.OK) {
                            ClientShopState.clearCart();
                            player.sendSystemMessage(Component.translatable("gui.tradesquares.buy.ok"));
                        } else {
                            player.sendSystemMessage(Component.translatable(
                                    "command.tradesquares.buy.fail."
                                            + payload.code().name().toLowerCase(Locale.ROOT)));
                        }
                    }
                    if (Minecraft.getInstance().screen instanceof ShopScreen s) {
                        s.reload();
                    }
                }));

        reg.playToClient(ShopSyncPayload.TYPE, ShopSyncPayload.CODEC, (payload, ctx) ->
                ctx.enqueueWork(() -> {
                    ClientShopState.currentShop = payload.shop();
                    if (Minecraft.getInstance().screen instanceof ShopScreen s) {
                        s.reload();
                    }
                }));

        reg.playToServer(BuyCartPayload.TYPE, BuyCartPayload.CODEC, (payload, ctx) ->
                ctx.enqueueWork(() -> handleBuy(ctx.player(), payload)));
    }

    private static void handleBuy(Player player, BuyCartPayload payload) {
        if (!(player instanceof ServerPlayer sp)) return;
        ShopFile shop = ShopCatalog.INSTANCE.get(payload.shopId());
        if (shop == null) {
            sendResult(sp, new TradeService.TradeResult(
                    TradeService.ResultCode.ENTRY_NOT_FOUND, 0, 0, 0, 0));
            return;
        }
        TradeService.TradeResult result = TradeService.execute(sp, shop, payload.entries());
        sendResult(sp, result);
        if (result.code() == TradeService.ResultCode.OK) {
            sendBalance(sp);
        }
    }

    public static void sendResult(ServerPlayer player, TradeService.TradeResult result) {
        PacketDistributor.sendToPlayer(player, new BuyResultPayload(
                result.code(), result.moneyCharged(), result.moneyGained(),
                result.itemsGained(), result.xpGained()));
    }

    public static void sendBalance(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new BalancePayload(CurrencyService.get(player)));
    }

    public static void sendShop(ServerPlayer player, ShopFile shop) {
        PacketDistributor.sendToPlayer(player, new ShopSyncPayload(shop));
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}
