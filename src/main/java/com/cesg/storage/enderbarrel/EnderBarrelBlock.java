package com.cesg.storage.enderbarrel;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.cesg.init.CESGDataComponents;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Half of a twinned inventory ("Ender Barrel"): crafting yields two barrels sharing a pair id;
 * place them anywhere — any dimension — and both open (and pipe into) the SAME 27 slots. Unlike an
 * Ender Chest the pool is per-pair, not per-player, and any number of pairs can exist.
 */
public class EnderBarrelBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private static final MapCodec<EnderBarrelBlock> MAP_CODEC = simpleCodec(EnderBarrelBlock::new);

    public EnderBarrelBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction direction = context.getClickedFace();
        if (context.isSecondaryUseActive()) {
            return defaultBlockState().setValue(FACING, direction);
        }
        if (direction == Direction.UP) {
            return defaultBlockState().setValue(FACING, Direction.UP);
        }
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return MAP_CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnderBarrelBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof EnderBarrelBlockEntity barrel) {
            UUID pairId = stack.get(CESGDataComponents.ENDER_BARREL_PAIR.get());
            // A component-less barrel (creative menu) starts its own pool and tags the REST of the
            // held stack with it, so the remaining copies become its twins.
            if (pairId == null) {
                pairId = UUID.randomUUID();
                if (!stack.isEmpty())
                    stack.set(CESGDataComponents.ENDER_BARREL_PAIR.get(), pairId);
            }
            barrel.setPairId(pairId);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (level.isClientSide)
            return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof EnderBarrelBlockEntity barrel))
            return InteractionResult.PASS;
        SimpleContainer pool = barrel.sharedPool();
        if (pool == null)
            return InteractionResult.CONSUME;
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, p) -> ChestMenu.threeRows(containerId, inventory, pool),
                Component.translatable("block.cesg.ender_barrel")));
        return InteractionResult.CONSUME;
    }

    /** The pair id must survive breaking, so the block drops itself with the component attached. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof EnderBarrelBlockEntity barrel
                    && barrel.getPairId() != null && !level.isClientSide) {
                ItemStack drop = new ItemStack(this);
                drop.set(CESGDataComponents.ENDER_BARREL_PAIR.get(), barrel.getPairId());
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), drop);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
