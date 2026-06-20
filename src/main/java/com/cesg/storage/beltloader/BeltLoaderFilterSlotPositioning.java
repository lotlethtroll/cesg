package com.cesg.storage.beltloader;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;

/**
 * Filter slot on the machine's front face ({@link DirectionalKineticBlock#FACING}).
 * Uses a plain {@link ValueBoxTransform} so Create's {@code FilteringRenderer} always
 * draws the preview on the correct side (not {@link ValueBoxTransform.Sided}, which
 * defaults to UP and skips rendering unless the side is set per-frame).
 */
public class BeltLoaderFilterSlotPositioning extends ValueBoxTransform {
    @Override
    public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(DirectionalKineticBlock.FACING);
        Vec3 onSouthFace = VecHelper.voxelSpace(8, 8, 15.5f);
        return VecHelper.rotateCentered(onSouthFace, AngleHelper.horizontalAngle(facing), Axis.Y);
    }

    @Override
    public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
        Direction facing = state.getValue(DirectionalKineticBlock.FACING);
        float yRot = AngleHelper.horizontalAngle(facing) + 180;
        float xRot = facing == Direction.UP ? 90 : facing == Direction.DOWN ? 270 : 0;
        TransformStack.of(ms)
                .rotateYDegrees(yRot)
                .rotateXDegrees(xRot);
    }
}
