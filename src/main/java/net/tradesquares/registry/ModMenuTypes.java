package net.tradesquares.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tradesquares.TradeSquares;
import net.tradesquares.menu.ShopMenu;

/** 菜单类型注册（vanilla MenuType）。 */
public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, TradeSquares.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<ShopMenu>> SHOP_MENU =
            MENU_TYPES.register("shop", () -> new MenuType<>(ShopMenu::new, FeatureFlags.VANILLA_SET));

    private ModMenuTypes() {}
}
