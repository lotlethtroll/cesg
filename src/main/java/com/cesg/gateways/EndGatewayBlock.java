package com.cesg.gateways;

import com.cesg.gateways.teleport.TeleportResolver;
import com.cesg.init.CESGBlockEntities;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Fabricated End Gateway: a kinetic block with a shaft socket on the back (rotation input). Inert
 * unless it is actually spinning — only a powered gateway will whisk a player to the central island.
 */
public class EndGatewayBlock extends DirectionalKineticBlock implements IBE<EndGatewayBlockEntity> {

    public EndGatewayBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<EndGatewayBlockEntity> getBlockEntityClass() {
        return EndGatewayBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends EndGatewayBlockEntity> getBlockEntityType() {
        return CESGBlockEntities.END_GATEWAY.get();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING).getOpposite();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer))
            return InteractionResult.SUCCESS;

        if (!serverLevel.dimension().equals(Level.END)) {
            player.displayClientMessage(Component.translatable("cesg.gateway.end_only"), true);
            return InteractionResult.FAIL;
        }

        EndGatewayBlockEntity gateway = getBlockEntity(level, pos);
        if (gateway == null || gateway.getSpeed() == 0) {
            player.displayClientMessage(Component.translatable("cesg.gateway.unpowered"), true);
            return InteractionResult.FAIL;
        }

        TeleportResolver.teleportToCentralIsland(serverPlayer, serverLevel);
        return InteractionResult.SUCCESS;
    }
}
