package com.cesg.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.foundation.block.connected.CTModel;

import net.createmod.catnip.data.Iterate;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelData.Builder;
import net.neoforged.neoforge.client.model.data.ModelProperty;

/**
 * CT model for the Gateway Flux Battery — culls horizontal faces shared with a connected neighbour
 * (same approach as Create's {@code FluidTankModel}).
 */
public class GatewayFluxBatteryModel extends CTModel {
    private static final ModelProperty<CullData> CULL_PROPERTY = new ModelProperty<>();

    public static GatewayFluxBatteryModel standard(BakedModel originalModel) {
        return new GatewayFluxBatteryModel(originalModel);
    }

    private GatewayFluxBatteryModel(BakedModel originalModel) {
        super(originalModel, new GatewayFluxBatteryCT(
                CESGSpriteShifts.GATEWAY_FLUX_BATTERY,
                CESGSpriteShifts.GATEWAY_FLUX_BATTERY_TOP,
                CESGSpriteShifts.GATEWAY_FLUX_BATTERY_INNER));
    }

    @Override
    protected ModelData.Builder gatherModelData(Builder builder, BlockAndTintGetter world, BlockPos pos,
            BlockState state, ModelData blockEntityData) {
        super.gatherModelData(builder, world, pos, state, blockEntityData);
        CullData cullData = new CullData();
        for (Direction d : Iterate.horizontalDirections)
            cullData.setCulled(d, ConnectivityHandler.isConnected(world, pos, pos.relative(d)));
        return builder.with(CULL_PROPERTY, cullData);
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData extraData,
            RenderType renderType) {
        if (side != null)
            return Collections.emptyList();

        List<BakedQuad> quads = new ArrayList<>();
        for (Direction d : Iterate.directions) {
            if (extraData.has(CULL_PROPERTY) && extraData.get(CULL_PROPERTY).isCulled(d))
                continue;
            quads.addAll(super.getQuads(state, d, rand, extraData, renderType));
        }
        quads.addAll(super.getQuads(state, null, rand, extraData, renderType));
        return quads;
    }

    private static class CullData {
        final boolean[] culledFaces = new boolean[4];

        CullData() {
            Arrays.fill(culledFaces, false);
        }

        void setCulled(Direction face, boolean cull) {
            if (face.getAxis().isVertical())
                return;
            culledFaces[face.get2DDataValue()] = cull;
        }

        boolean isCulled(Direction face) {
            if (face.getAxis().isVertical())
                return false;
            return culledFaces[face.get2DDataValue()];
        }
    }
}
