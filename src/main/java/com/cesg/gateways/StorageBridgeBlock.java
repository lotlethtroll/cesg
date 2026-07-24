package com.cesg.gateways;

import org.jetbrains.annotations.Nullable;

import com.cesg.init.CESGBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Phase 7A Cross-Dimensional Storage Bridge: place beside a Gateway Frame or Core, adjacent to a Storage
 * Network. It joins the local network's block-adjacency cluster and links it to the partner network across
 * the bound gateway. See {@link StorageBridgeBlockEntity} for the transfer rules.
 */
public class StorageBridgeBlock extends BaseEntityBlock {
    private static final MapCodec<StorageBridgeBlock> MAP_CODEC = simpleCodec(StorageBridgeBlock::new);

    public StorageBridgeBlock(Properties properties) {
        super(properties);
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
