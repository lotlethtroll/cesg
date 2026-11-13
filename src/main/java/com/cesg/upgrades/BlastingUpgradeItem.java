package com.cesg.upgrades;

/**
 * Tiered module: a PLACED enhanced shulker runs its contents through vanilla blasting recipes
 * (raw ores → ingots). Unstackable items are never melted, and anything an installed Crushing, Washing
 * or Haunting module can process is left to that module.
 * Higher tiers run more conversions per interval, at the same rates as Crushing and Washing.
 */
public class BlastingUpgradeItem extends ShulkerUpgradeItem {
    private final int moduleTier;

    public BlastingUpgradeItem(Properties properties, int moduleTier) {
        super(properties);
        this.moduleTier = moduleTier;
    }

    public int getModuleTier() {
        return moduleTier;
    }

    /** Conversions performed per processing interval (see EnhancedShulkerBoxBlockEntity). */
    public static int operationsForTier(int moduleTier) {
        return CrushingUpgradeItem.operationsForTier(moduleTier);
    }
}
