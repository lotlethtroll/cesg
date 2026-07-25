package com.cesg.client;

import com.cesg.gateways.GatewayFluxBatteryBlockEntity;
import com.cesg.gateways.GatewayFuelHandler;
import com.cesg.init.CESGFluids;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import org.joml.Matrix4f;

/**
 * Controller: fluid fill through windows + one full-height charge gauge per exterior array face.
 * Every member: Port-style conduit socket only on faces that touch a gateway frame/core.
 */
public class GatewayFluxBatteryRenderer extends SafeBlockEntityRenderer<GatewayFluxBatteryBlockEntity> {
    private static final ResourceLocation WHITE = ResourceLocation.parse("minecraft:block/white_concrete");
    private static final float FACE_EPS = 0.002f;

    // One gauge per array face: narrow strip on the left pillar (avoids covering center windows)
    private static final float GAUGE_W = 2.5f / 16f;
    private static final float GAUGE_INSET = 2f / 16f; // from the left edge of the face
    private static final float GAUGE_PAD_Y = 3f / 16f;

    private static final float SOCK_MIN = 3f / 16f;
    private static final float SOCK_MAX = 13f / 16f;
    private static final float SOCK_INNER_MIN = 5f / 16f;
    private static final float SOCK_INNER_MAX = 11f / 16f;
    private static final float SOCK_HOLE_MIN = 7f / 16f;
    private static final float SOCK_HOLE_MAX = 9f / 16f;

    private static final int BRASS_R = 206, BRASS_G = 160, BRASS_B = 90;
    private static final int GUN_R = 56, GUN_G = 56, GUN_B = 56;
    private static final int TEAL_R = 48, TEAL_G = 110, TEAL_B = 104;
    private static final int VOID_R = 12, VOID_G = 10, VOID_B = 24;
    private static final int ESSENCE_R = 120, ESSENCE_G = 96, ESSENCE_B = 172;
    private static final int EYE_R = 56, EYE_G = 132, EYE_B = 88;
    private static final int TRACK_R = 40, TRACK_G = 36, TRACK_B = 48;

    public GatewayFluxBatteryRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    protected void renderSafe(GatewayFluxBatteryBlockEntity be, float partialTicks, PoseStack ms,
            MultiBufferSource buffer, int light, int overlay) {
        if (be.isController()) {
            if (be.hasWindow())
                renderFluid(be, partialTicks, ms, buffer, light);
            renderArrayGauges(be, partialTicks, ms, buffer, light);
        }
        renderRingSockets(be, ms, buffer, light);
    }

    private static void renderFluid(GatewayFluxBatteryBlockEntity be, float partialTicks, PoseStack ms,
            MultiBufferSource buffer, int light) {
        LerpedFloat fluidLevel = be.getFluidLevel();
        if (fluidLevel == null)
            return;

        float capHeight = 1 / 4f;
        float tankHullWidth = 1 / 16f + 1 / 128f;
        float minPuddleHeight = 1 / 16f;
        float totalHeight = be.getHeight() - 2 * capHeight - minPuddleHeight;

        float level = fluidLevel.getValue(partialTicks);
        if (level < 1 / (512f * totalHeight))
            return;
        float clampedLevel = Mth.clamp(level * totalHeight, 0, totalHeight);

        FluidTank tank = be.getTankInventory();
        FluidStack fluidStack = tank.getFluid();
        if (fluidStack.isEmpty())
            return;

        boolean top = fluidStack.getFluid().getFluidType().isLighterThanAir();

        float xMin = tankHullWidth;
        float xMax = xMin + be.getWidth() - 2 * tankHullWidth;
        float yMin = totalHeight + capHeight + minPuddleHeight - clampedLevel;
        float yMax = yMin + clampedLevel;
        if (top) {
            yMin += totalHeight - clampedLevel;
            yMax += totalHeight - clampedLevel;
        }
        float zMin = tankHullWidth;
        float zMax = zMin + be.getWidth() - 2 * tankHullWidth;

        ms.pushPose();
        ms.translate(0, clampedLevel - totalHeight, 0);
        NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(fluidStack, xMin, yMin, zMin, xMax, yMax, zMax, buffer,
                ms, light, false, true);
        ms.popPose();
    }

