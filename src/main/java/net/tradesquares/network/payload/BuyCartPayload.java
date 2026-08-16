package net.tradesquares.network.payload;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.tradesquares.TradeSquares;
import net.tradesquares.trade.TradeService;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/** C2S：玩家提交购物车（商店 id + 条目清单）。服务端权威重算并结算。 */
public record BuyCartPayload(String shopId, List<TradeService.PurchaseEntry> entries)
        implements CustomPacketPayload {

    public static final Type<BuyCartPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TradeSquares.MOD_ID, "buy_cart"));

    public static final StreamCodec<ByteBuf, BuyCartPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, BuyCartPayload::shopId,
            ByteBufCodecs.collection(ArrayList::new, TradeService.PurchaseEntry.STREAM_CODEC), BuyCartPayload::entries,
            BuyCartPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
