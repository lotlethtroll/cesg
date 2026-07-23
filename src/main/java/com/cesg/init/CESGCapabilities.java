package com.cesg.init;

import com.cesg.gateways.GatewayFuelHandler;
import com.cesg.storage.station.StationCapabilities;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
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

        // Gateway fuel: pump into the Core directly, or into any Frame block (which routes to the Core).
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,
                CESGBlockEntities.CROSS_DIMENSIONAL_GATEWAY_CORE.get(),
                (be, side) -> new GatewayFuelHandler(be));
        // Frames expose the capability via their own BlockEntity (Create pipes only treat BE-backed
        // handlers as pipe endpoints); the handler routes to the connected Core found by ring walk.
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,
                CESGBlockEntities.GATEWAY_FRAME.get(),
                (be, side) -> new GatewayFuelHandler(be));

        // Gateway Port (6A): send/receive buffers for cross-gateway item + fluid logistics.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CESGBlockEntities.GATEWAY_PORT.get(),
                (be, side) -> be.createItemHandler());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CESGBlockEntities.GATEWAY_PORT.get(),
                (be, side) -> be.createFluidHandler());

        // Gateway Flux Battery (7E): pipes/pumps fill its two fuel reservoirs; it tops up the ring Core.
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CESGBlockEntities.GATEWAY_FLUX_BATTERY.get(),
                (be, side) -> be.createFluidHandler());

        // Placed enhanced shulker boxes: funnel/chute/hopper access to their contents. Goes inert
        // while a player is viewing the box (snapshot-clobber guard); station-held boxes unaffected.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CESGBlockEntities.ENHANCED_SHULKER_BOX.get(),
                (be, side) -> new com.cesg.upgrades.EnhancedShulkerBlockItemHandler(be));

        // Ender Barrel: hoppers/pipes at either twin operate on the shared pool.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CESGBlockEntities.ENDER_BARREL.get(),
                (be, side) -> {
                    if (side != null
                            && side != be.getBlockState().getValue(com.cesg.storage.enderbarrel.EnderBarrelBlock.FACING))
                        return null;
                    var pool = be.sharedPool();
                    return pool == null ? null : new net.neoforged.neoforge.items.wrapper.InvWrapper(pool);
                });

        // Ender Infuser: pipe fluids into/out of its tanks; funnel Blaze Powder into the catalyst slot.
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CESGBlockEntities.ENDER_INFUSER.get(),
                (be, side) -> new com.cesg.machine.EnderInfuserFluidHandler(be));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CESGBlockEntities.ENDER_INFUSER.get(),
                (be, side) -> be.getCatalyst());
    }
}
