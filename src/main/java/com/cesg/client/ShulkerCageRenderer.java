package com.cesg.client;

import com.cesg.farming.ShulkerCageBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.ShulkerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;

import org.jetbrains.annotations.Nullable;

/** Renders the trapped shulker as a shrunken figure suspended inside the cage bars. */
public class ShulkerCageRenderer implements BlockEntityRenderer<ShulkerCageBlockEntity> {
    /** Fraction of a full block the captured shulker is scaled to. */
    private static final float DISPLAY_SCALE = 0.5F;

    private final ShulkerModel<?> model;

    public ShulkerCageRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new ShulkerModel<>(context.bakeLayer(ModelLayers.SHULKER));
    }

    @Override
    public void render(ShulkerCageBlockEntity cage, float partialTick, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!cage.hasTrappedShulker())
            return;

        @Nullable DyeColor color = cage.getTrappedColor();
        Material material = color == null
                ? Sheets.DEFAULT_SHULKER_TEXTURE_LOCATION
                : Sheets.SHULKER_TEXTURE_LOCATION.get(color.getId());

        // Closed lid — the captured shulker stays shut inside the cage.
        ModelPart lid = model.getLid();
        lid.setPos(0.0F, 24.0F, 0.0F);
        lid.yRot = 0.0F;

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.scale(DISPLAY_SCALE, DISPLAY_SCALE, DISPLAY_SCALE);
        // settle slightly toward the cage floor so it reads as resting, not floating dead-center
        poseStack.translate(0.0F, -0.35F, 0.0F);
        poseStack.mulPose(Direction.UP.getRotation());
        poseStack.scale(1.0F, -1.0F, -1.0F);
        poseStack.translate(0.0F, -1.0F, 0.0F);

        VertexConsumer consumer = material.buffer(buffer, RenderType::entityCutoutNoCull);
        model.renderToBuffer(poseStack, consumer, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
