package com.cesg.upgrades;

import com.cesg.init.CESGRegistration;

import net.minecraft.world.item.ItemStack;

public final class ShulkerUpgradeItems {
    /** Reference ceiling for a tier-0 enhanced shulker; stack-depth tiers scale from this base. */
    public static final int BASE_STACK_LIMIT = 64;

    private ShulkerUpgradeItems() {}

    public static boolean isStackDepthUpgrade(ItemStack stack) {
        return !stack.isEmpty() && StackDepthUpgradeItem.isStackDepthUpgrade(stack.getItem());
    }

    /** True for stack depth (any tier), filter, compacting, smelting, and void upgrade items. */
    public static boolean isUpgradeItem(ItemStack stack) {
        if (stack.isEmpty())
            return false;
        return isStackDepthUpgrade(stack)
                || stack.is(CESGRegistration.FILTER_UPGRADE.get())
                || stack.is(CESGRegistration.COMPACTING_UPGRADE.get())
                || stack.is(CESGRegistration.SMELTING_UPGRADE.get())
                || stack.is(CESGRegistration.VOID_UPGRADE.get())
                || stack.getItem() instanceof MagnetUpgradeItem
                || stack.getItem() instanceof CrushingUpgradeItem
                || stack.getItem() instanceof WashingUpgradeItem;
    }

    /** Highest magnet module tier installed, or 0. Like stack depth, only the best tier applies. */
    public static int highestInstalledMagnetTier(Iterable<ItemStack> upgradeSlots) {
        int best = 0;
        for (ItemStack stack : upgradeSlots) {
            if (stack.getItem() instanceof MagnetUpgradeItem magnet)
                best = Math.max(best, magnet.getModuleTier());
        }
        return best;
    }

    /** Highest crushing module tier installed, or 0. Only the best tier applies. */
    public static int highestInstalledCrushingTier(Iterable<ItemStack> upgradeSlots) {
        int best = 0;
        for (ItemStack stack : upgradeSlots) {
            if (stack.getItem() instanceof CrushingUpgradeItem crushing)
                best = Math.max(best, crushing.getModuleTier());
        }
        return best;
    }

    /** Highest washing module tier installed, or 0. Only the best tier applies. */
    public static int highestInstalledWashingTier(Iterable<ItemStack> upgradeSlots) {
        int best = 0;
        for (ItemStack stack : upgradeSlots) {
            if (stack.getItem() instanceof WashingUpgradeItem washing)
                best = Math.max(best, washing.getModuleTier());
        }
        return best;
    }

    /** Items allowed in sidebar upgrade slots (same as {@link #isUpgradeItem}). */
    public static boolean isValidForUpgradeSlot(ItemStack stack) {
        return isUpgradeItem(stack);
    }

    /**
     * Highest stack-depth module tier installed in the given upgrade slots, or 0 if none.
     * When multiple tiers are present, only the best tier applies.
     */
    public static int highestInstalledStackDepthTier(Iterable<ItemStack> upgradeSlots) {
        int best = 0;
        for (ItemStack stack : upgradeSlots) {
            if (stack.getItem() instanceof StackDepthUpgradeItem depth)
                best = Math.max(best, depth.getModuleTier());
        }
        return best;
    }

    public static int installedStackLimit(Iterable<ItemStack> upgradeSlots) {
        int tier = highestInstalledStackDepthTier(upgradeSlots);
        return tier == 0 ? BASE_STACK_LIMIT : StackDepthUpgradeItem.stackLimitForTier(tier);
    }

    /**
     * Per-item slot capacity for an enhanced shulker. Stack-depth modules multiply the item's vanilla
     * max stack size (64 → 128/256/512, 16 → 32/64/128). Normally-unstackable items stay at one per
     * slot unless {@code allowUnstackableStacking} is true (reserved for a future upgrade module).
     * An empty stack reports the installed ceiling (the cap for standard 64-stacks).
     */
    public static int effectiveSlotLimit(ItemStack stack, int installedStackLimit) {
        return effectiveSlotLimit(stack, installedStackLimit, false);
    }

    public static int effectiveSlotLimit(ItemStack stack, int installedStackLimit,
            boolean allowUnstackableStacking) {
        if (stack.isEmpty())
            return installedStackLimit;

        int vanillaMax = stack.getMaxStackSize();
        if (vanillaMax <= 1 && !allowUnstackableStacking)
            return 1;

        int scale = Math.max(1, installedStackLimit / BASE_STACK_LIMIT);
        return vanillaMax * scale;
    }
}
