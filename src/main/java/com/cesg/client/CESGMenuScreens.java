package com.cesg.client;

import com.cesg.init.CESGMenus;
import com.cesg.upgrades.EnhancedShulkerScreen;

import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public class CESGMenuScreens {
    public static void register() {
        // Registered via @EventBusSubscriber below
    }

    @net.neoforged.fml.common.EventBusSubscriber(modid = com.cesg.CESG.MOD_ID, bus = net.neoforged.fml.common.EventBusSubscriber.Bus.MOD, value = net.neoforged.api.distmarker.Dist.CLIENT)
    public static class Registration {
        @net.neoforged.bus.api.SubscribeEvent
        public static void onRegister(RegisterMenuScreensEvent event) {
            event.register(CESGMenus.ENHANCED_SHULKER.get(), EnhancedShulkerScreen::new);
        }
    }
}
