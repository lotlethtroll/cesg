package com.cesg.storage.station;

import java.util.List;

import com.cesg.storage.ShulkerInventoryAccess;
import com.cesg.upgrades.EnhancedShulkerUpgradeTooltip;
import com.cesg.util.CESGLang;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class StationGoggleTooltip {
    private StationGoggleTooltip() {}

    /**
     * Shared station tooltip with a compact/detailed split: plain goggles show the essentials
     * (what's docked, power, ready-to-eject) plus a sneak hint; sneaking adds configuration,
     * upgrade, and hint lines. Keeps four near-identical station tooltips in one place.
     *
     * @param fullnessSuffix  ".load" / ".unload" — picked by station direction
     * @param thresholdKey    prefix-relative threshold line key (".threshold_load" / ".threshold_unload")
     */
    public static void appendStationTooltip(AbstractShulkerStationBlockEntity be, List<Component> tooltip,
            boolean sneaking, String prefix, String fullnessSuffix, String thresholdKey) {
        appendHeldHeader(be, tooltip, prefix, sneaking);

        boolean autoEject = be.getRetentionMode() == StationRetentionMode.AUTO_EJECT;
        if (autoEject && be.hasHeldShulker() && be.meetsEjectConditionPublic() && be.isPowered())
            CESGLang.forGoggles(tooltip, prefix + ".ready_to_eject", ChatFormatting.GREEN);

        if (!sneaking) {
            CESGLang.forGoggles(tooltip, "cesg.goggles.station.sneak_hint", ChatFormatting.DARK_GRAY);
            return;
        }

        CESGLang.forGoggles(tooltip, prefix + ".retention", ChatFormatting.WHITE,
                Component.translatable(be.getRetentionMode().getTranslationKey()));
        if (autoEject) {
            CESGLang.forGoggles(tooltip, prefix + ".eject_when", ChatFormatting.WHITE,
                    Component.translatable(be.getFullnessMode().getTranslationKey() + fullnessSuffix));
            if (be.getFullnessMode() == StationFullnessMode.SLOT_THRESHOLD)
                CESGLang.forGoggles(tooltip, prefix + thresholdKey, ChatFormatting.WHITE, be.getThreshold());
            CESGLang.forGoggles(tooltip, prefix + ".eject_funnel_hint", ChatFormatting.GRAY);
        }
    }

    private static void appendHeldHeader(AbstractShulkerStationBlockEntity be, List<Component> tooltip,
            String prefix, boolean sneaking) {
        if (be.getHeldShulker().isEmpty()) {
            CESGLang.forGoggles(tooltip, prefix + ".empty", ChatFormatting.GRAY);
            if (sneaking)
                CESGLang.forGoggles(tooltip, prefix + ".dock_via_funnel", ChatFormatting.DARK_GRAY);
        } else {
            int occupied = ShulkerInventoryAccess.countOccupiedSlots(be.getHeldShulker());
            int total = ShulkerInventoryAccess.getSlotCount(be.getHeldShulker());
            CESGLang.forGoggles(tooltip, prefix + ".contents", ChatFormatting.AQUA,
                    be.getHeldShulker().getHoverName().getString(), occupied, total);

            if (sneaking)
                appendHeldShulkerUpgrades(be.getHeldShulker(), tooltip);

            if (!be.isPowered())
                CESGLang.forGoggles(tooltip, prefix + ".unpowered", ChatFormatting.GRAY);
        }

        if (sneaking && !be.getStationName().isEmpty())
            CESGLang.forGoggles(tooltip, "cesg.goggles.station.names", ChatFormatting.WHITE, be.getStationName());
    }

    private static void appendHeldShulkerUpgrades(ItemStack shulker, List<Component> tooltip) {
        if (ShulkerInventoryAccess.getTier(shulker) < 2)
            return;
        EnhancedShulkerUpgradeTooltip.appendGoggleTooltip(shulker, tooltip);
    }
}
