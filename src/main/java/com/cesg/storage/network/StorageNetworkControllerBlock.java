package com.cesg.storage.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Anchor of a Phase 6D storage network. Members (enhanced shulker boxes, shulker stations, terminals)
 * join by touching the controller cluster — no cabling. The controller itself holds no inventory;
 * right-click reports the network's size and contents summary.
 */
public class StorageNetworkControllerBlock extends Block {
    /** Open brass frame (plates + corner pillars) around the floating core. */
    private static final net.minecraft.world.phys.shapes.VoxelShape SHAPE =
            net.minecraft.world.phys.shapes.Shapes.or(
                    box(0, 0, 0, 16, 3, 16),
                    box(0, 13, 0, 16, 16, 16),
                    box(3, 3, 3, 13, 13, 13));

    public StorageNetworkControllerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,
            net.minecraft.world.level.BlockGetter level, BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer))
            return InteractionResult.SUCCESS;
        StorageNetwork.Scan scan = StorageNetwork.scan(level, pos);
        int stacks = StorageNetwork.aggregate(scan).size();
        serverPlayer.displayClientMessage(Component.translatable("cesg.network.status",
                scan.memberCount(), scan.handlers().size(), stacks), true);
        return InteractionResult.SUCCESS;
    }
}
