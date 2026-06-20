package com.cesg.gateways;

import com.cesg.init.CESGBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class EndGatewayBlockEntity extends BlockEntity {
    public EndGatewayBlockEntity(BlockPos pos, BlockState state) {
        super(CESGBlockEntities.END_GATEWAY.get(), pos, state);
    }
}
