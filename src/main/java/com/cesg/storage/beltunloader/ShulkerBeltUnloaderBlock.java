package com.cesg.storage.beltunloader;

import com.cesg.init.CESGBlockEntities;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ShulkerBeltUnloaderBlock extends DirectionalKineticBlock implements IBE<ShulkerBeltUnloaderBlockEntity> {
    public ShulkerBeltUnloaderBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<ShulkerBeltUnloaderBlockEntity> getBlockEntityClass() {
        return ShulkerBeltUnloaderBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ShulkerBeltUnloaderBlockEntity> getBlockEntityType() {
        return CESGBlockEntities.SHULKER_BELT_UNLOADER.get();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING).getOpposite();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }
}
