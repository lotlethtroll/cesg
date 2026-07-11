package com.cesg.storage.network;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Phase 6D access point: right-click to open the network terminal — a searchable, aggregated view of
 * every member inventory, with click-to-withdraw and shift-click deposit. Must touch a member cluster
 * that contains a Storage Network Controller. Rendered as an angled console facing the placer.
 */
public class StorageTerminalBlock extends HorizontalDirectionalBlock {
    private static final MapCodec<StorageTerminalBlock> MAP_CODEC = simpleCodec(StorageTerminalBlock::new);
    /** Base slab + centered console; close enough for every facing. */
    private static final VoxelShape SHAPE = Shapes.or(
            box(0, 0, 0, 16, 5, 16),
            box(1, 5, 2, 15, 15, 14));

    public StorageTerminalBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return MAP_CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (level.isClientSide)
            return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer))
            return InteractionResult.PASS;

        if (!StorageNetwork.scan(level, pos).operational()) {
            serverPlayer.displayClientMessage(Component.translatable("cesg.network.no_controller"), true);
            return InteractionResult.SUCCESS;
        }

        serverPlayer.openMenu(new SimpleMenuProvider(
                (containerId, inventory, p) -> new StorageTerminalMenu(containerId, inventory, pos),
                Component.translatable("cesg.network.terminal_title")),
                buf -> buf.writeBlockPos(pos));
        return InteractionResult.SUCCESS;
    }
}
