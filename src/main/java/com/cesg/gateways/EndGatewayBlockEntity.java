package com.cesg.gateways;

import java.util.List;

import com.cesg.init.CESGBlockEntities;
import com.cesg.util.CESGLang;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class EndGatewayBlockEntity extends KineticBlockEntity {
    public EndGatewayBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public EndGatewayBlockEntity(BlockPos pos, BlockState state) {
        this(CESGBlockEntities.END_GATEWAY.get(), pos, state);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        CESGLang.forGoggles(tooltip, "cesg.goggles.end_gateway.summary", ChatFormatting.AQUA);
        if (getSpeed() == 0)
            CESGLang.forGoggles(tooltip, "cesg.goggles.gateway.unpowered", ChatFormatting.GRAY);
        return true;
    }
}
