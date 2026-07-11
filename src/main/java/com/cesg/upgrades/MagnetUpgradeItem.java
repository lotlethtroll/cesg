package com.cesg.upgrades;

import net.minecraft.world.item.Item;

/** Tiered module: a PLACED enhanced shulker pulls in dropped items it can accept. */
public class MagnetUpgradeItem extends ShulkerUpgradeItem {
    private final int moduleTier;

    public MagnetUpgradeItem(Properties properties, int moduleTier) {
        super(properties);
        this.moduleTier = moduleTier;
    }

    public int getModuleTier() {
        return moduleTier;
    }

    /** Pull radius in blocks. */
    public static double radiusForTier(int moduleTier) {
        return switch (moduleTier) {
            case 1 -> 4.0;
            case 2 -> 7.0;
            case 3 -> 10.0;
            default -> 0.0;
        };
    }

    /** Acceleration applied toward the box per tick — higher tiers snap items in faster. */
    public static double pullStrengthForTier(int moduleTier) {
        return switch (moduleTier) {
            case 1 -> 0.06;
            case 2 -> 0.10;
            case 3 -> 0.15;
            default -> 0.0;
        };
    }

    public static boolean isMagnetUpgrade(Item item) {
        return item instanceof MagnetUpgradeItem;
    }
}
