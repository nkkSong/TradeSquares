package net.tradesquares.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import net.tradesquares.TradeSquares;
import net.tradesquares.client.ClientShopState;
import net.tradesquares.data.model.ItemSpec;
import net.tradesquares.data.model.ShopCategory;
import net.tradesquares.data.model.ShopFile;
import net.tradesquares.data.model.ShopItem;
import net.tradesquares.menu.ShopMenu;
import net.tradesquares.network.payload.BuyCartPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * 商店界面（vanilla，可换皮）：
 * - 背景贴图路径 assets/tradesquares/textures/gui/shop.png，资源包覆盖即换皮；缺失时纯色底
 * - 顶栏：商店名 + 余额；左列：分类；右侧：条目卡片网格（分页，滚轮翻页）；底部：购物车栏 + 结算
 */
public class ShopScreen extends AbstractContainerScreen<ShopMenu> {
    public static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(TradeSquares.MOD_ID, "textures/gui/shop.png");

    private static final int IMG_W = 280; // 对齐 SDM Shop（FTB BaseScreen 默认 280 宽）
    private static final int IMG_H = 240; // 底部留购物车区（M4c）

    // 分类列
    private static final int CAT_X = 6;
    private static final int CAT_W = 70;
    private static final int CAT_H = 16;
    private static final int CAT_STEP = 18;
    private static final int CAT_TOP = 24;

    // 条目卡片（4 列 x 4 行/页；卡片之间留间隙；图标居中 + 价格 0.75 缩放居中于正下方）
    private static final int GRID_X = 84;
    private static final int GRID_Y = 24;
    private static final int GRID_COLS = 4;
    private static final int GRID_MAX = 16; // 每页条目数（4x4）
    private static final int GRID_STEP_X = 44; // 列步进（含间隙，左右 6px）
    private static final int GRID_STEP_Y = 48; // 行步进（含间隙，上下 8px）
    private static final int CARD_W = 38;      // 卡片内容宽
    private static final int CARD_H = 40;      // 卡片内容高
    private static final int CARD_PAD_X = 3;
    private static final int CARD_PAD_Y = 2;
    private static final float PRICE_SCALE = 0.75f;

    // 底部购物车栏 + 结算按钮
    private static final int BOTTOM_BAR_Y = 218;
    private static final int BOTTOM_BAR_H = 20;
    private static final int CHECKOUT_X = 236;
    private static final int CHECKOUT_W = 40;
    private static final int CHECKOUT_H = 14;
    private static final int CLEAR_X = 192;
    private static final int CLEAR_W = 40;
    private static final int MODE_BUY_X = 88;
    private static final int MODE_SELL_X = 132;
    private static final int MODE_W = 40;
    private static final int MODE_H = 12;

    private int selectedCategory = 0;
    private int page = 0;
    private boolean sellMode = false;

    public ShopScreen(ShopMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = IMG_W;
        this.imageHeight = IMG_H;
    }

    @Override
    protected void init() {
        super.init();
        selectedCategory = 0;
        page = 0;
    }

    public void reload() {
        selectedCategory = Math.min(selectedCategory, Math.max(0, visibleCategories().size() - 1));
        page = Math.min(page, Math.max(0, pageCount() - 1));
    }

    private int pageCount() {
        ShopCategory cat = currentCategory();
        if (cat == null) return 1;
        int n = visibleItems(cat).size();
        if (n == 0) return 1;
        return (n + GRID_MAX - 1) / GRID_MAX;
    }

    private ShopFile shop() {
        return ClientShopState.currentShop;
    }

    private List<ShopCategory> categories() {
        ShopFile shop = shop();
        return shop == null ? List.of() : shop.categories();
    }

    /** 当前模式可见的分类（只含本模式条目的分类；购买模式=全部有购买条目的分类）。 */
    private List<ShopCategory> visibleCategories() {
        return categories().stream()
                .filter(c -> c.items().stream().anyMatch(i -> i.isSell() == sellMode))
                .toList();
    }

    private List<ShopItem> visibleItems(ShopCategory cat) {
        return cat.items().stream().filter(i -> i.isSell() == sellMode).toList();
    }