    /** One gauge centered on each horizontal exterior face of the whole array (controller only). */
    private static void renderArrayGauges(GatewayFluxBatteryBlockEntity controller, float partialTicks, PoseStack ms,
            MultiBufferSource buffer, int light) {
        Level level = controller.getLevel();
        if (level == null)
            return;

        float fill = 0f;
        LerpedFloat fluidLevel = controller.getFluidLevel();
        if (fluidLevel != null)
            fill = Mth.clamp(fluidLevel.getValue(partialTicks), 0f, 1f);
        else
            fill = controller.getFillState();

        FluidStack fluid = controller.getTankInventory().getFluid();
        int fillR = ESSENCE_R, fillG = ESSENCE_G, fillB = ESSENCE_B;
        if (!fluid.isEmpty() && fluid.getFluid().getFluidType() == CESGFluids.LIQUID_EYE_OF_ENDER.getType()) {
            fillR = EYE_R;
            fillG = EYE_G;
            fillB = EYE_B;
        }

        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(WHITE);
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        Matrix4f m = ms.last().pose();
        float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();

        int w = controller.getWidth();
        int h = controller.getHeight();
        BlockPos origin = controller.getBlockPos();
        float y0 = GAUGE_PAD_Y;
        float y1 = h - GAUGE_PAD_Y;

        for (Direction face : Iterate.horizontalDirections) {
            // Skip faces that are entirely against a ring (socket handles those) or blocked
            if (faceFullyAgainstRing(level, origin, w, h, face))
                continue;

            // Left side of the face when viewed from outside (brass pillar, not window glass)
            float uA = GAUGE_INSET;
            float uB = GAUGE_INSET + GAUGE_W;
            float out = -FACE_EPS;

            // Track spanning full array height
            arrayFaceQuad(vc, m, face, w, uA, y0, uB, y1, out,
                    TRACK_R, TRACK_G, TRACK_B, 255, light, u0, u1, v0, v1);
            if (fill <= 0.001f)
                continue;
            float fillTop = y0 + fill * (y1 - y0);
            arrayFaceQuad(vc, m, face, w, uA + 1f / 64f, y0, uB - 1f / 64f, fillTop, out - FACE_EPS,
                    fillR, fillG, fillB, 255, LightTexture.FULL_BRIGHT, u0, u1, v0, v1);
        }
    }

    private static boolean faceFullyAgainstRing(Level level, BlockPos origin, int w, int h, Direction face) {
        int ringHits = 0;
        int total = 0;
        for (int y = 0; y < h; y++) {
            for (int i = 0; i < w; i++) {
                BlockPos cell = switch (face) {
                    case NORTH -> origin.offset(i, y, 0);
                    case SOUTH -> origin.offset(i, y, w - 1);
                    case WEST -> origin.offset(0, y, i);
                    case EAST -> origin.offset(w - 1, y, i);
                    default -> origin;
                };
                total++;
                if (GatewayFuelHandler.isRingBlock(level.getBlockState(cell.relative(face))))
                    ringHits++;
            }
        }
        return total > 0 && ringHits == total;
    }

    /** Sockets stay per-member — only the block face actually touching the ring. */
    private static void renderRingSockets(GatewayFluxBatteryBlockEntity be, PoseStack ms, MultiBufferSource buffer,
            int light) {
        Level level = be.getLevel();
        if (level == null)
            return;

        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(WHITE);
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        Matrix4f m = ms.last().pose();
        float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
        BlockPos pos = be.getBlockPos();

        for (Direction face : Iterate.horizontalDirections) {
            if (ConnectivityHandler.isConnected(level, pos, pos.relative(face)))
                continue;
            BlockState neighbor = level.getBlockState(pos.relative(face));
            if (GatewayFuelHandler.isRingBlock(neighbor))
                renderSocket(vc, m, face, light, u0, u1, v0, v1);
        }
    }

    private static void renderSocket(VertexConsumer vc, Matrix4f m, Direction face, int light,
            float su0, float su1, float sv0, float sv1) {
        float out = -FACE_EPS;
        blockFaceQuad(vc, m, face, SOCK_MIN, SOCK_MIN, SOCK_MAX, SOCK_MAX, out,
                BRASS_R, BRASS_G, BRASS_B, 255, light, su0, su1, sv0, sv1);
        blockFaceQuad(vc, m, face, SOCK_INNER_MIN, SOCK_INNER_MIN, SOCK_INNER_MAX, SOCK_INNER_MAX, out - FACE_EPS,
                GUN_R, GUN_G, GUN_B, 255, light, su0, su1, sv0, sv1);
        blockFaceQuad(vc, m, face, SOCK_INNER_MIN + 1f / 16f, SOCK_INNER_MIN + 1f / 16f,
                SOCK_INNER_MAX - 1f / 16f, SOCK_INNER_MAX - 1f / 16f, out - 2 * FACE_EPS,
                TEAL_R, TEAL_G, TEAL_B, 255, LightTexture.FULL_BRIGHT, su0, su1, sv0, sv1);
        blockFaceQuad(vc, m, face, SOCK_HOLE_MIN, SOCK_HOLE_MIN, SOCK_HOLE_MAX, SOCK_HOLE_MAX, out - 3 * FACE_EPS,
                VOID_R, VOID_G, VOID_B, 255, light, su0, su1, sv0, sv1);
    }

