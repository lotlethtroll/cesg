package com.cesg.upgrades;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class EnhancedShulkerBoxSlot extends Slot {
    private final EnhancedShulkerContainer shulkerContainer;
    private final int slotSize;

    public EnhancedShulkerBoxSlot(EnhancedShulkerContainer container, int slot, int x, int y, int slotSize) {
        super(container, slot, x, y);
        this.shulkerContainer = container;
        this.slotSize = slotSize;
    }

    public int getSlotSize() {
        return slotSize;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.canFitInsideContainerItems() && shulkerContainer.mayInsertStack(stack);
    }

    @Override
    public int getMaxStackSize() {
        return shulkerContainer.getMaxStackSize();
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return shulkerContainer.effectiveMaxStackSize(stack);
    }
}
