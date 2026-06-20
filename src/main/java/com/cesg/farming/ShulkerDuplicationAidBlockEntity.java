package com.cesg.farming;

import com.cesg.init.CESGBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class ShulkerDuplicationAidBlockEntity extends BlockEntity {
    private ItemStack heldShulker = ItemStack.EMPTY;
    private int cooldown;

    public ShulkerDuplicationAidBlockEntity(BlockPos pos, BlockState state) {
        super(CESGBlockEntities.SHULKER_DUPLICATION_AID.get(), pos, state);
    }

    public ItemStack getHeldShulker() {
        return heldShulker;
    }

    public void setHeldShulker(ItemStack stack) {
        heldShulker = stack.copy();
        setChanged();
    }

    public void onProjectileHit() {
        if (heldShulker.isEmpty() || cooldown > 0)
            return;
        if (level == null)
            return;

        level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level,
                worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5,
                new ItemStack(Items.SHULKER_SHELL)));

        cooldown = 100;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!heldShulker.isEmpty())
            tag.put("HeldShulker", heldShulker.save(registries));
        tag.putInt("Cooldown", cooldown);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        heldShulker = tag.contains("HeldShulker")
                ? ItemStack.parse(registries, tag.getCompound("HeldShulker")).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        cooldown = tag.getInt("Cooldown");
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ShulkerDuplicationAidBlockEntity be) {
        if (be.cooldown > 0) {
            be.cooldown--;
            be.setChanged();
        }
    }
}
