package com.cesg.client;

import com.cesg.machine.EnderInfuserBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

import org.joml.Matrix4f;

/** Draws the input fluid filling the Ender Infuser's tank, visible through its window faces. */
public class EnderInfuserRenderer extends SafeBlockEntityRenderer<EnderInfuserBlockEntity> {
    // Fluid fills the chamber: x 3..13, y 3..13, z 1 (just behind the window) .. 8 (chamber back wall).
    private static final float X0 = 3f / 16f;
    private static final float X1 = 13f / 16f;
    private static final float ZF = 1f / 16f;
    private static final float ZB = 8f / 16f;
    private static final float FLOOR = 3f / 16f;
    private static final float CEIL = 13f / 16f;

    public EnderInfuserRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    protected void renderSafe(EnderInfuserBlockEntity be, float partialTicks, PoseStack ms,
            MultiBufferSource buffer, int light, int overlay) {
        FluidStack fluid = be.getInput();
        if (fluid.isEmpty())
            return;
        float level = Math.min(1f, fluid.getAmount() / (float) EnderInfuserBlockEntity.TANK_CAPACITY);
        if (level <= 0f)
            return;

        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
        ResourceLocation stillRl = ext.getStillTexture(fluid);
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(stillRl);
        int tint = ext.getTintColor(fluid);
        int a = 255;
        int r = (tint >> 16) & 0xFF, g = (tint >> 8) & 0xFF, b = tint & 0xFF;
        float top = FLOOR + level * (CEIL - FLOOR);
        // glow so it reads clearly against the dark chamber regardless of world light
        light = net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;

        // The block model is rotated by FACING via the blockstate; rotate the fluid box to match so the
        // chamber/window and the fluid line up. (This is why the fluid only ever showed from a side.)
        net.minecraft.core.Direction facing = be.getBlockState().getValue(DirectionalKineticBlock.FACING);
        int xr = facing == net.minecraft.core.Direction.UP ? 270
                : facing == net.minecraft.core.Direction.DOWN ? 90 : 0;
        int yr = switch (facing) {
            case SOUTH -> 180;
            case EAST -> 90;
            case WEST -> 270;
            default -> 0;
        };

        ms.pushPose();
        ms.translate(0.5, 0.5, 0.5);
        ms.mulPose(Axis.XP.rotationDegrees(-xr));
        ms.mulPose(Axis.YP.rotationDegrees(-yr));
        ms.translate(-0.5, -0.5, -0.5);

        // no-cull cutout: renders in a BER, every face visible, opaque where the sprite is opaque
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        Matrix4f m = ms.last().pose();
        float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();

        // outward windings (CCW): top, front(north), east, west
        quad(vc, m, r, g, b, a, light, u0, u1, v0, v1, 0, 1, 0,
                X0, top, ZB, X1, top, ZB, X1, top, ZF, X0, top, ZF);
        quad(vc, m, r, g, b, a, light, u0, u1, v0, v1, 0, 0, -1,
                X1, FLOOR, ZF, X0, FLOOR, ZF, X0, top, ZF, X1, top, ZF);
        quad(vc, m, r, g, b, a, light, u0, u1, v0, v1, 1, 0, 0,
                X1, FLOOR, ZB, X1, FLOOR, ZF, X1, top, ZF, X1, top, ZB);
        quad(vc, m, r, g, b, a, light, u0, u1, v0, v1, -1, 0, 0,
                X0, FLOOR, ZF, X0, FLOOR, ZB, X0, top, ZB, X0, top, ZF);
        ms.popPose();
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private static void quad(VertexConsumer vc, Matrix4f m, int r, int g, int b, int a, int light,
            float u0, float u1, float v0, float v1, float nx, float ny, float nz,
            float x0, float y0, float z0, float x1, float y1, float z1,
            float x2, float y2, float z2, float x3, float y3, float z3) {
        vert(vc, m, x0, y0, z0, r, g, b, a, u0, v1, light, nx, ny, nz);
        vert(vc, m, x1, y1, z1, r, g, b, a, u1, v1, light, nx, ny, nz);
        vert(vc, m, x2, y2, z2, r, g, b, a, u1, v0, light, nx, ny, nz);
        vert(vc, m, x3, y3, z3, r, g, b, a, u0, v0, light, nx, ny, nz);
    }

    private static void vert(VertexConsumer vc, Matrix4f m, float x, float y, float z,
            int r, int g, int b, int a, float u, float v, int light, float nx, float ny, float nz) {
        vc.addVertex(m, x, y, z).setColor(r, g, b, a).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(nx, ny, nz);
    }
}
