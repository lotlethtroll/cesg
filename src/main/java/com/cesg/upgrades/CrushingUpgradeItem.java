package com.cesg.upgrades;

import net.minecraft.world.item.Item;

/**
 * Tiered module (Phase 7D): a PLACED enhanced shulker crushes/mills its contents in place, using
 * Create crushing + milling recipes. Chains to a terminal form (cobblestone → gravel → sand) like the
 * Smelting module. Higher tiers run more conversions per interval.
 */
public class CrushingUpgradeItem extends ShulkerUpgradeItem {
    private final int moduleTier;

    public CrushingUpgradeItem(Properties properties, int moduleTier) {
        super(properties);
        this.moduleTier = moduleTier;
    }

    public int getModuleTier() {
        return moduleTier;
    }

    /** Conversions performed per processing interval (see EnhancedShulkerBoxBlockEntity). */
    public static int operationsForTier(int moduleTier) {
        return switch (moduleTier) {
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 4;
            default -> 0;
        };
    }

    public static boolean isCrushingUpgrade(Item item) {
        return item instanceof CrushingUpgradeItem;
    }
}
