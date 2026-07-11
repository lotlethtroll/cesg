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
 * Logistics endpoint for gateway rings (Phase 6A): place beside a Gateway Frame or Core; pipes and
 * funnels insert into it, and its buffers travel through the bound gateway. See
 * {@link GatewayPortBlockEntity} for the transfer rules.
 */
public class GatewayPortBlock extends BaseEntityBlock {
    private static final MapCodec<GatewayPortBlock> MAP_CODEC = simpleCodec(GatewayPortBlock::new);

    public GatewayPortBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return MAP_CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GatewayPortBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, CESGBlockEntities.GATEWAY_PORT.get(), GatewayPortBlockEntity::serverTick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof GatewayPortBlockEntity port) {
            for (var handler : new net.neoforged.neoforge.items.ItemStackHandler[] { port.sendItems, port.receiveItems })
                for (int slot = 0; slot < handler.getSlots(); slot++)
                    if (!handler.getStackInSlot(slot).isEmpty())
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(),
                                handler.getStackInSlot(slot));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