    private ShopCategory currentCategory() {
        List<ShopCategory> cats = visibleCategories();
        if (cats.isEmpty()) return null;
        return cats.get(Math.min(selectedCategory, cats.size() - 1));
    }

    // 条目卡片坐标（index 为页内序号）
    private int cardX(int index) {
        return leftPos + GRID_X + (index % GRID_COLS) * GRID_STEP_X + CARD_PAD_X;
    }

    private int cardY(int index) {
        return topPos + GRID_Y + (index / GRID_COLS) * GRID_STEP_Y + CARD_PAD_Y;
    }

    /** 悬停的条目（当前页范围内）。 */
    private ShopItem hoveredItem(double mouseX, double mouseY) {
        ShopCategory cat = currentCategory();
        if (cat == null) return null;
        List<ShopItem> items = visibleItems(cat);
        int start = page * GRID_MAX;
        int end = Math.min(items.size(), start + GRID_MAX);
        for (int i = start; i < end; i++) {
            int slot = i - start;
            int gx = cardX(slot);
            int gy = cardY(slot);
            if (mouseX >= gx && mouseX <= gx + CARD_W && mouseY >= gy && mouseY <= gy + CARD_H) {
                return items.get(i);
            }
        }
        return null;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalDelta, double verticalDelta) {
        if (verticalDelta == 0) return false;
        // 悬停条目 + Shift/Ctrl = 批量加减购物车（Shift=10，Ctrl=64）
        ShopItem hovered = hoveredItem(mouseX, mouseY);
        if (hovered != null && (hasShiftDown() || hasControlDown())) {
            ShopCategory cat = currentCategory();
            if (cat == null) return true;
            int step = hasControlDown() ? 64 : 10;
            int delta = verticalDelta > 0 ? step : -step;
            if (delta > 0) {
                int max = maxCanTake(hovered);
                int target = Math.min(ClientShopState.cartCount(cat.id(), hovered.id()) + delta, max);
                ClientShopState.setCart(cat.id(), hovered.id(), target);
            } else {
                ClientShopState.addCart(cat.id(), hovered.id(), delta);
            }
            return true;
        }
        // 无修饰键 = 翻页
        int pc = pageCount();
        if (pc <= 1) return false;
        if (verticalDelta > 0) {
            page = Math.max(0, page - 1);
        } else {
            page = Math.min(pc - 1, page + 1);
        }
        return true;
    }

    /** 不画默认标签（title/物品栏文字），顶栏已自绘。 */
    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        if (Minecraft.getInstance().getResourceManager().getResource(BACKGROUND).isPresent()) {
            g.blit(BACKGROUND, x, y, 0, 0, IMG_W, IMG_H);
        } else {
            g.fill(x, y, x + IMG_W, y + IMG_H, 0xFF1E1E2E);
        }

        // 顶栏：商店名 + 余额
        ShopFile shop = shop();
        if (shop != null) {
            String name = shop.name().isEmpty() ? shop.id() : shop.name();
            g.drawString(font, name, x + 8, y + 6, 0xFFFFFFFF);
        }
        String balanceText = TradeSquares.moneyString(ClientShopState.balance);
        g.drawString(font, balanceText, x + IMG_W - 8 - font.width(balanceText), y + 6, 0xFFFFFF55);

        // 模式切换按钮（购买/出售）
        g.fill(x + MODE_BUY_X, y + 4, x + MODE_BUY_X + MODE_W, y + 4 + MODE_H, sellMode ? 0xFF2A2A40 : 0xFF404060);
        g.fill(x + MODE_SELL_X, y + 4, x + MODE_SELL_X + MODE_W, y + 4 + MODE_H, sellMode ? 0xFF404060 : 0xFF2A2A40);
        g.drawString(font, "购买", x + MODE_BUY_X + (MODE_W - font.width("购买")) / 2, y + 6, 0xFFFFFFFF);
        g.drawString(font, "出售", x + MODE_SELL_X + (MODE_W - font.width("出售")) / 2, y + 6, 0xFFFFFFFF);

