package com.cesg.storage.station;

import com.cesg.client.ShulkerStationConfigScreen;
import com.cesg.init.CESGBlockEntities;
import com.cesg.storage.ShulkerInventoryAccess;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.block.IBE;

import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.Tags;

public abstract class AbstractShulkerStationBlock<T extends AbstractShulkerStationBlockEntity> extends DirectionalKineticBlock
        implements IBE<T> {
    private static final VoxelShape SHAPE = box(1, 0, 1, 15, 14, 15);

    protected AbstractShulkerStationBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hitResult) {
        T be = getBlockEntity(level, pos);
        if (be == null)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (shouldOpenConfig(stack, player)) {
            if (level.isClientSide)
                CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> openConfigScreen(be, player));
            return ItemInteractionResult.SUCCESS;
        }

        if (level.isClientSide)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        ItemStack held = player.getItemInHand(hand);
        if (!held.isEmpty() && ShulkerInventoryAccess.isShulkerBox(held) && be.getHeldShulker().isEmpty()) {
            be.setHeldShulker(held.split(1));
            return ItemInteractionResult.SUCCESS;
        }

        if (held.isEmpty() && !be.getHeldShulker().isEmpty()) {
            player.getInventory().placeItemBackInInventory(be.getHeldShulker());
            be.setHeldShulker(ItemStack.EMPTY);
            return ItemInteractionResult.SUCCESS;
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private static boolean shouldOpenConfig(ItemStack held, Player player) {
        return held.is(Tags.Items.TOOLS_WRENCH) || (player.isShiftKeyDown() && held.isEmpty());
    }

    @OnlyIn(Dist.CLIENT)
    private static void openConfigScreen(AbstractShulkerStationBlockEntity be, Player player) {
        if (!(player instanceof LocalPlayer))
            return;
        ScreenOpener.open(new ShulkerStationConfigScreen(be));
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof AbstractShulkerStationBlockEntity station && !station.getHeldShulker().isEmpty())
                popResource(level, pos, station.getHeldShulker());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
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
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction preferred = context.getNearestLookingDirection().getOpposite();
        return defaultBlockState().setValue(FACING, context.getPlayer() != null && context.getPlayer().isShiftKeyDown()
                ? preferred.getOpposite()
                : preferred);
    }
}
