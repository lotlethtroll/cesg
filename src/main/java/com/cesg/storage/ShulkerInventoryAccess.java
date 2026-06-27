package com.cesg.storage;

import com.cesg.init.CESGDataComponents;
import com.cesg.upgrades.EnhancedShulkerBoxes;
import com.cesg.upgrades.EnhancedShulkerContents;
import com.cesg.upgrades.EnhancedShulkerItemStackHandler;
import com.cesg.upgrades.ShulkerUpgradeItems;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.neoforged.neoforge.items.ComponentItemHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import org.jetbrains.annotations.Nullable;

public final class ShulkerInventoryAccess {
    private static final IItemHandler EMPTY = new ItemStackHandler(0);

    private ShulkerInventoryAccess() {}

    public static boolean isShulkerBox(ItemStack stack) {
        if (stack.isEmpty())
            return false;
        Item item = stack.getItem();
        if (item instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock)
            return true;
        return EnhancedShulkerBoxes.isEnhancedShulker(stack);
    }

    public static int getTier(ItemStack stack) {
        EnhancedShulkerContents contents = stack.get(CESGDataComponents.ENHANCED_SHULKER);
        if (contents != null)
            return contents.tier();
        if (isVanillaShulker(stack))
            return 1;
        return 0;
    }

    public static boolean isVanillaShulker(ItemStack stack) {
        // Enhanced shulkers extend ShulkerBoxBlock, so they must be excluded here or they would be
        // routed through the vanilla CONTAINER component (27 slots) instead of ENHANCED_SHULKER.
        if (EnhancedShulkerBoxes.isEnhancedShulker(stack))
            return false;
        Item item = stack.getItem();
        return item instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock;
    }

    public static IItemHandler wrap(ItemStack shulker) {
        return wrap(shulker, null);
    }

    public static IItemHandler wrap(ItemStack shulker, @Nullable Level level) {
        if (!isShulkerBox(shulker))
            return EMPTY;

        if (isVanillaShulker(shulker))
            return new ComponentItemHandler(shulker, DataComponents.CONTAINER, 27);

        EnhancedShulkerContents contents = shulker.getOrDefault(CESGDataComponents.ENHANCED_SHULKER,
                EnhancedShulkerContents.empty(2));
        return new EnhancedShulkerItemStackHandler(shulker, contents, level);
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
        EnhancedShulkerContents contents = shulker.get(CESGDataComponents.ENHANCED_SHULKER);
        if (contents != null)
            return contents.slotCount();
        return 0;
    }

    /** Clamps a station slot-threshold to the held shulker's capacity (27 / 54 / 81 / 108). */
    public static int clampThreshold(ItemStack shulker, int threshold) {
        int slots = getSlotCount(shulker);
        if (slots <= 0)
            return threshold;
        return Math.min(Math.max(threshold, 1), slots);
    }

    /** Per-slot stack cap for the held shulker (64 vanilla; 64/128/256 for enhanced with stack depth modules). */
    public static int getStackLimit(ItemStack shulker) {
        EnhancedShulkerContents contents = shulker.get(CESGDataComponents.ENHANCED_SHULKER);
        if (contents == null)
            return 64;

        NonNullList<ItemStack> upgrades = NonNullList.withSize(contents.upgradeSlotCount(), ItemStack.EMPTY);
        contents.copyUpgradesInto(upgrades);
        return ShulkerUpgradeItems.installedStackLimit(upgrades);
    }

    public static boolean isFull(ItemStack shulker) {
        IItemHandler handler = wrap(shulker);
        if (handler.getSlots() == 0)
            return false;
        for (int i = 0; i < handler.getSlots(); i++) {
            // Full means every slot is maxed to its per-slot capacity, not merely occupied, so
            // stack-depth upgrades let a box keep accepting items until each slot hits its raised limit.
            if (!isSlotSaturated(handler, i))
                return false;
        }
        return true;
    }

    /**
     * True when {@code slot} is occupied and cannot accept even one more of the item it holds. This is
     * the only handler-agnostic way to detect a full slot: vanilla boxes cap a slot at the item's max
     * stack size (their handler reports {@link net.minecraft.world.item.Item#ABSOLUTE_MAX_STACK_SIZE} as
     * the slot limit, so a raw count comparison overshoots), while enhanced boxes with stack-depth
     * modules deliberately stack past the vanilla max. Simulating a one-item insert respects both.
     */
    private static boolean isSlotSaturated(IItemHandler handler, int slot) {
        ItemStack stack = handler.getStackInSlot(slot);
        if (stack.isEmpty())
            return false;
        return !handler.insertItem(slot, stack.copyWithCount(1), true).isEmpty();
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
            // A slot counts toward the threshold only once it can hold no more of its item. Enhanced
            // boxes stack past the item's vanilla max (that's what stack-depth modules do), so a raw
            // count comparison against the handler's reported slot limit would mis-detect vanilla boxes
            // (whose handler reports 99 as the limit while only accepting the item's max stack size).
            if (!isSlotSaturated(handler, slot))
                return false;
        }

        return true;
    }

    /** True when at most {@code maxOccupiedSlots} slots still hold items. */
    public static boolean isSlotThresholdEmptied(ItemStack shulker, int maxOccupiedSlots) {
        return countOccupiedSlots(shulker) <= maxOccupiedSlots;
    }
}
