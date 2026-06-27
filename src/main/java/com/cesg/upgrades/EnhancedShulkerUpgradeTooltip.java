package com.cesg.upgrades;



import com.cesg.init.CESGDataComponents;
import com.cesg.util.CESGLang;


import net.minecraft.ChatFormatting;

import net.minecraft.core.NonNullList;

import net.minecraft.network.chat.Component;

import net.minecraft.world.item.ItemStack;

import net.minecraft.world.item.TooltipFlag;



import java.util.ArrayList;

import java.util.List;



public final class EnhancedShulkerUpgradeTooltip {

    private EnhancedShulkerUpgradeTooltip() {}



    public static List<ItemStack> getInstalledUpgrades(ItemStack shulker) {

        if (!shulker.has(CESGDataComponents.ENHANCED_SHULKER))

            return List.of();



        EnhancedShulkerContents contents = shulker.get(CESGDataComponents.ENHANCED_SHULKER.get());

        if (contents == null)

            return List.of();

        NonNullList<ItemStack> slots = NonNullList.withSize(contents.upgradeSlotCount(), ItemStack.EMPTY);

        contents.copyUpgradesInto(slots);



        List<ItemStack> installed = new ArrayList<>();

        for (ItemStack stack : slots) {

            if (!stack.isEmpty())

                installed.add(stack);

        }

        return installed;

    }



    /** Appends dynamic installed-upgrade lines when shift is held; static summary uses Create ItemDescription. */

    public static void appendItemTooltip(ItemStack shulker, List<Component> tooltip, TooltipFlag flag) {

        if (!shulker.has(CESGDataComponents.ENHANCED_SHULKER) || !flag.hasShiftDown())

            return;



        EnhancedShulkerContents contents = shulker.get(CESGDataComponents.ENHANCED_SHULKER.get());

        if (contents != null) {

            tooltip.add(Component.translatable("cesg.enhanced_shulker.tooltip.tier_slots",

                            contents.tier(), contents.slotCount(), contents.upgradeSlotCount())

                    .withStyle(ChatFormatting.GRAY));

        }



        List<ItemStack> upgrades = getInstalledUpgrades(shulker);

        if (upgrades.isEmpty()) {

            tooltip.add(Component.translatable("cesg.enhanced_shulker.tooltip.no_upgrades")

                    .withStyle(ChatFormatting.GRAY));

            return;

        }



        tooltip.add(Component.translatable("cesg.enhanced_shulker.tooltip.upgrades_header")

                .withStyle(ChatFormatting.GRAY));

        for (ItemStack upgrade : upgrades)

            tooltip.add(Component.translatable("cesg.enhanced_shulker.tooltip.upgrade_line", upgrade.getHoverName())

                    .withStyle(ChatFormatting.DARK_AQUA));

    }



    /** Number of stacks previewed before collapsing the rest into an "and N more..." line. */
    private static final int PREVIEW_MAX_LINES = 5;

