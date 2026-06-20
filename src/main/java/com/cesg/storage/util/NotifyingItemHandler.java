package com.cesg.storage.util;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

public class NotifyingItemHandler implements IItemHandler {
    private final IItemHandler delegate;
    private final Runnable onChanged;

    public NotifyingItemHandler(IItemHandler delegate, Runnable onChanged) {
        this.delegate = delegate;
        this.onChanged = onChanged;
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
        ItemStack result = delegate.insertItem(slot, stack, simulate);
        if (!simulate && (result.getCount() != stack.getCount()))
            onChanged.run();
        return result;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStack result = delegate.extractItem(slot, amount, simulate);
        if (!simulate && !result.isEmpty())
            onChanged.run();
        return result;
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
