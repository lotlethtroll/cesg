package com.cesg.storage.station;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Filter slot on the station's front, left, or right face (relative to {@link DirectionalKineticBlock#FACING}).
 * Shares one filter stack; interaction hit-tests any of the three faces.
 */
public class MultiFaceFilterSlotPositioning extends ValueBoxTransform.Sided {
    @Override
    protected Vec3 getSouthLocation() {
        return VecHelper.voxelSpace(8, 8, 15.5f);
    }

    @Override
    protected boolean isSideActive(BlockState state, Direction direction) {
        if (!(state.getBlock() instanceof DirectionalKineticBlock))
            return false;
        Direction facing = state.getValue(DirectionalKineticBlock.FACING);
        Direction.Axis sideAxis = sideRotationAxis(facing);
        return direction == facing
                || direction == facing.getCounterClockWise(sideAxis)
                || direction == facing.getClockWise(sideAxis);
    }

    /**
     * Axis to rotate the facing around when deriving the two side faces. The no-arg
     * {@link Direction#getCounterClockWise()} only supports horizontal directions, so vertical
     * facings rotate around X instead to keep the slot positioning crash-free.
     */
    private static Direction.Axis sideRotationAxis(Direction facing) {
        return facing.getAxis().isVertical() ? Direction.Axis.X : Direction.Axis.Y;
    }

    @Override
    public boolean testHit(LevelAccessor level, BlockPos pos, BlockState state, Vec3 localHit) {
        for (Direction side : activeSides(state)) {
            fromSide(side);
            if (super.testHit(level, pos, state, localHit))
                return true;
        }
        return false;
    }

    public Iterable<Direction> activeSides(BlockState state) {
        if (!(state.getBlock() instanceof DirectionalKineticBlock))
            return java.util.List.of();
        Direction facing = state.getValue(DirectionalKineticBlock.FACING);
        Direction.Axis sideAxis = sideRotationAxis(facing);
        return java.util.List.of(facing, facing.getCounterClockWise(sideAxis), facing.getClockWise(sideAxis));
    }
}
