package com.cesg.farming;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.cesg.init.CESGBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class ShulkerCageBlock extends BaseEntityBlock {
    private static final MapCodec<ShulkerCageBlock> MAP_CODEC = simpleCodec(ShulkerCageBlock::new);

    public ShulkerCageBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return MAP_CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShulkerCageBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, CESGBlockEntities.SHULKER_CAGE.get(), ShulkerCageBlockEntity::serverTick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (level.isClientSide)
            return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ShulkerCageBlockEntity cage) || !cage.hasTrappedShulker())
            return InteractionResult.PASS;

        if (cage.releaseShulker()) {
            level.playSound(null, pos, SoundEvents.SHULKER_AMBIENT, SoundSource.HOSTILE, 0.8F, 1.0F);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }

    /**
     * Breaking the cage drops it with the trapped shulker preserved inside the item (via block-entity
     * data), so the capture persists through mining. The shulker is only released back into the world by
     * the explicit empty-hand right-click ({@link #useWithoutItem}); we intentionally do not release on
     * removal, which would duplicate the mob.
     */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ItemStack stack = new ItemStack(this);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof ShulkerCageBlockEntity cage
                && cage.hasTrappedShulker())
            cage.saveToItem(stack, params.getLevel().registryAccess());
        return List.of(stack);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos,
            Player player) {
        ItemStack stack = super.getCloneItemStack(state, target, level, pos, player);
        if (level.getBlockEntity(pos) instanceof ShulkerCageBlockEntity cage && cage.hasTrappedShulker()
                && level instanceof Level lvl)
            cage.saveToItem(stack, lvl.registryAccess());
        return stack;
    }

    @Override
    public void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (level.isClientSide)
            return;
        BlockEntity be = level.getBlockEntity(hit.getBlockPos());
        if (be instanceof ShulkerCageBlockEntity cage)
            cage.onProjectileHit(projectile);
    }
}
