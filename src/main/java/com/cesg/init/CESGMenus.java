package com.cesg.init;

import com.cesg.CESG;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.cesg.upgrades.EnhancedShulkerMenu;

public class CESGMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, CESG.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<EnhancedShulkerMenu>> ENHANCED_SHULKER =
            MENUS.register("enhanced_shulker", () -> IMenuTypeExtension.create(EnhancedShulkerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<com.cesg.storage.network.StorageTerminalMenu>> STORAGE_TERMINAL =
            MENUS.register("storage_terminal",
                    () -> IMenuTypeExtension.create(com.cesg.storage.network.StorageTerminalMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<com.cesg.gateways.StorageBridgeMenu>> STORAGE_BRIDGE =
            MENUS.register("storage_bridge",
                    () -> IMenuTypeExtension.create(com.cesg.gateways.StorageBridgeMenu::new));

    static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
