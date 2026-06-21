package com.cesg.storage.station;

import com.cesg.storage.ShulkerInventoryAccess;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

public class ShulkerHoldingHandler implements IItemHandler {
    private final ShulkerStation station;

    static IItemHandler create(ShulkerStation station) {
        return new ShulkerHoldingHandler(station);
    }

    ShulkerHoldingHandler(ShulkerStation station) {
        this.station = station;
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (slot != 0 || stack.isEmpty() || station.hasHeldShulker())
            return stack;
        if (!ShulkerInventoryAccess.isShulkerBox(stack))
            return stack;

        if (!simulate)
            station.setHeldShulker(stack.copyWithCount(1));

        return stack.getCount() <= 1 ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - 1);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return ShulkerInventoryAccess.isShulkerBox(stack);
    }
}
