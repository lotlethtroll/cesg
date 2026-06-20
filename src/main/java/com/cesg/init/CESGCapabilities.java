package com.cesg.init;

import com.cesg.CESG;
import com.cesg.storage.beltloader.ShulkerBeltLoaderBlockEntity;
import com.cesg.storage.loader.ShulkerLoaderBlockEntity;
import com.cesg.storage.unloader.ShulkerUnloaderBlockEntity;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class CESGCapabilities {
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(CESGCapabilities::onRegisterCapabilities);
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CESGBlockEntities.SHULKER_LOADER.get(),
                (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CESGBlockEntities.SHULKER_UNLOADER.get(),
                (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CESGBlockEntities.SHULKER_BELT_LOADER.get(),
                (be, side) -> be.getItemHandler(side));
    }
}
