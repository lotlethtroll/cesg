package com.cesg.upgrades;

/**
 * Tiered module: a PLACED enhanced shulker haunts its contents in place using Create haunting
 * recipes (sandstone → end stone, sand → soul sand). It leaves alone anything an installed Crushing or
 * Washing module can process.
 * Higher tiers run more conversions per interval, at the same rates as Crushing and Washing.
 */
public class HauntingUpgradeItem extends ShulkerUpgradeItem {
    private final int moduleTier;

    public HauntingUpgradeItem(Properties properties, int moduleTier) {
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
