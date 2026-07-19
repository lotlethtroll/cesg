package com.cesg.client;

import com.cesg.CESG;
import com.cesg.upgrades.EnhancedShulkerBoxBlockEntity;
import com.cesg.upgrades.EnhancedShulkerUpgrades;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.ShulkerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import org.jetbrains.annotations.Nullable;

public class EnhancedShulkerBoxRenderer implements BlockEntityRenderer<EnhancedShulkerBoxBlockEntity> {
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private final ShulkerModel<?> model;

    public EnhancedShulkerBoxRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new ShulkerModel<>(context.bakeLayer(ModelLayers.SHULKER));
    }

    @Override
    public void render(EnhancedShulkerBoxBlockEntity blockEntity, float partialTick, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Direction direction = Direction.UP;
        Level level = blockEntity.getLevel();
        if (level != null) {
            BlockState blockState = level.getBlockState(blockEntity.getBlockPos());
            if (blockState.getBlock() instanceof ShulkerBoxBlock)
                direction = blockState.getValue(ShulkerBoxBlock.FACING);
        }

        @Nullable DyeColor color = blockEntity.getBlockState().getBlock() instanceof ShulkerBoxBlock shulkerBox
                ? shulkerBox.getColor()
                : null;
        ResourceLocation texture = enhancedTexture(color, blockEntity.displayTier());

        float progress = blockEntity.getOpenNess(partialTick);
        ModelPart lid = model.getLid();
        lid.setPos(0.0F, 24.0F - progress * 0.5F * 16.0F, 0.0F);
        lid.yRot = 270.0F * progress * DEG_TO_RAD;

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.scale(0.9995F, 0.9995F, 0.9995F);
        poseStack.mulPose(direction.getRotation());
        poseStack.scale(1.0F, -1.0F, -1.0F);
        poseStack.translate(0.0F, -1.0F, 0.0F);
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /**
     * Vanilla shulker entity texture with subtle corner-bracket trim on the lid top, colored by
     * tier: ender teal (Enhanced), andesite (Reinforced), brass (Ultimate).
     */
    public static ResourceLocation enhancedTexture(@Nullable DyeColor color, int tier) {
        String name = color == null ? "shulker" : "shulker_" + color.getName();
        int clamped = Math.max(EnhancedShulkerUpgrades.MIN_TIER, Math.min(EnhancedShulkerUpgrades.MAX_TIER, tier));
        return CESG.id("textures/entity/enhanced_shulker/" + name + "_t" + clamped + ".png");
    }

    @Override
    public AABB getRenderBoundingBox(EnhancedShulkerBoxBlockEntity blockEntity) {
        var pos = blockEntity.getBlockPos();
        return new AABB(pos.getX() - 0.5, pos.getY() - 0.5, pos.getZ() - 0.5, pos.getX() + 1.5, pos.getY() + 1.5,
                pos.getZ() + 1.5);
    }
}
