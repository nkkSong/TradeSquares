package net.tradesquares.economy;

import com.mojang.serialization.Codec;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.tradesquares.TradeSquares;

import java.util.function.Supplier;

/** 默认虚拟货币：玩家 DataAttachment（copyOnDeath，服务端权威）。 */
public final class MoneyAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, TradeSquares.MOD_ID);

    public static final Supplier<AttachmentType<Long>> MONEY = ATTACHMENT_TYPES.register(
            "money",
            () -> AttachmentType.builder(() -> 0L)
                    .serialize(Codec.LONG)
                    .copyOnDeath()
                    .build()
    );

    private MoneyAttachments() {}
}
