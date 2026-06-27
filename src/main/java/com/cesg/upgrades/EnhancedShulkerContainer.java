package com.cesg.upgrades;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jetbrains.annotations.Nullable;

public class EnhancedShulkerContainer implements Container {
    private final EnhancedShulkerItemStackHandler handler;
    private final ItemStack shulkerStack;
    @Nullable
    private final Level level;
    @Nullable
    private final BlockPos blockPos;

    public EnhancedShulkerContainer(EnhancedShulkerItemStackHandler handler, ItemStack shulkerStack,
            @Nullable Level level, @Nullable BlockPos blockPos) {
        this.handler = handler;
        this.shulkerStack = shulkerStack;
        this.level = level;
        this.blockPos = blockPos;
    }

    @Override
    public int getContainerSize() {
        return handler.getSlots();
    }

    @Override
    public int getMaxStackSize() {
        return handler.getInstalledStackLimit();
    }

    /** Per-item stack cap for GUI slots; scales with stack depth and the item's vanilla max stack size. */
    public int effectiveMaxStackSize(ItemStack stack) {
        return handler.effectiveSlotLimitFor(stack);
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < getContainerSize(); i++) {
            if (!handler.getStackInSlot(i).isEmpty())
                return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return handler.getStackInSlot(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return handler.extractItem(slot, amount, false);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = handler.getStackInSlot(slot);
        if (stack.isEmpty())
            return ItemStack.EMPTY;
        handler.setStackInSlot(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        handler.setStackInSlot(slot, stack);
    }

    public boolean mayInsertStack(ItemStack stack) {
        return stack.isEmpty() || handler.isItemValid(0, stack);
    }

    @Override
    public void setChanged() {
        // Vanilla moveItemStackTo merges items into an existing slot by mutating the stack in place and
        // calling setChanged() (never the handler's insert/set methods), so this is the only hook that
        // persists shift-click stack merges. Without it, merged counts past the first placement are lost.
        handler.persist();
    }

    @Override
    public boolean stillValid(Player player) {
        if (level != null && blockPos != null) {
            return player.distanceToSqr(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5) <= 64
                    && level.getBlockEntity(blockPos) instanceof EnhancedShulkerBoxBlockEntity;
        }
        return player.getInventory().contains(shulkerStack);
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < getContainerSize(); i++)
            handler.setStackInSlot(i, ItemStack.EMPTY);
    }

    @Override
    public void startOpen(Player player) {
        if (level == null || blockPos == null)
            return;
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (blockEntity instanceof EnhancedShulkerBoxBlockEntity box)
            box.startOpen(player);
    }

    @Override
    public void stopOpen(Player player) {
        if (level == null || blockPos == null)
            return;
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (blockEntity instanceof EnhancedShulkerBoxBlockEntity box)
            box.stopOpen(player);
    }
}
