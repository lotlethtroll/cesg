package com.cesg.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.block.state.BlockState;

/** Interior teal cog centered on the back shaft; shared by gateway blocks with a translucent front shell. */
public class GatewayCoreRenderer extends SafeBlockEntityRenderer<KineticBlockEntity> {
    private static final Color TEAL = new Color(0x44, 0xBB, 0xAA);
    private static final float COG_SCALE = 0.5f;
    /** Keep the cog readable through the front glass without pulling it off the shaft plane. */
    private static final float FRONT_NUDGE = 0.05f;
    /** Sit the cog on the shaft end cap (block center) with a hair toward the back face. */
    private static final float BACK_NUDGE = 2f / 16f;

    public GatewayCoreRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    protected void renderSafe(KineticBlockEntity be, float partialTicks, PoseStack ms,
            MultiBufferSource buffer, int light, int overlay) {
        if (!(be.getBlockState().getBlock() instanceof DirectionalKineticBlock))
            return;
        BlockState state = be.getBlockState();
        Axis axis = KineticBlockEntityRenderer.getRotationAxisOf(be);
        Direction axisFacing = Direction.fromAxisAndDirection(axis, AxisDirection.POSITIVE);
        Direction front = state.getValue(DirectionalKineticBlock.FACING);
        Direction back = front.getOpposite();

        ms.pushPose();
        ms.translate(
                0.5f + front.getStepX() * FRONT_NUDGE + back.getStepX() * BACK_NUDGE,
                0.5f + front.getStepY() * FRONT_NUDGE + back.getStepY() * BACK_NUDGE,
                0.5f + front.getStepZ() * FRONT_NUDGE + back.getStepZ() * BACK_NUDGE);
        ms.scale(COG_SCALE, COG_SCALE, COG_SCALE);
        ms.translate(-0.5f, -0.5f, -0.5f);

        VertexConsumer vc = buffer.getBuffer(RenderType.solid());
        SuperByteBuffer cog = CachedBuffers.partialFacingVertical(CESGPartialModels.GATEWAY_CORE_COG, state, axisFacing);
        float angle = KineticBlockEntityRenderer.getAngleForBe(be, be.getBlockPos(), axis);
        KineticBlockEntityRenderer.kineticRotationTransform(cog, be, axis, angle, light)
                .color(TEAL)
                .renderInto(ms, vc);
        ms.popPose();
    }
}
