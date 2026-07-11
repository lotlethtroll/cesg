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

    /** Belt stations only function two blocks above a belt (Create's belt-processing rule). */
    @Override
    protected boolean canSurvive(net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos) {
        return level.getBlockState(pos.below(2))
                .getBlock() instanceof com.simibubi.create.content.kinetics.belt.BeltBlock;
    }
}
