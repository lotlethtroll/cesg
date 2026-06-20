package com.cesg.client;

import com.cesg.storage.beltloader.ShulkerBeltLoaderBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Filter preview plus a brass spout that grows seamlessly downward from the fixed extractor nozzle,
 * mirroring how Create's Hose Pulley pays out a continuous line of body segments capped by a tip.
 */
public class ShulkerBeltLoaderRenderer extends SafeBlockEntityRenderer<ShulkerBeltLoaderBlockEntity> {
    /** Y of the bottom of the static nozzle ring (1px below the block); the tube hangs flush from here. */
    private static final float TUBE_TOP_Y = -1f / 16f;
    /** Height of one {@code SPOUT_MIDDLE} body ring. */
    private static final float SEGMENT_HEIGHT = 2f / 16f;
    /** Height of the brass extractor nozzle head. */
    private static final float CAP_HEIGHT = 3f / 16f;
    /** {@code SPOUT_MIDDLE}'s top face sits 2px below its own matrix origin; cancel it so rings butt together. */
    private static final float MIDDLE_TOP_OFFSET = 2f / 16f;
    /** The brass nozzle model's top face sits at its own matrix origin, so no extra offset is needed. */
    private static final float CAP_TOP_OFFSET = 0f;

    public ShulkerBeltLoaderRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public boolean shouldRenderOffScreen(ShulkerBeltLoaderBlockEntity be) {
        return be.isTubeVisible();
    }

    @Override
    protected void renderSafe(ShulkerBeltLoaderBlockEntity be, float partialTicks, PoseStack ms,
            MultiBufferSource buffer, int light, int overlay) {
        FilteringRenderer.renderOnBlockEntity(be, partialTicks, ms, buffer, light, overlay);
        renderTube(be, partialTicks, ms, buffer, light);
    }

    private static void renderTube(ShulkerBeltLoaderBlockEntity be, float partialTicks, PoseStack ms,
            MultiBufferSource buffer, int light) {
        float extension = be.getTubeExtension(partialTicks);
        if (extension <= 0)
            return;

        BlockState state = be.getBlockState();
        VertexConsumer vb = buffer.getBuffer(RenderType.solid());

        float reach = extension * ShulkerBeltLoaderBlockEntity.MAX_TUBE_REACH;
        int segments = Math.max(0, Math.round((reach - CAP_HEIGHT) / SEGMENT_HEIGHT));

        // penY tracks the absolute Y (block space) where the next part's top face must land.
        float penY = TUBE_TOP_Y;
        for (int i = 0; i < segments; i++) {
            renderPart(AllPartialModels.SPOUT_MIDDLE, state, ms, vb, light, penY + MIDDLE_TOP_OFFSET);
            penY -= SEGMENT_HEIGHT;
        }
        renderPart(CESGPartialModels.EXTRACTOR_NOZZLE, state, ms, vb, light, penY + CAP_TOP_OFFSET);
    }

    private static void renderPart(PartialModel part, BlockState state,
            PoseStack ms, VertexConsumer vb, int light, float y) {
        ms.pushPose();
        ms.translate(0, y, 0);
        CachedBuffers.partial(part, state)
                .light(light)
                .renderInto(ms, vb);
        ms.popPose();
    }
}
