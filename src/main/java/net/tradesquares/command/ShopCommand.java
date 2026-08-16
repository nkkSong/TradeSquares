package net.tradesquares.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.tradesquares.TradeSquares;
import net.tradesquares.data.CsvImporter;
import net.tradesquares.data.ShopCatalog;
import net.tradesquares.data.ShopLoadResult;
import net.tradesquares.data.ShopReloader;
import net.tradesquares.data.model.ShopFile;
import net.tradesquares.economy.CurrencyService;
import net.tradesquares.trade.TradeService;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * /shop 命令：reload / list / open（M4 占位）/ money（M2）。
 * M1 全部需 op；M5 再细化权限（普通玩家 get 自己的余额）。
 */
public final class ShopCommand {
    private ShopCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("shop")
                .then(Commands.literal("reload")
                        .requires(src -> src.hasPermission(2))
                        .executes(ShopCommand::reload))
                .then(Commands.literal("list")
                        .requires(src -> src.hasPermission(2))
                        .executes(ShopCommand::list))
                .then(Commands.literal("open")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("shop", StringArgumentType.string())
                                .executes(ShopCommand::open)))
                .then(Commands.literal("buy")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("shop", StringArgumentType.string())
                                .then(Commands.argument("category", StringArgumentType.string())
                                        .then(Commands.argument("item", StringArgumentType.string())
                                                .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                        .executes(ShopCommand::buy))))))
                .then(Commands.literal("import")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("shop", StringArgumentType.string())
                                .then(Commands.argument("file", StringArgumentType.greedyString())
                                        .executes(ShopCommand::importCsv))))
                .then(Commands.literal("additem")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("shop", StringArgumentType.string())
                                .then(Commands.argument("category", StringArgumentType.string())
                                        .then(Commands.argument("price", LongArgumentType.longArg(0))
                                                .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                        .executes(ShopCommand::addItem))))))
                .then(Commands.literal("money")
                        .then(Commands.literal("get")
                                .executes(ctx -> moneyGet(ctx, ctx.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .requires(src -> src.hasPermission(2))
                                        .executes(ctx -> moneyGet(ctx, EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("add")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.argument("players", EntityArgument.players())
                                        .then(Commands.argument("amount", LongArgumentType.longArg(0))
                                                .executes(ctx -> moneyAdd(ctx, EntityArgument.getPlayers(ctx, "players"), LongArgumentType.getLong(ctx, "amount"), true)))))
                        .then(Commands.literal("remove")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.argument("players", EntityArgument.players())
                                        .then(Commands.argument("amount", LongArgumentType.longArg(0))
                                                .executes(ctx -> moneyAdd(ctx, EntityArgument.getPlayers(ctx, "players"), LongArgumentType.getLong(ctx, "amount"), false)))))
                        .then(Commands.literal("set")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.argument("players", EntityArgument.players())
                                        .then(Commands.argument("amount", LongArgumentType.longArg(0))
                                                .executes(ctx -> moneySet(ctx, EntityArgument.getPlayers(ctx, "players"), LongArgumentType.getLong(ctx, "amount"))))))
                        .then(Commands.literal("pay")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.argument("from", EntityArgument.player())
                                        .then(Commands.argument("to", EntityArgument.player())
                                                .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                                        .executes(ctx -> moneyPay(ctx,
                                                                EntityArgument.getPlayer(ctx, "from"),
                                                                EntityArgument.getPlayer(ctx, "to"),
                                                                LongArgumentType.getLong(ctx, "amount"))))))))
        );
    }

    // ---------- shop ----------

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        ShopLoadResult result = ShopReloader.reload();
        CommandSourceStack src = ctx.getSource();
        src.sendSuccess(() -> Component.translatable(
                "command.tradesquares.reload.ok", result.shops().size(), result.errors().size()), true);
        if (!result.errors().isEmpty()) {
            String detail = String.join(" | ", result.errors());
            if (detail.length() > 500) {
                detail = detail.substring(0, 500) + "...";
            }
            src.sendFailure(Component.translatable("command.tradesquares.reload.errors", detail));
        }
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        Map<String, ShopFile> all = ShopCatalog.INSTANCE.all();
        if (all.isEmpty()) {
            ctx.getSource().sendSuccess(
                    () -> Component.translatable("command.tradesquares.list.empty"), false);
            return 0;
        }
        String ids = String.join(", ", all.keySet());
        ctx.getSource().sendSuccess(
                () -> Component.translatable("command.tradesquares.list", ids), false);
        return 1;
    }

    // ---------- open（打开商店 GUI，M4b） ----------

    private static int open(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String shopId = StringArgumentType.getString(ctx, "shop");
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ShopFile shop = ShopCatalog.INSTANCE.get(shopId);
        if (shop == null) {
            StringBuilder avail = new StringBuilder();
            for (String id : ShopCatalog.INSTANCE.all().keySet()) {
                avail.append(id).append(' ');
            }
            ctx.getSource().sendFailure(Component.literal("商店不存在: " + shopId + "（已加载: " + avail + "）"));
            return 0;
        }
        String title = shop.name().isEmpty() ? shopId : shop.name();
        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new net.tradesquares.menu.ShopMenu(id, inv),
                Component.literal(title)));
        net.tradesquares.network.Net.sendShop(player, shop);
        net.tradesquares.network.Net.sendBalance(player);
        return 1;
    }

    // ---------- buy（M3 测试入口，M4 由 GUI 购物车调用） ----------

    private static int buy(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String shopId = StringArgumentType.getString(ctx, "shop");
        String categoryId = StringArgumentType.getString(ctx, "category");
        String itemId = StringArgumentType.getString(ctx, "item");
        int count = IntegerArgumentType.getInteger(ctx, "count");
        ServerPlayer player = ctx.getSource().getPlayerOrException();

        ShopFile shop = ShopCatalog.INSTANCE.get(shopId);
        if (shop == null) {
            ctx.getSource().sendFailure(Component.literal("商店不存在: " + shopId));
            return 0;
        }

        TradeService.TradeResult result = TradeService.execute(player, shop,
                List.of(new TradeService.PurchaseEntry(categoryId, itemId, count)));

        if (result.code() == TradeService.ResultCode.OK) {
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "command.tradesquares.buy.ok", itemId, count,
                    TradeSquares.moneyString(result.moneyCharged()),
                    TradeSquares.moneyString(result.moneyGained()),
                    result.itemsGained(), result.xpGained()), true);
        } else {
            ctx.getSource().sendFailure(Component.translatable(
                    "command.tradesquares.buy.fail." + result.code().name().toLowerCase(Locale.ROOT)));
        }
        return 1;
    }

    // ---------- import（CSV 批量导入） ----------

    private static int importCsv(CommandContext<CommandSourceStack> ctx) {
        String shopId = StringArgumentType.getString(ctx, "shop");
        String fileName = StringArgumentType.getString(ctx, "file");
        CommandSourceStack src = ctx.getSource();
        Path shopFile = ShopReloader.fileFor(shopId);
        if (shopFile == null) {
            src.sendFailure(Component.literal("商店不存在: " + shopId));
            return 0;
        }
        Path csvFile = ShopReloader.SHOPS_DIR.resolve("import").resolve(fileName + ".csv");
        if (!Files.exists(csvFile)) {
            src.sendFailure(Component.literal("文件不存在: config/tradesquares/shops/import/" + fileName + ".csv"));
            return 0;
        }
        CsvImporter.ImportResult result = CsvImporter.importCsv(csvFile, shopFile);
        ShopReloader.reload();
        src.sendSuccess(() -> Component.literal("导入完成：新增 " + result.added() + " 条，跳过 " + result.skipped() + " 条"), true);
        if (!result.errors().isEmpty()) {
            String detail = String.join(" | ", result.errors());
            if (detail.length() > 500) {
                detail = detail.substring(0, 500) + "...";
            }
            src.sendFailure(Component.literal("问题：" + detail));
        }
        return 1;
    }

    // ---------- additem（手持物品入库，轻量可视化录入） ----------

    private static int addItem(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String shopId = StringArgumentType.getString(ctx, "shop");
        String categoryId = StringArgumentType.getString(ctx, "category");
        long price = LongArgumentType.getLong(ctx, "price");
        int giveCount = IntegerArgumentType.getInteger(ctx, "count");
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        CommandSourceStack src = ctx.getSource();

        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            src.sendFailure(Component.literal("手里没有物品"));
            return 0;
        }
        Path file = ShopReloader.fileFor(shopId);
        if (file == null) {
            src.sendFailure(Component.literal("商店不存在: " + shopId));
            return 0;
        }
        String itemId = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
        String entryId = BuiltInRegistries.ITEM.getKey(held.getItem()).getPath();

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray cats = root.getAsJsonArray("categories");
            for (JsonElement ce : cats) {
                JsonObject cat = ce.getAsJsonObject();
                if (!categoryId.equals(cat.has("id") ? cat.get("id").getAsString() : "")) continue;

                boolean hadItems = cat.has("items");
                JsonArray items = hadItems ? cat.getAsJsonArray("items") : new JsonArray();
                for (JsonElement ie : items) {
                    JsonObject io = ie.getAsJsonObject();
                    if (entryId.equals(io.has("id") ? io.get("id").getAsString() : "")) {
                        src.sendFailure(Component.literal("条目已存在: " + entryId + "（同一分类内 id 唯一）"));
                        return 0;
                    }
                }

                JsonObject entry = new JsonObject();
                entry.addProperty("id", entryId);
                entry.addProperty("icon", itemId);
                entry.addProperty("name", held.getHoverName().getString());
                JsonObject give = new JsonObject();
                JsonArray giveItems = new JsonArray();
                JsonObject gi = new JsonObject();
                gi.addProperty("item", itemId);
                gi.addProperty("count", giveCount);
                giveItems.add(gi);
                give.add("items", giveItems);
                entry.add("give", give);
                JsonObject cost = new JsonObject();
                cost.addProperty("money", price);
                entry.add("cost", cost);

                items.add(entry);
                if (!hadItems) {
                    cat.add("items", items);
                }

                Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                Files.writeString(file, gson.toJson(root), StandardCharsets.UTF_8);
                ShopReloader.reload();
                src.sendSuccess(() -> Component.literal("已添加 " + entryId
                        + " 到 " + shopId + "/" + categoryId
                        + "，价格 " + TradeSquares.moneyString(price)), true);
                return 1;
            }
            // 循环结束未找到分类
            StringBuilder avail = new StringBuilder();
            for (JsonElement ce : cats) {
                JsonObject c = ce.getAsJsonObject();
                if (c.has("id")) avail.append(c.get("id").getAsString()).append(' ');
            }
            src.sendFailure(Component.literal("分类不存在: " + categoryId + "（可用: " + avail + "）"));
            return 0;
        } catch (Exception ex) {
            TradeSquares.LOGGER.error("additem 写入失败", ex);
            src.sendFailure(Component.literal("写入失败: " + ex.getMessage()));
            return 0;
        }
    }

    // ---------- money ----------

    private static int moneyGet(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        long money = CurrencyService.get(target);
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "command.tradesquares.money.get", target.getDisplayName(), TradeSquares.moneyString(money)), false);
        return 1;
    }

    private static int moneyAdd(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets,
                                long amount, boolean add) {
        for (ServerPlayer p : targets) {
            long before = CurrencyService.get(p);
            long after = add ? CurrencyService.add(p, amount) : CurrencyService.set(p, before - amount);
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    add ? "command.tradesquares.money.add" : "command.tradesquares.money.remove",
                    p.getDisplayName(), TradeSquares.moneyString(amount), TradeSquares.moneyString(after)), true);
        }
        return targets.size();
    }

    private static int moneySet(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets, long amount) {
        for (ServerPlayer p : targets) {
            long after = CurrencyService.set(p, amount);
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "command.tradesquares.money.set", p.getDisplayName(), TradeSquares.moneyString(after)), true);
        }
        return targets.size();
    }

    private static int moneyPay(CommandContext<CommandSourceStack> ctx, ServerPlayer from, ServerPlayer to, long amount) {
        long fromMoney = CurrencyService.get(from);
        if (fromMoney < amount) {
            ctx.getSource().sendFailure(Component.translatable(
                    "command.tradesquares.money.not_enough", from.getDisplayName()));
            return 0;
        }
        long fromAfter = CurrencyService.add(from, -amount);
        long toAfter = CurrencyService.add(to, amount);
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "command.tradesquares.money.pay", from.getDisplayName(), to.getDisplayName(),
                TradeSquares.moneyString(amount), TradeSquares.moneyString(fromAfter), TradeSquares.moneyString(toAfter)), true);
        return 1;
    }
}