package com.cesg.gateways;

import org.jetbrains.annotations.Nullable;

import com.cesg.init.CESGBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Phase 7A Cross-Dimensional Storage Bridge: place beside a Gateway Frame or Core, adjacent to a Storage
 * Network. It joins the local network's block-adjacency cluster and links it to the partner network across
 * the bound gateway. See {@link StorageBridgeBlockEntity} for the transfer rules.
 */
public class StorageBridgeBlock extends BaseEntityBlock {
    private static final MapCodec<StorageBridgeBlock> MAP_CODEC = simpleCodec(StorageBridgeBlock::new);

    /**
     * Mirrors {@link StorageBridgeBlockEntity#remoteStatus()} onto the block so partner liveness is
     * readable at a glance. Without this a FAULT bridge — bound and loaded, but with no partner Bridge on
     * the far ring — looks identical to a healthy one unless the player wears goggles and hovers it.
     * Driven server-side from the flush tick; vanilla handles the client sync.
     */
    public static final EnumProperty<StorageBridgeBlockEntity.RemoteStatus> STATUS =
            EnumProperty.create("status", StorageBridgeBlockEntity.RemoteStatus.class);

    /**
     * World light shed by the backlit gauge, per status. Dim on purpose: enough to read the bridge's state
     * across a dark room and to match the emissive lens, but well under a lantern so a bank of bridges
     * doesn't light the base by itself.
     */
    public static int lightLevel(BlockState state) {
        return switch (state.getValue(STATUS)) {
            case LIVE -> 7;
            case FAULT -> 5;
            case OFFLINE -> 0;
        };
    }

    public StorageBridgeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(STATUS, StorageBridgeBlockEntity.RemoteStatus.OFFLINE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STATUS);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return MAP_CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StorageBridgeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, CESGBlockEntities.STORAGE_BRIDGE.get(), StorageBridgeBlockEntity::serverTick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (level.isClientSide)
            return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer))
            return InteractionResult.PASS;
        serverPlayer.openMenu(new SimpleMenuProvider(
                (containerId, inventory, p) -> new StorageBridgeMenu(containerId, inventory, pos),
                Component.translatable("cesg.bridge.gui.title")),
                buf -> buf.writeBlockPos(pos));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        // In-transit (buffered) items are already out of both networks — drop them so a break never voids
        // items caught mid-transfer.
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof StorageBridgeBlockEntity bridge) {
            for (var stack : bridge.bufferedContents())
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
