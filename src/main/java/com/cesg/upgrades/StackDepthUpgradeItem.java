package com.cesg.upgrades;

import net.minecraft.world.item.Item;

/** Tiered module that raises per-slot stack limits when installed in an enhanced shulker sidebar. */
public class StackDepthUpgradeItem extends ShulkerUpgradeItem {
    private final int moduleTier;

    public StackDepthUpgradeItem(Properties properties, int moduleTier) {
        super(properties);
        this.moduleTier = moduleTier;
    }

    public int getModuleTier() {
        return moduleTier;
    }

    /** Per-slot stack cap granted by this module (Milestone 2 behavior). */
    public int getStackLimit() {
        return stackLimitForTier(moduleTier);
    }

    public static int stackLimitForTier(int moduleTier) {
        return switch (moduleTier) {
            case 1 -> 128;
            case 2 -> 256;
            case 3 -> 512;
            default -> 64;
        };
    }

    public static boolean isStackDepthUpgrade(Item item) {
        return item instanceof StackDepthUpgradeItem;
    }
}
