package com.cesg.storage.util;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.function.Predicate;

/**
 * Exposes a shulker's internal slots for extraction while blocking insertion.
 * Used by the stationary Shulker Unloader.
 */
public class ShulkerExtractContentsHandler implements IItemHandler {
    private IItemHandler delegate = new net.neoforged.neoforge.items.ItemStackHandler(0);
    private int minOccupiedSlotsForExtract = 0;
    private Predicate<ItemStack> itemFilter = stack -> true;
    private TransferBudget transferBudget = TransferBudget.UNLIMITED;

    public void setDelegate(IItemHandler delegate) {
        this.delegate = delegate;
    }

    public IItemHandler getDelegate() {
        return delegate;
    }

    public void setItemFilter(Predicate<ItemStack> itemFilter) {
        this.itemFilter = itemFilter;
    }

    /** Binds the station's speed-scaled throughput limiter; only throttles real (non-simulated) extracts. */
    public void setTransferBudget(TransferBudget transferBudget) {
        this.transferBudget = transferBudget == null ? TransferBudget.UNLIMITED : transferBudget;
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

    /**
     * CONTRACT: simulation must never promise more than execution delivers. Create's extraction
     * helpers trust the simulated stack and spawn it even when the real extract returns less — a
     * budget (or unpowered station) applied only to the execute path therefore DUPES items
     * (simulate: full stack, execute: nothing, funnel emits the simulated stack forever).
     */
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (countOccupiedSlots() <= minOccupiedSlotsForExtract)
            return ItemStack.EMPTY;
        ItemStack inSlot = delegate.getStackInSlot(slot);
        if (!inSlot.isEmpty() && !itemFilter.test(inSlot))
            return ItemStack.EMPTY;

        int allowance = transferBudget.available();
        if (allowance <= 0)
            return ItemStack.EMPTY;
        int capped = Math.min(amount, allowance);

        if (simulate)
            return delegate.extractItem(slot, capped, true);

        ItemStack extracted = delegate.extractItem(slot, capped, false);
        transferBudget.consume(extracted.getCount());
        return extracted;
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
