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

    /** Hotbar swap (number keys / offhand) bypasses insert logic; transform the incoming stack here. */
    @Override
    public void setByPlayer(ItemStack newStack, ItemStack oldStack) {
        super.setByPlayer(shulkerContainer.transformForStorage(newStack), oldStack);
    }

    /**
     * Cursor placement goes through the handler (not direct slot writes) so the smelting and void
     * modules see manual inserts too. Voided overflow counts as accepted: it leaves the cursor.
     */
    @Override
    public ItemStack safeInsert(ItemStack cursor, int increment) {
        if (cursor.isEmpty() || !mayPlace(cursor))
            return cursor;
        int want = Math.min(increment, cursor.getCount());
        ItemStack remainder = shulkerContainer.insertViaHandler(getContainerSlot(), cursor.copyWithCount(want));
        cursor.shrink(want - remainder.getCount());
        return cursor;
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
