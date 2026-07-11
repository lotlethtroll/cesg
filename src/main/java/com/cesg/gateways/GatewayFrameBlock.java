package com.cesg.gateways;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import org.jetbrains.annotations.Nullable;

/**
 * Structural ring block for the Cross-Dimensional Gateway multiblock. Build a vertical rectangular ring
 * of these (plus one Core) to form a portal. Exposes a fluid capability so Create pipes can pump gateway
 * fuel through the frame into the Core. The {@link #FUEL} state is driven by the Core: when the gateway
 * is fuelled and active, an internal conduit shows the ACTUAL fuel flowing — lilac Teleport Essence for
 * same-dimension links, green Liquid Eye of Ender for cross-dimensional ones.
 */
public class GatewayFrameBlock extends Block implements EntityBlock {
    public static final EnumProperty<FrameFuel> FUEL = EnumProperty.create("fuel", FrameFuel.class);
    /** Powered/active glow — separate from FUEL so pumped fluid shows in UNLIT frames (cosmetic flow). */
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty LIT =
            net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT;

    /** What the conduit docks into per direction: another frame, the core (gets a collar), or air. */
    public enum ConduitLink implements net.minecraft.util.StringRepresentable {
        NONE, FRAME, CORE;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final java.util.Map<net.minecraft.core.Direction, EnumProperty<ConduitLink>> CONNECTIONS =
            java.util.Map.of(
                    net.minecraft.core.Direction.NORTH, EnumProperty.create("link_north", ConduitLink.class),
                    net.minecraft.core.Direction.SOUTH, EnumProperty.create("link_south", ConduitLink.class),
                    net.minecraft.core.Direction.EAST, EnumProperty.create("link_east", ConduitLink.class),
                    net.minecraft.core.Direction.WEST, EnumProperty.create("link_west", ConduitLink.class),
                    net.minecraft.core.Direction.UP, EnumProperty.create("link_up", ConduitLink.class),
                    net.minecraft.core.Direction.DOWN, EnumProperty.create("link_down", ConduitLink.class));

    /** Which fuel is flowing through the frame's conduit (NONE = inactive). */
    public enum FrameFuel implements net.minecraft.util.StringRepresentable {
        NONE, ESSENCE, EYE;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public GatewayFrameBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any().setValue(FUEL, FrameFuel.NONE).setValue(LIT, false);
        for (EnumProperty<ConduitLink> prop : CONNECTIONS.values())
            state = state.setValue(prop, ConduitLink.NONE);
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FUEL);
        builder.add(LIT);
        CONNECTIONS.values().forEach(builder::add);
    }

    private static ConduitLink linkFor(BlockState neighbor) {
        if (neighbor.getBlock() instanceof CrossDimensionalGatewayCoreBlock)
            return ConduitLink.CORE;
        if (neighbor.getBlock() instanceof GatewayFrameBlock)
            return ConduitLink.FRAME;
        return ConduitLink.NONE;
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (var entry : CONNECTIONS.entrySet())
            state = state.setValue(entry.getValue(), linkFor(
                    context.getLevel().getBlockState(context.getClickedPos().relative(entry.getKey()))));
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, net.minecraft.core.Direction direction,
            BlockState neighborState, net.minecraft.world.level.LevelAccessor level,
            BlockPos pos, BlockPos neighborPos) {
        return state.setValue(CONNECTIONS.get(direction), linkFor(neighborState));
    }

    /**
     * Self-heal ALL six links on any neighbor change. updateShape only refreshes the single face
     * that changed, which left stale links toward the core in pre-existing rings; the core pings
     * its whole ring on load (invalidateRing -> updateNeighborsAt), so old worlds repair themselves.
     */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
            BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (level.isClientSide)
            return;
        BlockState updated = state;
        for (var entry : CONNECTIONS.entrySet())
            updated = updated.setValue(entry.getValue(),
                    linkFor(level.getBlockState(pos.relative(entry.getKey()))));
        if (updated != state)
            level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
    }

    /** Adjacent frames merge visually: hide the faces between them (vanilla glass rule). */
    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacentState, net.minecraft.core.Direction direction) {
        return adjacentState.is(this) || super.skipRendering(state, adjacentState, direction);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GatewayFrameBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
            Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        if (level.isClientSide || type != com.cesg.init.CESGBlockEntities.GATEWAY_FRAME.get())
            return null;
        return (lvl, pos, st, be) -> GatewayFrameBlockEntity.serverTick(lvl, pos, st, (GatewayFrameBlockEntity) be);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        // Only on a genuine placement (not a LIT toggle) — refresh fuel routing across the connected ring.
        if (!oldState.is(this))
            GatewayFuelHandler.invalidateRing(level, pos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!newState.is(this))
            GatewayFuelHandler.invalidateRing(level, pos);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
