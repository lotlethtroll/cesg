package com.cesg.init;

import com.cesg.storage.station.StationCapabilities;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class CESGCapabilities {
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(CESGCapabilities::onRegisterCapabilities);
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        StationCapabilities.register(event,
                CESGBlockEntities.SHULKER_LOADER.get(),
                CESGBlockEntities.SHULKER_UNLOADER.get(),
                CESGBlockEntities.SHULKER_BELT_LOADER.get(),
                CESGBlockEntities.SHULKER_BELT_UNLOADER.get());
    }
}
