package com.cesg.storage.station;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.logistics.funnel.AbstractFunnelBlock;
import com.simibubi.create.content.logistics.funnel.BeltFunnelBlock;
import com.simibubi.create.content.logistics.funnel.FunnelBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class StationFunnelConnection {
    private StationFunnelConnection() {}

    public static boolean hasFunnelFacingStation(Level level, BlockPos stationPos, Direction side) {
        BlockPos funnelPos = stationPos.relative(side);
        BlockState state = level.getBlockState(funnelPos);
        if (!AbstractFunnelBlock.isFunnel(state))
            return false;

        Direction funnelFacing = AbstractFunnelBlock.getFunnelFacing(state);
        return funnelFacing == side;
    }

    public static boolean hasOutputFunnel(Level level, BlockPos stationPos, Direction side) {
        if (!hasFunnelFacingStation(level, stationPos, side))
            return false;

        BlockState state = level.getBlockState(stationPos.relative(side));
        if (state.getBlock() instanceof BeltFunnelBlock)
            return isBeltFunnelExtracting(level, stationPos.relative(side), state);

        if (state.getBlock() instanceof FunnelBlock)
            return state.getValue(FunnelBlock.EXTRACTING);

        return false;
    }

    private static boolean isBeltFunnelExtracting(Level level, BlockPos funnelPos, BlockState state) {
        BeltFunnelBlock.Shape shape = state.getValue(BeltFunnelBlock.SHAPE);
        if (shape == BeltFunnelBlock.Shape.PUSHING)
            return true;
        if (shape == BeltFunnelBlock.Shape.PULLING)
            return false;

        BeltBlockEntity belt = BeltHelper.getSegmentBE(level, funnelPos.below());
        if (belt == null)
            return false;

        return belt.getMovementFacing() == state.getValue(BeltFunnelBlock.HORIZONTAL_FACING);
    }

    public static List<Direction> findFunnelsFacingStation(Level level, BlockPos stationPos) {
        List<Direction> sides = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            if (hasFunnelFacingStation(level, stationPos, dir))
                sides.add(dir);
        }
        return sides;
    }

    public static List<Direction> findOutputFunnels(Level level, BlockPos stationPos) {
        List<Direction> sides = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            if (hasOutputFunnel(level, stationPos, dir))
                sides.add(dir);
        }
        return sides;
    }
}
