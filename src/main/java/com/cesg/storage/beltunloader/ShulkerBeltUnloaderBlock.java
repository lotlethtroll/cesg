package com.cesg.storage.beltunloader;

import com.cesg.init.CESGBlockEntities;
import com.cesg.storage.station.AbstractShulkerStationBlock;

public class ShulkerBeltUnloaderBlock extends AbstractShulkerStationBlock<ShulkerBeltUnloaderBlockEntity> {
    public ShulkerBeltUnloaderBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<ShulkerBeltUnloaderBlockEntity> getBlockEntityClass() {
        return ShulkerBeltUnloaderBlockEntity.class;
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntityType<? extends ShulkerBeltUnloaderBlockEntity> getBlockEntityType() {
        return CESGBlockEntities.SHULKER_BELT_UNLOADER.get();
    }
}