    /**
     * Quad on an array-sized face. {@code u}/{@code y} are in blocks from the controller origin,
     * with {@code u} along the face and {@code y} vertical. Pose is at the controller block.
     */
    private static void arrayFaceQuad(VertexConsumer vc, Matrix4f m, Direction face, int width,
            float u0, float y0, float u1, float y1, float out,
            int r, int g, int b, int a, int light,
            float su0, float su1, float sv0, float sv1) {
        float x0, yA, z0, x1, yB, z1, x2, yC, z2, x3, yD, z3;
        float nx = face.getStepX(), ny = face.getStepY(), nz = face.getStepZ();
        switch (face) {
            case NORTH -> {
                float z = 0f + out;
                x0 = u0; yA = y0; z0 = z;
                x1 = u1; yB = y0; z1 = z;
                x2 = u1; yC = y1; z2 = z;
                x3 = u0; yD = y1; z3 = z;
                // viewing from north: left is +X, so u increases to the right — swap for CCW
                x0 = width - u0; x1 = width - u1; x2 = width - u1; x3 = width - u0;
            }
            case SOUTH -> {
                float z = width - out;
                x0 = u0; yA = y0; z0 = z;
                x1 = u1; yB = y0; z1 = z;
                x2 = u1; yC = y1; z2 = z;
                x3 = u0; yD = y1; z3 = z;
            }
            case WEST -> {
                float x = 0f + out;
                x0 = x; yA = y0; z0 = width - u0;
                x1 = x; yB = y0; z1 = width - u1;
                x2 = x; yC = y1; z2 = width - u1;
                x3 = x; yD = y1; z3 = width - u0;
            }
            case EAST -> {
                float x = width - out;
                x0 = x; yA = y0; z0 = u0;
                x1 = x; yB = y0; z1 = u1;
                x2 = x; yC = y1; z2 = u1;
                x3 = x; yD = y1; z3 = u0;
            }
            default -> {
                return;
            }
        }
        vert(vc, m, x0, yA, z0, r, g, b, a, su0, sv1, light, nx, ny, nz);
        vert(vc, m, x1, yB, z1, r, g, b, a, su1, sv1, light, nx, ny, nz);
        vert(vc, m, x2, yC, z2, r, g, b, a, su1, sv0, light, nx, ny, nz);
        vert(vc, m, x3, yD, z3, r, g, b, a, su0, sv0, light, nx, ny, nz);
    }

    private static void blockFaceQuad(VertexConsumer vc, Matrix4f m, Direction face,
            float u0, float v0, float u1, float v1, float out,
            int r, int g, int b, int a, int light,
            float su0, float su1, float sv0, float sv1) {
        float x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3;
        float nx = face.getStepX(), ny = face.getStepY(), nz = face.getStepZ();
        switch (face) {
            case NORTH -> {
                float z = 0f + out;
                x0 = 1f - u0; y0 = v0; z0 = z;
                x1 = 1f - u1; y1 = v0; z1 = z;
                x2 = 1f - u1; y2 = v1; z2 = z;
                x3 = 1f - u0; y3 = v1; z3 = z;
            }
            case SOUTH -> {
                float z = 1f - out;
                x0 = u0; y0 = v0; z0 = z;
                x1 = u1; y1 = v0; z1 = z;
                x2 = u1; y2 = v1; z2 = z;
                x3 = u0; y3 = v1; z3 = z;
            }
            case WEST -> {
                float x = 0f + out;
                x0 = x; y0 = v0; z0 = 1f - u0;
                x1 = x; y1 = v0; z1 = 1f - u1;
                x2 = x; y2 = v1; z2 = 1f - u1;
                x3 = x; y3 = v1; z3 = 1f - u0;
            }
            case EAST -> {
                float x = 1f - out;
                x0 = x; y0 = v0; z0 = u0;
                x1 = x; y1 = v0; z1 = u1;
                x2 = x; y2 = v1; z2 = u1;
                x3 = x; y3 = v1; z3 = u0;
            }
            default -> {
                return;
            }
        }
        vert(vc, m, x0, y0, z0, r, g, b, a, su0, sv1, light, nx, ny, nz);
        vert(vc, m, x1, y1, z1, r, g, b, a, su1, sv1, light, nx, ny, nz);
        vert(vc, m, x2, y2, z2, r, g, b, a, su1, sv0, light, nx, ny, nz);
        vert(vc, m, x3, y3, z3, r, g, b, a, su0, sv0, light, nx, ny, nz);
    }

    private static void vert(VertexConsumer vc, Matrix4f m, float x, float y, float z,
            int r, int g, int b, int a, float u, float v, int light, float nx, float ny, float nz) {
        vc.addVertex(m, x, y, z).setColor(r, g, b, a).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(nx, ny, nz);
    }

    @Override
    public boolean shouldRenderOffScreen(GatewayFluxBatteryBlockEntity be) {
        return be.isController();
    }
}
