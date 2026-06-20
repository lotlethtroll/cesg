package com.cesg.storage.beltloader;

import com.cesg.init.CESGBlockEntities;
import com.cesg.storage.station.AbstractShulkerStationBlock;

public class ShulkerBeltLoaderBlock extends AbstractShulkerStationBlock<ShulkerBeltLoaderBlockEntity> {
    public ShulkerBeltLoaderBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<ShulkerBeltLoaderBlockEntity> getBlockEntityClass() {
        return ShulkerBeltLoaderBlockEntity.class;
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntityType<? extends ShulkerBeltLoaderBlockEntity> getBlockEntityType() {
        return CESGBlockEntities.SHULKER_BELT_LOADER.get();
    }
}
