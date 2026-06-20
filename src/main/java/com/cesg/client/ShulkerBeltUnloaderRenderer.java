package com.cesg.client;

import com.cesg.storage.beltunloader.ShulkerBeltUnloaderBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Brass spout that grows seamlessly downward from the block bottom and caps off with a wide brass
 * extractor nozzle, mirroring the loader and Create's Hose Pulley pay-out.
 */
public class ShulkerBeltUnloaderRenderer extends SafeBlockEntityRenderer<ShulkerBeltUnloaderBlockEntity> {
    /** Tube hangs from the block bottom (this cube has no protruding nozzle ring). */
    private static final float TUBE_TOP_Y = 0f;
    /** Height of one {@code SPOUT_MIDDLE} body ring. */
    private static final float SEGMENT_HEIGHT = 2f / 16f;
    /** Height of the brass extractor nozzle head. */
    private static final float CAP_HEIGHT = 3f / 16f;
    /** {@code SPOUT_MIDDLE}'s top face sits 2px below its own matrix origin; cancel it so rings butt together. */
    private static final float MIDDLE_TOP_OFFSET = 2f / 16f;
    /** The brass nozzle model's top face sits at its own matrix origin, so no extra offset is needed. */
    private static final float CAP_TOP_OFFSET = 0f;

    public ShulkerBeltUnloaderRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public boolean shouldRenderOffScreen(ShulkerBeltUnloaderBlockEntity be) {
        return be.isTubeVisible();
    }

    @Override
    protected void renderSafe(ShulkerBeltUnloaderBlockEntity be, float partialTicks, PoseStack ms,
            MultiBufferSource buffer, int light, int overlay) {
        float extension = be.getTubeExtension(partialTicks);
        if (extension <= 0)
            return;

        BlockState state = be.getBlockState();
        VertexConsumer vb = buffer.getBuffer(RenderType.solid());

        float reach = extension * ShulkerBeltUnloaderBlockEntity.MAX_TUBE_REACH;
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
