package net.tradesquares.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.tradesquares.registry.ModMenuTypes;

/**
 * 商店窗口菜单（无槽位，纯展示 + 网络交互）。
 * 商店数据由 S2C ShopSyncPayload 推送，客户端缓存在 ClientShopState。
 */
public class ShopMenu extends AbstractContainerMenu {
    public ShopMenu(int windowId, Inventory playerInv) {
        super(ModMenuTypes.SHOP_MENU.get(), windowId);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
