package com.cesg.upgrades;

import com.cesg.init.CESGDataComponents;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;

import org.jetbrains.annotations.Nullable;

/** Crafting and migration helpers for enhanced shulker tier progression. */
public final class EnhancedShulkerUpgrades {
    public static final int MIN_TIER = 2;
    public static final int MAX_TIER = 4;

    private EnhancedShulkerUpgrades() {}

    public static boolean isVanillaShulkerBox(ItemStack stack) {
        if (stack.isEmpty() || EnhancedShulkerBoxes.isEnhancedShulker(stack))
            return false;
        if (!(stack.getItem() instanceof BlockItem blockItem))
            return false;
        return blockItem.getBlock() instanceof ShulkerBoxBlock;
    }

    @Nullable
    public static DyeColor getVanillaShulkerColor(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem))
            return null;
        if (!(blockItem.getBlock() instanceof ShulkerBoxBlock shulkerBox))
            return null;
        return shulkerBox.getColor();
    }

    public static int tierOf(ItemStack stack) {
        if (!EnhancedShulkerBoxes.isEnhancedShulker(stack))
            return 0;
        EnhancedShulkerContents contents = stack.get(CESGDataComponents.ENHANCED_SHULKER.get());
        return contents == null ? MIN_TIER : contents.tier();
    }

    /** Builds the upgraded box, copying contents and display name from {@code source}. */
    public static ItemStack buildUpgradedStack(ItemStack source, int targetTier) {
        targetTier = clampTier(targetTier);
        EnhancedShulkerContents newContents;

        if (isVanillaShulkerBox(source)) {
            ItemContainerContents vanilla = source.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
            newContents = EnhancedShulkerContents.migrateFromVanilla(targetTier, vanilla);
        } else if (EnhancedShulkerBoxes.isEnhancedShulker(source)) {
            EnhancedShulkerContents existing = source.get(CESGDataComponents.ENHANCED_SHULKER.get());
            if (existing == null)
                existing = EnhancedShulkerContents.forTier(MIN_TIER);
            newContents = EnhancedShulkerContents.upgradeTier(existing, targetTier);
        } else {
            return ItemStack.EMPTY;
        }

        ItemStack result;
        if (EnhancedShulkerBoxes.isEnhancedShulker(source)) {
            result = new ItemStack(source.getItem());
        } else {
            result = new ItemStack(EnhancedShulkerBoxes.byColor(getVanillaShulkerColor(source)).get());
        }
        result.set(CESGDataComponents.ENHANCED_SHULKER.get(), newContents);
        if (source.has(DataComponents.CUSTOM_NAME))
            result.set(DataComponents.CUSTOM_NAME, source.get(DataComponents.CUSTOM_NAME));
        return result;
    }

    public static int clampTier(int tier) {
        return Math.max(MIN_TIER, Math.min(MAX_TIER, tier));
    }
}
