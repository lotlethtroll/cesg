package com.cesg.upgrades;

import com.cesg.init.CESGDataComponents;
import com.cesg.storage.ShulkerInventoryAccess;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Automation access (funnels, chutes, hoppers, pipes) for a PLACED enhanced shulker box. The box
 * stores its inventory inside an item-stack component, and handlers over that component are
 * snapshots — so this wrapper re-wraps whenever the contents component changes, and goes inert while
 * a player has the GUI open (two snapshot writers over one stack clobber each other: dupe/void).
 *
 * <p>Shulkers held in stations are items, not placed blocks — station capability logic is unaffected.
 */
public class EnhancedShulkerBlockItemHandler implements IItemHandler {
    private final EnhancedShulkerBoxBlockEntity box;

    private IItemHandler cached;
    private Object cachedContents;

    public EnhancedShulkerBlockItemHandler(EnhancedShulkerBoxBlockEntity box) {
        this.box = box;
    }

    /** Live view over the current contents component; rebuilt when the component object changes. */
    private IItemHandler live() {
        ItemStack stack = box.getShulkerStack();
        Object contents = stack.get(CESGDataComponents.ENHANCED_SHULKER.get());
        if (cached == null || contents != cachedContents) {
            cached = ShulkerInventoryAccess.wrap(stack, box.getLevel());
            if (cached instanceof EnhancedShulkerItemStackHandler enhanced)
                enhanced.setChangeListener(box::setChanged);
            cachedContents = contents;
        }
        return cached;
    }

    /** Own writes refresh the identity marker so the next call doesn't needlessly rebuild. */
    private void syncMarker() {
        cachedContents = box.getShulkerStack().get(CESGDataComponents.ENHANCED_SHULKER.get());
    }

    private boolean locked() {
        return box.isViewed();
    }

    @Override
    public int getSlots() {
        return locked() ? 0 : live().getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return locked() ? ItemStack.EMPTY : live().getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        // No nesting: shulker boxes never accept other shulker boxes, matching vanilla.
        if (locked() || ShulkerInventoryAccess.isShulkerBox(stack))
            return stack;
        ItemStack remainder = live().insertItem(slot, stack, simulate);
        if (!simulate)
            syncMarker();
        return remainder;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (locked())
            return ItemStack.EMPTY;
        ItemStack pulled = live().extractItem(slot, amount, simulate);
        if (!simulate)
            syncMarker();
        return pulled;
    }

    @Override
    public int getSlotLimit(int slot) {
        return locked() ? 0 : live().getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return !locked() && !ShulkerInventoryAccess.isShulkerBox(stack) && live().isItemValid(slot, stack);
    }
}
