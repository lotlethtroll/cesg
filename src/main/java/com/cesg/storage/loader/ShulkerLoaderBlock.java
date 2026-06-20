package com.cesg.storage.loader;

import com.cesg.init.CESGBlockEntities;
import com.cesg.storage.station.AbstractShulkerStationBlock;

public class ShulkerLoaderBlock extends AbstractShulkerStationBlock<ShulkerLoaderBlockEntity> {
    public ShulkerLoaderBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<ShulkerLoaderBlockEntity> getBlockEntityClass() {
        return ShulkerLoaderBlockEntity.class;
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntityType<? extends ShulkerLoaderBlockEntity> getBlockEntityType() {
        return CESGBlockEntities.SHULKER_LOADER.get();
    }
}
