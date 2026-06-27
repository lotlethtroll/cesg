package com.cesg.client;

import com.cesg.storage.station.AbstractShulkerStationBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class ShulkerStationRenderer extends SafeBlockEntityRenderer<AbstractShulkerStationBlockEntity> {
    public ShulkerStationRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    protected void renderSafe(AbstractShulkerStationBlockEntity be, float partialTicks, PoseStack ms,
            MultiBufferSource buffer, int light, int overlay) {
        FilteringRenderer.renderOnBlockEntity(be, partialTicks, ms, buffer, light, overlay);
    }
}
