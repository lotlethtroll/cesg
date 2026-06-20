package com.cesg.storage.util;

import com.cesg.storage.ShulkerInventoryAccess;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Exposes a shulker's internal slots for extraction while blocking insertion.
 * Used by the stationary Shulker Unloader.
 */
public class ShulkerExtractContentsHandler implements IItemHandler {
    private IItemHandler delegate = new net.neoforged.neoforge.items.ItemStackHandler(0);
    private int minOccupiedSlotsForExtract = 0;

    public void setDelegate(IItemHandler delegate) {
        this.delegate = delegate;
    }

    /**
     * Blocks extraction once occupied slots drop below this count (keeps at least N slots of items).
     */
    public void setMinOccupiedSlotsForExtract(int minOccupiedSlotsForExtract) {
        this.minOccupiedSlotsForExtract = minOccupiedSlotsForExtract;
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
        return stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (countOccupiedSlots() <= minOccupiedSlotsForExtract)
            return ItemStack.EMPTY;
        return delegate.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return delegate.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return false;
    }

    private int countOccupiedSlots() {
        int count = 0;
        for (int i = 0; i < delegate.getSlots(); i++) {
            if (!delegate.getStackInSlot(i).isEmpty())
                count++;
        }
        return count;
    }
}
