package com.cesg.storage.station;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

public class ShulkerBoxEjectHandler implements IItemHandler {
    private final ShulkerStation station;
    private final Direction side;

    public ShulkerBoxEjectHandler(ShulkerStation station, Direction side) {
        this.station = station;
        this.side = side;
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return station.canExposeShulkerForEject(side) ? station.getHeldShulker() : ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!station.canExposeShulkerForEject(side) || amount < 1)
            return ItemStack.EMPTY;

        ItemStack extracted = station.getHeldShulker().copy();
        if (!simulate)
            station.clearHeldShulkerAfterEject();
        return extracted;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return false;
    }
}
