package com.cesg.storage.station;

import java.util.List;
import java.util.function.Predicate;

import com.cesg.storage.ShulkerInventoryAccess;
import com.cesg.upgrades.ShulkerStorageUpgrades;
import com.cesg.util.CESGLang;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Goggle lines for station and docked-shulker filters, including sneak-held-item reject hints. */
public final class FilterGoggleTooltip {
    private FilterGoggleTooltip() {}

    /**
     * @param insertPath {@code true} for loaders (station filter, then shulker filter);
     *                   {@code false} for unloaders (station filter on extracted items only)
     */
    public static void appendStationFilter(AbstractShulkerStationBlockEntity station, List<Component> tooltip,
            boolean isPlayerSneaking, boolean insertPath) {
        ItemStack stationFilter = station.filtering.getFilter();
        if (stationFilter.isEmpty())
            CESGLang.forGoggles(tooltip, "cesg.goggles.station.filter_all", ChatFormatting.WHITE);
        else
            CESGLang.forGoggles(tooltip, "cesg.goggles.station.filter_set", ChatFormatting.WHITE,
                    stationFilter.getHoverName());

        ItemStack held = station.getHeldShulker();
        if (!held.isEmpty() && ShulkerStorageUpgrades.heldShulkerHasFilterUpgrade(held))
            appendDockedShulkerFilter(tooltip, held);

        if (insertPath && !stationFilter.isEmpty() && ShulkerStorageUpgrades.heldShulkerHasFilterUpgrade(held))
            CESGLang.forGoggles(tooltip, "cesg.goggles.station.filter_chain_insert", ChatFormatting.DARK_GRAY);
        else if (insertPath && !stationFilter.isEmpty())
            CESGLang.forGoggles(tooltip, "cesg.goggles.station.filter_reject_insert_station", ChatFormatting.DARK_GRAY);
        else if (insertPath && ShulkerStorageUpgrades.heldShulkerHasFilterUpgrade(held))
            CESGLang.forGoggles(tooltip, "cesg.goggles.station.filter_reject_insert_shulker", ChatFormatting.DARK_GRAY);
        else if (!insertPath && !stationFilter.isEmpty())
            CESGLang.forGoggles(tooltip, "cesg.goggles.station.filter_reject_extract", ChatFormatting.DARK_GRAY);

        if (isPlayerSneaking)
            appendHeldItemProbe(station, tooltip, insertPath);

        CESGLang.forGoggles(tooltip, "cesg.goggles.station.filter_hint", ChatFormatting.DARK_GRAY);
    }

    private static void appendDockedShulkerFilter(List<Component> tooltip, ItemStack heldShulker) {
        ItemStack shulkerFilter = ShulkerStorageUpgrades.heldShulkerFilterStack(heldShulker);
        if (shulkerFilter.isEmpty())
            CESGLang.forGoggles(tooltip, "cesg.goggles.station.shulker_filter_all", ChatFormatting.GRAY);
        else
            CESGLang.forGoggles(tooltip, "cesg.goggles.station.shulker_filter_set", ChatFormatting.WHITE,
                    shulkerFilter.getHoverName());
    }

    private static void appendHeldItemProbe(AbstractShulkerStationBlockEntity station, List<Component> tooltip,
            boolean insertPath) {
        Minecraft client = Minecraft.getInstance();
        var player = client.player;
        if (player == null)
            return;

        ItemStack probe = player.getMainHandItem();
        if (probe.isEmpty() || ShulkerInventoryAccess.isShulkerBox(probe))
            return;

        Level level = station.getLevel();
        Predicate<ItemStack> stationFilter = stack -> station.filtering == null || station.filtering.test(stack);
        ShulkerStorageUpgrades.FilterLayer reject = insertPath
                ? ShulkerStorageUpgrades.findInsertRejectLayer(level, stationFilter, station.getHeldShulker(), probe)
                : ShulkerStorageUpgrades.findExtractRejectLayer(stationFilter, probe);

        if (reject == null) {
            CESGLang.forGoggles(tooltip, "cesg.goggles.station.filter_accepts_held", ChatFormatting.GREEN,
                    probe.getHoverName());
            return;
        }

        if (reject == ShulkerStorageUpgrades.FilterLayer.STATION)
            CESGLang.forGoggles(tooltip, "cesg.goggles.station.filter_rejects_held_station", ChatFormatting.YELLOW,
                    probe.getHoverName());
        else
            CESGLang.forGoggles(tooltip, "cesg.goggles.station.filter_rejects_held_shulker", ChatFormatting.YELLOW,
                    probe.getHoverName());
    }
}