    /**
     * Vanilla-style contents preview ("Cobblestone x64", then "and N more...") read from the enhanced
     * main inventory. Vanilla shulkers get this for free from the {@code minecraft:container} component;
     * enhanced shulkers store contents in {@link CESGDataComponents#ENHANCED_SHULKER} so we render it here.
     */
    public static void appendContentsPreview(ItemStack shulker, List<Component> tooltip) {
        if (!shulker.has(CESGDataComponents.ENHANCED_SHULKER))
            return;

        EnhancedShulkerContents contents = shulker.get(CESGDataComponents.ENHANCED_SHULKER.get());
        if (contents == null)
            return;

        NonNullList<ItemStack> slots = NonNullList.withSize(contents.slotCount(), ItemStack.EMPTY);
        contents.copyMainInto(slots);

        int shown = 0;
        int hidden = 0;
        for (ItemStack stack : slots) {
            if (stack.isEmpty())
                continue;
            if (shown < PREVIEW_MAX_LINES) {
                tooltip.add(Component.translatable("container.shulkerBox.itemCount",
                        stack.getHoverName(), stack.getCount()).withStyle(ChatFormatting.GRAY));
                shown++;
            } else {
                hidden++;
            }
        }

        if (hidden > 0)
            tooltip.add(Component.translatable("container.shulkerBox.more", hidden)
                    .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
    }

    public static void appendGoggleTooltip(ItemStack shulker, List<Component> tooltip) {
        if (!shulker.has(CESGDataComponents.ENHANCED_SHULKER))
            return;

        EnhancedShulkerContents contents = shulker.get(CESGDataComponents.ENHANCED_SHULKER.get());
        if (contents != null) {
            CESGLang.forGoggles(tooltip, "cesg.goggles.enhanced_shulker.tier_slots", ChatFormatting.WHITE,
                    contents.tier(), contents.slotCount(), contents.upgradeSlotCount());
        }

        NonNullList<ItemStack> upgradeSlots = upgradeSlotsFrom(shulker);
        List<ItemStack> upgrades = getInstalledUpgrades(shulker);
        if (upgrades.isEmpty()) {
            CESGLang.forGoggles(tooltip, "cesg.goggles.enhanced_shulker.no_upgrades", ChatFormatting.GRAY);
            return;
        }

        for (ItemStack upgrade : upgrades)
            CESGLang.forGoggles(tooltip, "cesg.goggles.enhanced_shulker.upgrade",
                    ChatFormatting.LIGHT_PURPLE, upgrade.getHoverName());

        if (ShulkerUpgradeItems.highestInstalledStackDepthTier(upgradeSlots) > 0)
            CESGLang.forGoggles(tooltip, "cesg.goggles.enhanced_shulker.stack_limit", ChatFormatting.WHITE,
                    ShulkerUpgradeItems.installedStackLimit(upgradeSlots));

        if (ShulkerStorageUpgrades.hasFilterUpgrade(upgradeSlots)) {
            ItemStack filter = contents == null ? ItemStack.EMPTY : contents.filterStack();
            if (filter.isEmpty())
                CESGLang.forGoggles(tooltip, "cesg.goggles.enhanced_shulker.filter_all", ChatFormatting.GRAY);
            else
                CESGLang.forGoggles(tooltip, "cesg.goggles.enhanced_shulker.filter_set", ChatFormatting.WHITE,
                        filter.getHoverName());
            CESGLang.forGoggles(tooltip, "cesg.goggles.enhanced_shulker.filter_reject_hint", ChatFormatting.DARK_GRAY);
        }

        if (ShulkerStorageUpgrades.hasCompactingUpgrade(upgradeSlots)) {
            CESGLang.forGoggles(tooltip, "cesg.goggles.enhanced_shulker.compacting", ChatFormatting.WHITE);
            if (contents != null) {
                NonNullList<ItemStack> main = NonNullList.withSize(contents.slotCount(), ItemStack.EMPTY);
                contents.copyMainInto(main);
                int stackLimit = ShulkerUpgradeItems.installedStackLimit(upgradeSlots);
                if (ShulkerStorageUpgrades.hasMergeablePartialStacks(main, stackLimit))
                    CESGLang.forGoggles(tooltip, "cesg.goggles.enhanced_shulker.compacting_backlog",
                            ChatFormatting.YELLOW);
            }
        }
    }

    private static NonNullList<ItemStack> upgradeSlotsFrom(ItemStack shulker) {
        EnhancedShulkerContents contents = shulker.get(CESGDataComponents.ENHANCED_SHULKER.get());
        if (contents == null)
            return NonNullList.create();
        NonNullList<ItemStack> slots = NonNullList.withSize(contents.upgradeSlotCount(), ItemStack.EMPTY);
        contents.copyUpgradesInto(slots);
        return slots;
    }
}

