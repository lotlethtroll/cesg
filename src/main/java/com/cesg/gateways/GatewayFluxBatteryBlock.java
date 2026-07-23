package com.cesg.gateways;

import org.jetbrains.annotations.Nullable;

import com.cesg.init.CESGBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Gateway Flux Battery (Phase 7E): a fuel reservoir placed beside a gateway ring. Create pipes/pumps
 * fill it, and it tops up the connected Core's tanks to smooth bursty demand. See
 * {@link GatewayFluxBatteryBlockEntity}.
 */
public class GatewayFluxBatteryBlock extends BaseEntityBlock {
    private static final MapCodec<GatewayFluxBatteryBlock> MAP_CODEC = simpleCodec(GatewayFluxBatteryBlock::new);

    public GatewayFluxBatteryBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return MAP_CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GatewayFluxBatteryBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, CESGBlockEntities.GATEWAY_FLUX_BATTERY.get(),
                        GatewayFluxBatteryBlockEntity::serverTick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
