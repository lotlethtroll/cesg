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

    /** Belt stations only function two blocks above a belt (Create's belt-processing rule). */
    @Override
    protected boolean canSurvive(net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos) {
        return level.getBlockState(pos.below(2))
                .getBlock() instanceof com.simibubi.create.content.kinetics.belt.BeltBlock;
    }
}
