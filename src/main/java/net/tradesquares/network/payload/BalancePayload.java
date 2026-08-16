package net.tradesquares.network.payload;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.tradesquares.TradeSquares;
import org.jetbrains.annotations.NotNull;

/** S2C：玩家当前货币余额（UI 顶栏展示用）。 */
public record BalancePayload(long balance) implements CustomPacketPayload {

    public static final Type<BalancePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TradeSquares.MOD_ID, "balance"));

    public static final StreamCodec<ByteBuf, BalancePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, BalancePayload::balance,
            BalancePayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
