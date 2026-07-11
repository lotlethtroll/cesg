package com.cesg.upgrades;

import com.cesg.init.CESGRegistration;
import com.cesg.storage.ShulkerInventoryAccess;
import com.cesg.init.CESGDataComponents;
import com.simibubi.create.content.logistics.filter.FilterItemStack;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/** Shared filter checks for storage handlers and goggle reject hints. */
public final class ShulkerStorageUpgrades {
    public enum FilterLayer {
        STATION,
        SHULKER
    }

    private ShulkerStorageUpgrades() {}

    public static boolean hasFilterUpgrade(Iterable<ItemStack> upgradeSlots) {
        for (ItemStack stack : upgradeSlots) {
            if (!stack.isEmpty() && stack.is(CESGRegistration.FILTER_UPGRADE))
                return true;
        }
        return false;
    }

    public static boolean hasCompactingUpgrade(Iterable<ItemStack> upgradeSlots) {
        for (ItemStack stack : upgradeSlots) {
            if (!stack.isEmpty() && stack.is(CESGRegistration.COMPACTING_UPGRADE))
                return true;
        }
        return false;
    }

    public static boolean hasSmeltingUpgrade(Iterable<ItemStack> upgradeSlots) {
        for (ItemStack stack : upgradeSlots) {
            if (!stack.isEmpty() && stack.is(CESGRegistration.SMELTING_UPGRADE))
                return true;
        }
        return false;
    }

    public static boolean hasVoidUpgrade(Iterable<ItemStack> upgradeSlots) {
        for (ItemStack stack : upgradeSlots) {
            if (!stack.isEmpty() && stack.is(CESGRegistration.VOID_UPGRADE))
                return true;
        }
        return false;
    }

    public static boolean isValidFilterConfiguration(ItemStack stack) {
        return !stack.isEmpty() && !ShulkerInventoryAccess.isShulkerBox(stack);
    }

    /**
     * Returns true when the candidate may enter storage. When no filter module is installed, all items pass.
     * An empty configured filter accepts all items (Create filter semantics).
     */
    public static boolean passesStorageFilter(@Nullable Level level, Iterable<ItemStack> upgradeSlots,
            ItemStack filterStack, ItemStack candidate) {
        if (candidate.isEmpty())
            return true;
        if (!hasFilterUpgrade(upgradeSlots))
            return true;
        if (filterStack.isEmpty())
            return true;
        return testFilterStack(level, filterStack, candidate);
    }

    public static boolean testFilterStack(@Nullable Level level, ItemStack filterStack, ItemStack candidate) {
        if (candidate.isEmpty())
            return true;
        if (filterStack.isEmpty())
            return true;
        if (level == null)
            return FilterItemStack.of(filterStack).test(null, candidate);
        return FilterItemStack.of(filterStack).test(level, candidate);
    }

    /** True when the held enhanced shulker has an active filter module (filter slot may still be empty). */
    public static boolean heldShulkerHasFilterUpgrade(ItemStack shulker) {
        EnhancedShulkerContents contents = shulker.get(CESGDataComponents.ENHANCED_SHULKER);
        if (contents == null)
            return false;
        NonNullList<ItemStack> upgrades = NonNullList.withSize(contents.upgradeSlotCount(), ItemStack.EMPTY);
        contents.copyUpgradesInto(upgrades);
        return hasFilterUpgrade(upgrades);
    }

    public static ItemStack heldShulkerFilterStack(ItemStack shulker) {
        EnhancedShulkerContents contents = shulker.get(CESGDataComponents.ENHANCED_SHULKER);
        return contents == null ? ItemStack.EMPTY : contents.filterStack();
    }

