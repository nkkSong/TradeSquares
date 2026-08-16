package net.tradesquares.data.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/** 一个商店文件（config/tradesquares/shops/<name>.json）。 */
public record ShopFile(int schema, String id, String name, String currency, List<ShopCategory> categories) {
    public static final int CURRENT_SCHEMA = 1;

    public static final Codec<ShopFile> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("schema", CURRENT_SCHEMA).forGetter(ShopFile::schema),
            Codec.STRING.fieldOf("id").forGetter(ShopFile::id),
            Codec.STRING.optionalFieldOf("name", "").forGetter(ShopFile::name),
            Codec.STRING.optionalFieldOf("currency", "default").forGetter(ShopFile::currency),
            Codec.list(ShopCategory.CODEC).optionalFieldOf("categories", List.of()).forGetter(ShopFile::categories)
    ).apply(i, ShopFile::new));
}
