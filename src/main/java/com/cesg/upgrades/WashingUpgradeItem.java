package com.cesg.upgrades;

import net.minecraft.world.item.Item;

/**
 * Tiered module (Phase 7D): a PLACED enhanced shulker washes its contents in place, using Create
 * splashing (fan/bulk washing) recipes — gravel → flint / iron nugget, sand → clay, etc. Chains to a
 * terminal form like the Smelting module. Higher tiers run more conversions per interval.
 */
public class WashingUpgradeItem extends ShulkerUpgradeItem {
    private final int moduleTier;

    public WashingUpgradeItem(Properties properties, int moduleTier) {
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

    public static boolean isWashingUpgrade(Item item) {
        return item instanceof WashingUpgradeItem;
    }
}
