package com.cesg.storage.util;

import com.cesg.storage.ShulkerInventoryAccess;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.function.Predicate;

/**
 * Exposes a shulker's internal slots for insertion while blocking per-slot extraction.
 * Whole-shulker eject uses a separate single-slot handler on the station.
 */
public class ShulkerContentsHandler implements IItemHandler {
    private IItemHandler delegate = new net.neoforged.neoforge.items.ItemStackHandler(0);
    private int maxInsertSlotExclusive = Integer.MAX_VALUE;
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

    /** Binds the station's speed-scaled throughput limiter; only throttles real (non-simulated) inserts. */
    public void setTransferBudget(TransferBudget transferBudget) {
        this.transferBudget = transferBudget == null ? TransferBudget.UNLIMITED : transferBudget;
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

    /**
     * CONTRACT: simulation must never accept more than execution will. Callers that trust the
     * simulated remainder (Create funnels/arms) take that many items from the source; a budget
     * applied only to the execute path then silently VOIDS the difference. Mirror of the dupe fixed
     * in {@link ShulkerExtractContentsHandler#extractItem}.
     */
    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (ShulkerInventoryAccess.isShulkerBox(stack))
            return stack;
        if (!itemFilter.test(stack))
            return stack;
        if (slot >= maxInsertSlotExclusive)
            return stack;

        int allowance = transferBudget.available();
        if (allowance <= 0)
            return stack;
        int requested = Math.min(allowance, stack.getCount());
        int untouched = stack.getCount() - requested;

        ItemStack leftover = delegate.insertItem(slot, stack.copyWithCount(requested), simulate);
        int accepted = requested - leftover.getCount();
        if (accepted <= 0)
            return stack;

        if (!simulate)
            transferBudget.consume(accepted);
        int remaining = untouched + leftover.getCount();
        return remaining <= 0 ? ItemStack.EMPTY : stack.copyWithCount(remaining);
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
