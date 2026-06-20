package com.cesg.farming;

import com.cesg.storage.ShulkerInventoryAccess;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

public class ShulkerDuplicationAidBlock extends BaseEntityBlock {
    public static final MapCodec<ShulkerDuplicationAidBlock> CODEC = simpleCodec(ShulkerDuplicationAidBlock::new);

    public ShulkerDuplicationAidBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShulkerDuplicationAidBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide)
            return ItemInteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ShulkerDuplicationAidBlockEntity aid))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        ItemStack held = player.getItemInHand(hand);
        if (!held.isEmpty() && ShulkerInventoryAccess.isShulkerBox(held) && aid.getHeldShulker().isEmpty()) {
            aid.setHeldShulker(held.split(1));
            return ItemInteractionResult.SUCCESS;
        }

        if (held.isEmpty() && !aid.getHeldShulker().isEmpty()) {
            player.getInventory().placeItemBackInInventory(aid.getHeldShulker());
            aid.setHeldShulker(ItemStack.EMPTY);
            return ItemInteractionResult.SUCCESS;
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (level.isClientSide)
            return;
        BlockEntity be = level.getBlockEntity(hit.getBlockPos());
        if (be instanceof ShulkerDuplicationAidBlockEntity aid)
            aid.onProjectileHit();
    }
}
