package com.cesg.storage;

import com.cesg.init.CESGDataComponents;
import com.cesg.init.CESGRegistration;
import com.cesg.upgrades.EnhancedShulkerContents;
import com.cesg.upgrades.EnhancedShulkerItemStackHandler;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.neoforged.neoforge.items.ComponentItemHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public final class ShulkerInventoryAccess {
    private static final IItemHandler EMPTY = new ItemStackHandler(0);

    private ShulkerInventoryAccess() {}

    public static boolean isShulkerBox(ItemStack stack) {
        if (stack.isEmpty())
            return false;
        Item item = stack.getItem();
        if (item instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock)
            return true;
        return stack.is(CESGRegistration.ENHANCED_SHULKER_T2.get());
    }

    public static int getTier(ItemStack stack) {
        if (stack.has(CESGDataComponents.ENHANCED_SHULKER))
            return stack.get(CESGDataComponents.ENHANCED_SHULKER).tier();
        if (isVanillaShulker(stack))
            return 1;
        return 0;
    }

    public static boolean isVanillaShulker(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock;
    }

    public static IItemHandler wrap(ItemStack shulker) {
        if (!isShulkerBox(shulker))
            return EMPTY;

        if (isVanillaShulker(shulker))
            return new ComponentItemHandler(shulker, DataComponents.CONTAINER, 27);

        EnhancedShulkerContents contents = shulker.getOrDefault(CESGDataComponents.ENHANCED_SHULKER,
                EnhancedShulkerContents.empty(2));
        return new EnhancedShulkerItemStackHandler(shulker, contents);
    }

    public static int countOccupiedSlots(ItemStack shulker) {
        IItemHandler handler = wrap(shulker);
        int count = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            if (!handler.getStackInSlot(i).isEmpty())
                count++;
        }
        return count;
    }

    public static int getSlotCount(ItemStack shulker) {
        if (isVanillaShulker(shulker))
            return 27;
        if (shulker.has(CESGDataComponents.ENHANCED_SHULKER))
            return shulker.get(CESGDataComponents.ENHANCED_SHULKER).slotCount();
        return 0;
    }

    public static boolean isFull(ItemStack shulker) {
        IItemHandler handler = wrap(shulker);
        for (int i = 0; i < handler.getSlots(); i++) {
            if (handler.getStackInSlot(i).isEmpty())
                return false;
        }
        return handler.getSlots() > 0;
    }

    public static boolean isEmpty(ItemStack shulker) {
        return countOccupiedSlots(shulker) == 0;
    }

    /**
     * {@code allowedSlots} is how many shulker slots (from index 0) may be filled before eject.
     * Returns true when those slots are all full, or when a disallowed slot already holds items.
     */
    public static boolean isSlotThresholdReached(ItemStack shulker, int allowedSlots) {
        IItemHandler handler = wrap(shulker);
        int totalSlots = handler.getSlots();
        if (totalSlots == 0)
            return false;

        int limit = Math.min(Math.max(allowedSlots, 1), totalSlots);

        for (int slot = limit; slot < totalSlots; slot++) {
            if (!handler.getStackInSlot(slot).isEmpty())
                return true;
        }

        for (int slot = 0; slot < limit; slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty())
                return false;
            int capacity = Math.min(handler.getSlotLimit(slot), stack.getMaxStackSize());
            if (stack.getCount() < capacity)
                return false;
        }

        return true;
    }

    /** True when at most {@code maxOccupiedSlots} slots still hold items. */
    public static boolean isSlotThresholdEmptied(ItemStack shulker, int maxOccupiedSlots) {
        return countOccupiedSlots(shulker) <= maxOccupiedSlots;
    }
}
