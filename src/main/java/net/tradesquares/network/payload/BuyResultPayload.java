package net.tradesquares.network.payload;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.tradesquares.TradeSquares;
import net.tradesquares.trade.TradeService;
import org.jetbrains.annotations.NotNull;

/** S2C：购买结果（code + 结算明细），客户端据此提示并刷新。 */
public record BuyResultPayload(TradeService.ResultCode code, long moneyCharged, long moneyGained,
                               int itemsGained, int xpGained) implements CustomPacketPayload {

    public static final Type<BuyResultPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TradeSquares.MOD_ID, "buy_result"));

    public static final StreamCodec<ByteBuf, BuyResultPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, p -> p.code().ordinal(),
            ByteBufCodecs.VAR_LONG, BuyResultPayload::moneyCharged,
            ByteBufCodecs.VAR_LONG, BuyResultPayload::moneyGained,
            ByteBufCodecs.VAR_INT, BuyResultPayload::itemsGained,
            ByteBufCodecs.VAR_INT, BuyResultPayload::xpGained,
            (codeOrd, mc, mg, ig, xg) -> new BuyResultPayload(
                    codeOrd >= 0 && codeOrd < TradeService.ResultCode.values().length
                            ? TradeService.ResultCode.values()[codeOrd] : TradeService.ResultCode.ENTRY_NOT_FOUND,
                    mc, mg, ig, xg)
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
