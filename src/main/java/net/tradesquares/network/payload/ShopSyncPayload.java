package net.tradesquares.network.payload;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.tradesquares.TradeSquares;
import net.tradesquares.data.model.ShopFile;
import org.jetbrains.annotations.NotNull;

/** S2C：推送单个商店的完整数据（NBT 序列化，模型无 registry 依赖可纯 NbtOps 编解码）。 */
public record ShopSyncPayload(ShopFile shop) implements CustomPacketPayload {

    public static final Type<ShopSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TradeSquares.MOD_ID, "shop_sync"));

    public static final StreamCodec<ByteBuf, ShopSyncPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG,
            p -> encodeShop(p.shop()),
            tag -> new ShopSyncPayload(decodeShop(tag))
    );

    private static CompoundTag encodeShop(ShopFile shop) {
        return (CompoundTag) ShopFile.CODEC.encodeStart(NbtOps.INSTANCE, shop)
                .getOrThrow(err -> new IllegalStateException("商店序列化失败: " + err));
    }

    private static ShopFile decodeShop(CompoundTag tag) {
        return ShopFile.CODEC.parse(NbtOps.INSTANCE, tag)
                .getOrThrow(err -> new IllegalStateException("商店反序列化失败: " + err));
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