        // 分类列（当前模式可见）
        List<ShopCategory> cats = visibleCategories();
        for (int i = 0; i < cats.size(); i++) {
            int cy = y + CAT_TOP + i * CAT_STEP;
            boolean selected = i == selectedCategory;
            g.fill(x + CAT_X, cy, x + CAT_X + CAT_W, cy + CAT_H, selected ? 0xFF404060 : 0xFF2A2A40);
            String catName = cats.get(i).name().isEmpty() ? cats.get(i).id() : cats.get(i).name();
            String clipped = font.plainSubstrByWidth(catName, CAT_W - 8);
            g.drawString(font, clipped, x + CAT_X + 4, cy + 4, 0xFFFFFFFF);
        }

        // 页码（分类列下方）
        int pc = pageCount();
        if (pc > 1) {
            String pageText = (page + 1) + " / " + pc;
            g.drawString(font, pageText, x + 8, y + CAT_TOP + Math.max(1, cats.size()) * CAT_STEP + 2, 0xFFCCCCCC);
        }

        // 条目卡片网格（分页）
        ShopCategory cat = currentCategory();
        if (cat == null) {
            g.drawString(font, sellMode ? "（此商店没有收购条目）" : "（此商店没有商品）",
                    x + GRID_X, y + GRID_Y + 8, 0xFFCCCCCC);
            return;
        }
        List<ShopItem> items = visibleItems(cat);
        int start = page * GRID_MAX;
        int end = Math.min(items.size(), start + GRID_MAX);
        for (int i = start; i < end; i++) {
            int slot = i - start;
            int gx = cardX(slot);
            int gy = cardY(slot);
            ShopItem item = items.get(i);
            ItemStack stack = itemIcon(item);
            g.fill(gx, gy, gx + CARD_W, gy + CARD_H, 0xFF2A2A40);
            g.renderItem(stack, gx + (CARD_W - 16) / 2, gy + 2);
            String price = font.plainSubstrByWidth(priceText(item), (int) ((CARD_W - 4) / PRICE_SCALE));
            float scaledW = font.width(price) * PRICE_SCALE;
            var pose = g.pose();
            pose.pushPose();
            pose.translate(gx + (CARD_W - scaledW) / 2, gy + 22, 0);
            pose.scale(PRICE_SCALE, PRICE_SCALE, 1.0f);
            g.drawString(font, price, 0, 0, 0xFFDDDDDD);
            pose.popPose();
            int cartN = ClientShopState.cartCount(cat.id(), item.id());
            if (cartN > 0) {
                g.renderItemDecorations(font, stack, gx + (CARD_W - 16) / 2, gy + 2, String.valueOf(cartN));
            }
        }

