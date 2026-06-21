package com.cesg.storage.station;

import com.cesg.storage.beltloader.ShulkerBeltLoaderBlockEntity;
import com.cesg.storage.beltunloader.ShulkerBeltUnloaderBlockEntity;
import com.cesg.storage.loader.ShulkerLoaderBlockEntity;
import com.cesg.storage.unloader.ShulkerUnloaderBlockEntity;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;

/** Registers item handler capabilities for shulker station block entities. */
public final class StationCapabilities {
    private StationCapabilities() {}

    public static void register(
            RegisterCapabilitiesEvent event,
            BlockEntityType<ShulkerLoaderBlockEntity> loader,
            BlockEntityType<ShulkerUnloaderBlockEntity> unloader,
            BlockEntityType<ShulkerBeltLoaderBlockEntity> beltLoader,
            BlockEntityType<ShulkerBeltUnloaderBlockEntity> beltUnloader) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, loader,
                (ShulkerLoaderBlockEntity be, Direction side) -> resolveHandler(be, side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, unloader,
                (ShulkerUnloaderBlockEntity be, Direction side) -> resolveHandler(be, side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, beltLoader,
                (ShulkerBeltLoaderBlockEntity be, Direction side) -> resolveHandler(be, side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, beltUnloader,
                (ShulkerBeltUnloaderBlockEntity be, Direction side) -> resolveHandler(be, side));
    }

    private static IItemHandler resolveHandler(AbstractShulkerStationBlockEntity be, Direction side) {
        return be.resolveItemHandler(side);
    }
}
