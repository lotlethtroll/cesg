package com.cesg.storage.unloader;

import com.cesg.init.CESGBlockEntities;
import com.cesg.storage.station.AbstractShulkerStationBlock;

public class ShulkerUnloaderBlock extends AbstractShulkerStationBlock<ShulkerUnloaderBlockEntity> {
    public ShulkerUnloaderBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<ShulkerUnloaderBlockEntity> getBlockEntityClass() {
        return ShulkerUnloaderBlockEntity.class;
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntityType<? extends ShulkerUnloaderBlockEntity> getBlockEntityType() {
        return CESGBlockEntities.SHULKER_UNLOADER.get();
    }
}
