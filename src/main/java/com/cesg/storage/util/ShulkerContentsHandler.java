package com.cesg.storage.util;

import com.cesg.storage.ShulkerInventoryAccess;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Exposes a shulker's internal slots for insertion while blocking per-slot extraction.
 * Whole-shulker eject uses a separate single-slot handler on the station.
 */
public class ShulkerContentsHandler implements IItemHandler {
    private IItemHandler delegate = new net.neoforged.neoforge.items.ItemStackHandler(0);
    private int maxInsertSlotExclusive = Integer.MAX_VALUE;

    public void setDelegate(IItemHandler delegate) {
        this.delegate = delegate;
    }

    /**
     * Rejects inserts into slot indices {@code >= maxInsertSlotExclusive}. Use to keep
     * funnel loading within the first N shulker slots until auto-eject.
     */
    public void setMaxInsertSlotExclusive(int maxInsertSlotExclusive) {
        this.maxInsertSlotExclusive = maxInsertSlotExclusive;
    }

    @Override
    public int getSlots() {
        return delegate.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return delegate.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (ShulkerInventoryAccess.isShulkerBox(stack))
            return stack;
        if (slot >= maxInsertSlotExclusive)
            return stack;
        return delegate.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return delegate.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return delegate.isItemValid(slot, stack);
    }
}
