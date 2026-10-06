package gg.archipelago.aprandomizer;

import gg.archipelago.aprandomizer.managers.chest.SharedChestMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class APMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, APRandomizer.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<SharedChestMenu>> SHARED_CHEST =
            MENUS.register("shared_chest", () -> new MenuType<>(SharedChestMenu::new, FeatureFlagSet.of()));
}
