package com.cesg.client;

import com.cesg.upgrades.EnhancedShulkerUpgrades;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ShulkerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;

import org.jetbrains.annotations.Nullable;

/**
 * Item preview for enhanced shulker boxes: the closed shulker model with the same tier-trimmed
 * texture the block renderer uses, so the lid trim is visible in inventories and in hand.
 */
public class EnhancedShulkerBoxItemRenderer extends BlockEntityWithoutLevelRenderer {
    private ShulkerModel<?> model;

    public EnhancedShulkerBoxItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        model = null;
    }

    private ShulkerModel<?> model() {
        if (model == null)
            model = new ShulkerModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.SHULKER));
        return model;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight, int packedOverlay) {
        @Nullable DyeColor color = stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof ShulkerBoxBlock shulkerBox
                        ? shulkerBox.getColor()
                        : null;
        ResourceLocation texture = EnhancedShulkerBoxRenderer.enhancedTexture(color,
                EnhancedShulkerUpgrades.tierOf(stack));

        ShulkerModel<?> model = model();
        ModelPart lid = model.getLid();
        lid.setPos(0.0F, 24.0F, 0.0F);
        lid.yRot = 0.0F;

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.scale(0.9995F, 0.9995F, 0.9995F);
        poseStack.scale(1.0F, -1.0F, -1.0F);
        poseStack.translate(0.0F, -1.0F, 0.0F);
        model.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(texture)), packedLight,
                packedOverlay);
        poseStack.popPose();
    }
}
