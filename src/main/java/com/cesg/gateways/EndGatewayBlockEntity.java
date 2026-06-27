package com.cesg.gateways;

import java.util.List;

import com.cesg.init.CESGBlockEntities;
import com.cesg.util.CESGLang;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;

public class EndGatewayBlockEntity extends BlockEntity implements IHaveGoggleInformation {
    public EndGatewayBlockEntity(BlockPos pos, BlockState state) {
        super(CESGBlockEntities.END_GATEWAY.get(), pos, state);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        CESGLang.forGoggles(tooltip, "cesg.goggles.end_gateway.summary", ChatFormatting.AQUA);
        return true;
    }
}
