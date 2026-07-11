package com.cesg.client;

import com.cesg.CESG;
import com.cesg.gateways.GatewayFrameBlock;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/**
 * Connected textures for the Gateway Frame (art pass Wave 2): adjacent frames merge into one large
 * brass-outlined window. Lit and unlit frames connect to each other (a ring lights as one piece);
 * each state uses its own connected sheet.
 */
public class GatewayFrameCT extends ConnectedTextureBehaviour.Base {
    private static final CTSpriteShiftEntry UNLIT = CTSpriteShifter.getCT(AllCTTypes.OMNIDIRECTIONAL,
            CESG.id("block/gateway_frame"), CESG.id("block/gateway_frame_connected"));
    private static final CTSpriteShiftEntry LIT = CTSpriteShifter.getCT(AllCTTypes.OMNIDIRECTIONAL,
            CESG.id("block/gateway_frame_lit"), CESG.id("block/gateway_frame_lit_connected"));

    @Override
    public CTSpriteShiftEntry getShift(BlockState state, Direction direction, @Nullable TextureAtlasSprite sprite) {
        return state.getValue(GatewayFrameBlock.LIT) ? LIT : UNLIT;
    }

    @Override
    public boolean connectsTo(BlockState state, BlockState other, BlockAndTintGetter reader, BlockPos pos,
            BlockPos otherPos, Direction face) {
        return state.getBlock() == other.getBlock();
    }
}
