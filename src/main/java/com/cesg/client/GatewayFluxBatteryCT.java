package com.cesg.client;

import org.jetbrains.annotations.Nullable;

import com.cesg.gateways.GatewayFluxBatteryBlock;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.HorizontalCTBehaviour;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Create fluid-tank style connected textures for the Gateway Flux Battery:
 * side + top RECTANGLE shifts, plus an inner-lid shift when looking into the tank.
 */
public class GatewayFluxBatteryCT extends HorizontalCTBehaviour {
    private final CTSpriteShiftEntry innerShift;

    public GatewayFluxBatteryCT(CTSpriteShiftEntry layerShift, CTSpriteShiftEntry topShift,
            CTSpriteShiftEntry innerShift) {
        super(layerShift, topShift);
        this.innerShift = innerShift;
    }

    @Override
    public CTSpriteShiftEntry getShift(BlockState state, Direction direction, @Nullable TextureAtlasSprite sprite) {
        if (sprite != null && direction.getAxis() == Axis.Y && innerShift.getOriginal() == sprite)
            return innerShift;
        return super.getShift(state, direction, sprite);
    }

    @Override
    public boolean buildContextForOccludedDirections() {
        return true;
    }

    @Override
    public boolean connectsTo(BlockState state, BlockState other, BlockAndTintGetter reader, BlockPos pos,
            BlockPos otherPos, Direction face) {
        if (state.getBlock() != other.getBlock())
            return false;
        // ConnectivityHandler.isConnected compares the two block entities' controllers, and returns false
        // the moment either lookup comes back empty — its checked() helper is a bare instanceof on a null.
        // Ponder bakes section meshes through a reader that does not surface block entities, so a formed
        // array there would render as a grid of separate 1x1x1 tanks no matter how the controllers are
        // wired. Fall back to the blockstate only in that case: in a real level both entities exist, so
        // live behaviour is exactly as before and two unrelated tanks still never merge visually.
        if (reader.getBlockEntity(pos) == null || reader.getBlockEntity(otherPos) == null)
            return sharesArrayShell(state, other, face);
        return ConnectivityHandler.isConnected(reader, pos, otherPos);
    }

    /**
     * Blockstate-only stand-in for connectivity. The lid properties already encode array membership: a
     * cell only reports {@code BOTTOM}/{@code TOP} when it is on that face of its array, so neighbours
     * stacked within one array agree across the shared face.
     */
    private static boolean sharesArrayShell(BlockState state, BlockState other, Direction face) {
        if (face.getAxis() != Axis.Y)
            return true; // same block, side by side: the horizontal shift already handles the seam
        boolean up = face == Direction.UP;
        return !state.getValue(up ? GatewayFluxBatteryBlock.TOP : GatewayFluxBatteryBlock.BOTTOM)
                && !other.getValue(up ? GatewayFluxBatteryBlock.BOTTOM : GatewayFluxBatteryBlock.TOP);
    }
}