    /**
     * Which filter layer would block {@code candidate} during station insert automation.
     * Station filter is checked first, then the docked shulker's filter module.
     */
    @Nullable
    public static FilterLayer findInsertRejectLayer(@Nullable Level level, Predicate<ItemStack> stationFilter,
            ItemStack heldShulker, ItemStack candidate) {
        if (candidate.isEmpty())
            return null;
        if (!stationFilter.test(candidate))
            return FilterLayer.STATION;

        EnhancedShulkerContents contents = heldShulker.get(CESGDataComponents.ENHANCED_SHULKER);
        if (contents == null)
            return null;

        NonNullList<ItemStack> upgrades = NonNullList.withSize(contents.upgradeSlotCount(), ItemStack.EMPTY);
        contents.copyUpgradesInto(upgrades);
        if (!passesStorageFilter(level, upgrades, contents.filterStack(), candidate))
            return FilterLayer.SHULKER;
        return null;
    }

    /**
     * Which filter layer would block extracting {@code candidate} from a docked shulker at a station.
     * Only the station filter applies to extraction handlers.
     */
    @Nullable
    public static FilterLayer findExtractRejectLayer(Predicate<ItemStack> stationFilter, ItemStack candidate) {
        if (candidate.isEmpty())
            return null;
        if (!stationFilter.test(candidate))
            return FilterLayer.STATION;
        return null;
    }

    /** Merges partial stacks of the same item up to {@code slotLimit} per slot (no item budget). */
    public static void compactStacks(NonNullList<ItemStack> stacks, int slotLimit) {
        compactStacks(stacks, slotLimit, Integer.MAX_VALUE);
    }

    /** Per automation pass; aligns with station transfer budget (~64 items/tick). */
    public static final int AUTOMATION_COMPACT_ITEM_BUDGET = 64;

    public record CompactResult(int itemsMoved, boolean complete) {}

    /**
     * Merges partial stacks, moving at most {@code maxItemsToMove} items total.
     * {@code complete} is false when mergeable partial stacks remain after the pass.
     */
    public static CompactResult compactStacks(NonNullList<ItemStack> stacks, int slotLimit, int maxItemsToMove) {
        int moved = 0;
        boolean changed;
        do {
            changed = false;
            for (int i = 0; i < stacks.size(); i++) {
                ItemStack left = stacks.get(i);
                if (left.isEmpty())
                    continue;
                for (int j = i + 1; j < stacks.size(); j++) {
                    ItemStack right = stacks.get(j);
                    if (right.isEmpty() || !ItemStack.isSameItemSameComponents(left, right))
                        continue;
                    int itemLimit = ShulkerUpgradeItems.effectiveSlotLimit(left, slotLimit);
                    int space = itemLimit - left.getCount();
                    if (space <= 0)
                        break;
                    if (moved >= maxItemsToMove)
                        return new CompactResult(moved, false);
                    int toMove = Math.min(space, right.getCount());
                    if (moved + toMove > maxItemsToMove)
                        toMove = maxItemsToMove - moved;
                    if (toMove <= 0)
                        return new CompactResult(moved, false);
                    left.grow(toMove);
                    stacks.set(i, left);
                    right.shrink(toMove);
                    if (right.isEmpty())
                        stacks.set(j, ItemStack.EMPTY);
                    else
                        stacks.set(j, right);
                    moved += toMove;
                    changed = true;
                }
            }
        } while (changed);
        return new CompactResult(moved, !hasMergeablePartialStacks(stacks, slotLimit));
    }

    /** True when {@link #compactStacks} would still merge at least one item. */
    public static boolean hasMergeablePartialStacks(NonNullList<ItemStack> stacks, int slotLimit) {
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack left = stacks.get(i);
            if (left.isEmpty())
                continue;
            int leftLimit = ShulkerUpgradeItems.effectiveSlotLimit(left, slotLimit);
            if (left.getCount() >= leftLimit)
                continue;
            for (int j = i + 1; j < stacks.size(); j++) {
                ItemStack right = stacks.get(j);
                if (right.isEmpty() || !ItemStack.isSameItemSameComponents(left, right))
                    continue;
                if (right.getCount() > 0)
                    return true;
            }
        }
        return false;
    }
}
