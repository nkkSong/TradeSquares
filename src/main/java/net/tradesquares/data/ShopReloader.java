package net.tradesquares.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.neoforged.fml.loading.FMLPaths;
import net.tradesquares.data.model.ShopFile;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 从 config/tradesquares/shops/*.json 加载商店（服务端权威数据源）。
 * 模式借鉴 quest-shop 的 ShopReloader（MIT 可参考实现），但目录改为 config 而非 datapack，
 * 由 /shop reload 命令与服务器启动时触发。
 */
public final class ShopReloader {
    public static final Path SHOPS_DIR = FMLPaths.CONFIGDIR.get()
            .resolve("tradesquares").resolve("shops");

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 商店 id -> 源文件路径（additem 等写文件操作定位用）。 */
    private static final Map<String, java.nio.file.Path> FILE_BY_ID = new HashMap<>();

    private ShopReloader() {}

    public static java.nio.file.Path fileFor(String shopId) {
        return FILE_BY_ID.get(shopId);
    }

    public static ShopLoadResult reload() {
        List<ShopFile> shops = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        try {
            Files.createDirectories(SHOPS_DIR);
            try (Stream<Path> stream = Files.list(SHOPS_DIR)) {
                List<Path> files = stream
                        .filter(p -> p.getFileName().toString().endsWith(".json"))
                        .sorted()
                        .toList();
                for (Path file : files) {
                    try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                        JsonElement json = JsonParser.parseReader(reader);
                        JsonElement migrated = ShopMigrator.migrate(json);
                        ShopFile shop = ShopFile.CODEC.parse(JsonOps.INSTANCE, migrated)
                                .getOrThrow(err -> new IllegalArgumentException("解析失败: " + err));
                        shops.add(shop);
                        FILE_BY_ID.put(shop.id(), file);
                    } catch (Exception ex) {
                        errors.add(file.getFileName() + ": " + ex.getMessage());
                        LOGGER.error("TradeSquares 商店文件加载失败: {}", file, ex);
                    }
                }
            }
        } catch (IOException ex) {
            errors.add("目录读取失败: " + ex.getMessage());
            LOGGER.error("TradeSquares 商店目录读取失败", ex);
        }
        ShopCatalog.INSTANCE.replace(shops, errors);
        LOGGER.info("TradeSquares 商店加载完成: {} 个商店, {} 个错误", shops.size(), errors.size());
        return new ShopLoadResult(shops, errors);
    }
}