        // 底部购物车栏 + 结算按钮
        int by = y + BOTTOM_BAR_Y;
        g.fill(x + 4, by, x + IMG_W - 4, by + BOTTOM_BAR_H, 0xFF2A2A40);
        String cartText = ClientShopState.cartEmpty()
                ? "购物车为空"
                : "购物车: " + ClientShopState.cartKinds() + " 种 / " + ClientShopState.cartTotalCount() + " 件";
        g.drawString(font, cartText, x + 8, by + 6, 0xFFFFFFFF);
        boolean checkoutHover = mouseX >= x + CHECKOUT_X && mouseX <= x + CHECKOUT_X + CHECKOUT_W
                && mouseY >= by && mouseY <= by + CHECKOUT_H;
        g.fill(x + CHECKOUT_X, by, x + CHECKOUT_X + CHECKOUT_W, by + CHECKOUT_H,
                checkoutHover ? 0xFF55AA55 : 0xFF3A7A3A);
        g.drawString(font, "结算", x + CHECKOUT_X + (CHECKOUT_W - font.width("结算")) / 2, by + 3, 0xFFFFFFFF);
        g.fill(x + CLEAR_X, by, x + CLEAR_X + CLEAR_W, by + CHECKOUT_H, 0xFF7A3A3A);
        g.drawString(font, "清空", x + CLEAR_X + (CLEAR_W - font.width("清空")) / 2, by + 3, 0xFFFFFFFF);
    }

    private String priceText(ShopItem item) {
        if (item.isSell()) {
            return TradeSquares.moneyString(item.give().money());
        }
        StringBuilder sb = new StringBuilder();
        if (!item.cost().items().isEmpty()) {
            for (ItemSpec s : item.cost().items()) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(s.count()).append('x').append(itemDisplayName(s.item()));
            }
        }
        if (item.cost().money() > 0) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(TradeSquares.moneyString(item.cost().money()));
        }
        return sb.toString();
    }

    private ItemStack itemIcon(ShopItem item) {
        if (!item.icon().equals(ResourceLocation.withDefaultNamespace("barrier"))
                && !item.icon().equals(Items.AIR.builtInRegistryHolder().key().location())) {
            Item iconItem = BuiltInRegistries.ITEM.get(item.icon());
            if (iconItem != Items.AIR) return new ItemStack(iconItem);
        }
        if (!item.give().items().isEmpty()) {
            Item giveItem = BuiltInRegistries.ITEM.get(item.give().items().get(0).item());
            if (giveItem != Items.AIR) return new ItemStack(giveItem);
        }
        // 出售条目（give 无物品）用 cost 的第一个物品作图标
        if (!item.cost().items().isEmpty()) {
            Item costItem = BuiltInRegistries.ITEM.get(item.cost().items().get(0).item());
            if (costItem != Items.AIR) return new ItemStack(costItem);
        }
        return new ItemStack(Items.BARRIER);
    }

    private String itemDisplayName(ResourceLocation id) {
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == Items.AIR) return id.getPath();
        return new ItemStack(item).getHoverName().getString();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 模式切换
        if (mouseY >= topPos + 4 && mouseY <= topPos + 4 + MODE_H) {
            if (mouseX >= leftPos + MODE_BUY_X && mouseX <= leftPos + MODE_BUY_X + MODE_W && sellMode) {
                sellMode = false;
                page = 0;
                return true;
            }
            if (mouseX >= leftPos + MODE_SELL_X && mouseX <= leftPos + MODE_SELL_X + MODE_W && !sellMode) {
                sellMode = true;
                page = 0;
                return true;
            }
        }
        List<ShopCategory> cats = visibleCategories();
        for (int i = 0; i < cats.size(); i++) {
            int cy = topPos + CAT_TOP + i * CAT_STEP;
            if (mouseX >= leftPos + CAT_X && mouseX <= leftPos + CAT_X + CAT_W
                    && mouseY >= cy && mouseY <= cy + CAT_H) {
                if (i != selectedCategory) {
                    ClientShopState.clearCart(); // 跨分类不保留购物车
                }
                selectedCategory = i;
                page = 0;
                return true;
            }
        }
        int by = topPos + BOTTOM_BAR_Y;
        if (mouseX >= leftPos + CHECKOUT_X && mouseX <= leftPos + CHECKOUT_X + CHECKOUT_W
                && mouseY >= by && mouseY <= by + CHECKOUT_H) {
            checkout();
            return true;
        }
        if (mouseX >= leftPos + CLEAR_X && mouseX <= leftPos + CLEAR_X + CLEAR_W
                && mouseY >= by && mouseY <= by + CHECKOUT_H) {
            ClientShopState.clearCart();
            return true;
        }
        ShopCategory cat = currentCategory();
        if (cat != null) {
            List<ShopItem> items = visibleItems(cat);
            int start = page * GRID_MAX;
            int end = Math.min(items.size(), start + GRID_MAX);
            for (int i = start; i < end; i++) {
                int slot = i - start;
                int gx = cardX(slot);
                int gy = cardY(slot);
                if (mouseX >= gx && mouseX <= gx + CARD_W && mouseY >= gy && mouseY <= gy + CARD_H) {
                    ShopItem item = items.get(i);
                    if (button == 0) {
                        if (tabDown()) { // Tab = 设为满（不累加，再点保持满）
                            ClientShopState.setCart(cat.id(), item.id(), maxCanTake(item));
                            return true;
                        }
                        int delta;
                        if (hasControlDown()) delta = 64;
                        else if (hasShiftDown()) delta = 10;
                        else delta = 1;
                        // 所有加购都不超过可买上限
                        int max = maxCanTake(item);
                        int target = Math.min(ClientShopState.cartCount(cat.id(), item.id()) + delta, max);
                        ClientShopState.setCart(cat.id(), item.id(), target);
                        return true;
                    } else if (button == 1) {
                        int delta;
                        if (hasControlDown()) delta = -64;
                        else if (hasShiftDown()) delta = -10;
                        else delta = -1;
                        ClientShopState.addCart(cat.id(), item.id(), delta);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** GLFW 物理按键检测 Tab（鼠标点击瞬间直接查键盘状态，最可靠）。 */
    private boolean tabDown() {
        long handle = Minecraft.getInstance().getWindow().getWindow();
        return GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_TAB) == GLFW.GLFW_PRESS;
    }

    /** 估算一次最多能加多少（Tab+左键）：购买=余额/背包物品上限，出售=背包物品上限。 */
    private int maxCanTake(ShopItem item) {
        if (sellMode) {
            int max = Integer.MAX_VALUE;
            for (ItemSpec spec : item.cost().items()) {
                int have = countItemsClient(spec);
                max = Math.min(max, have / Math.max(1, spec.count()));
            }
            return Math.max(0, max); // 0 = 卖不起不加
        }
        long max = Long.MAX_VALUE;
        if (item.cost().money() > 0) {
            max = Math.min(max, ClientShopState.balance / item.cost().money());
        }
        for (ItemSpec spec : item.cost().items()) {
            int have = countItemsClient(spec);
            max = Math.min(max, have / Math.max(1, spec.count()));
        }
        return (int) Math.max(0, max); // 0 = 加满时什么都加不了（不加，不误导）
    }

    private int countItemsClient(ItemSpec spec) {
        var player = Minecraft.getInstance().player;
        if (player == null) return 0;
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (matchesClient(stack, spec)) total += stack.getCount();
        }
        return total;
    }

    private boolean matchesClient(ItemStack stack, ItemSpec spec) {
        if (stack.isEmpty() || !stack.is(BuiltInRegistries.ITEM.get(spec.item()))) return false;
        return spec.match() == net.tradesquares.data.model.MatchRule.ANY || stack.getComponents().isEmpty();
    }

    /** 提交购物车到服务端权威结算（成功回包后清空购物车）。 */
    private void checkout() {
        ShopFile shop = shop();
        if (shop == null || ClientShopState.cartEmpty()) return;
        PacketDistributor.sendToServer(
                new BuyCartPayload(shop.id(), ClientShopState.buildCartEntries()));
    }

    @Override
    protected void renderTooltip(GuiGraphics g, int mouseX, int mouseY) {
        ShopCategory cat = currentCategory();
        if (cat == null) return;
        List<ShopItem> items = visibleItems(cat);
        int start = page * GRID_MAX;
        int end = Math.min(items.size(), start + GRID_MAX);
        for (int i = start; i < end; i++) {
            int slot = i - start;
            int gx = cardX(slot);
            int gy = cardY(slot);
            if (mouseX >= gx && mouseX <= gx + CARD_W && mouseY >= gy && mouseY <= gy + CARD_H) {
                ShopItem item = items.get(i);
                ItemStack stack = itemIcon(item);
                List<Component> lines = new ArrayList<>();
                lines.add(Component.literal(item.name().isEmpty() ? item.id() : item.name()));
                item.desc().forEach(d -> lines.add(Component.literal(d).copy().withStyle(s -> s.withColor(0xAAAAAA))));
                lines.add(Component.literal("付出: " + priceText(item)).copy().withStyle(s -> s.withColor(0xFFAA00)));
                lines.add(Component.literal("获得: " + gainText(item)).copy().withStyle(s -> s.withColor(0x55FF55)));
                g.renderComponentTooltip(font, lines, mouseX, mouseY);
                return;
            }
        }
        super.renderTooltip(g, mouseX, mouseY);
    }

    private String gainText(ShopItem item) {
        StringBuilder sb = new StringBuilder();
        if (!item.give().items().isEmpty()) {
            for (ItemSpec s : item.give().items()) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(s.count()).append('x').append(itemDisplayName(s.item()));
            }
        }
        if (item.give().money() > 0) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(TradeSquares.moneyString(item.give().money()));
        }
        if (item.give().xp() > 0) {
            sb.append(" 经验+").append(item.give().xp());
        }
        return sb.toString();
    }
}
