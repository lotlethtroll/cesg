package com.cesg.storage.station;

import java.util.List;

import com.cesg.storage.ShulkerInventoryAccess;
import com.cesg.util.CESGLang;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

public final class StationGoggleTooltip {
    private StationGoggleTooltip() {}

    public static void appendEjectFunnelTooltip(AbstractShulkerStationBlockEntity be, List<Component> tooltip,
            String prefix) {
        List<Direction> funnelSides = StationFunnelConnection.findOutputFunnels(be.getLevel(), be.getBlockPos());
        if (funnelSides.isEmpty()) {
            List<Direction> attached = StationFunnelConnection.findFunnelsFacingStation(be.getLevel(), be.getBlockPos());
            if (attached.isEmpty())
                CESGLang.forGoggles(tooltip, prefix + ".eject_funnel_hint", ChatFormatting.GRAY);
            else
                CESGLang.forGoggles(tooltip, prefix + ".funnel_wrong_mode_hint", ChatFormatting.YELLOW);
        } else {
            for (Direction funnelSide : funnelSides)
                CESGLang.forGoggles(tooltip, prefix + ".funnel_connected", ChatFormatting.WHITE,
                        Component.translatable(funnelSide.getSerializedName()));

            if (be.hasDockedShulker() && be.meetsEjectConditionPublic() && be.isPowered())
                CESGLang.forGoggles(tooltip, prefix + ".ready_to_eject", ChatFormatting.GREEN);
        }
    }

    public static void appendDockedHeader(AbstractShulkerStationBlockEntity be, List<Component> tooltip, String prefix) {
        if (be.getHeldShulker().isEmpty()) {
            CESGLang.forGoggles(tooltip, prefix + ".empty", ChatFormatting.GRAY);
            CESGLang.forGoggles(tooltip, prefix + ".dock_via_funnel", ChatFormatting.DARK_GRAY);
        } else {
            int occupied = ShulkerInventoryAccess.countOccupiedSlots(be.getHeldShulker());
            int total = ShulkerInventoryAccess.getSlotCount(be.getHeldShulker());
            CESGLang.forGoggles(tooltip, prefix + ".contents", ChatFormatting.AQUA,
                    be.getHeldShulker().getHoverName().getString(), occupied, total);

            if (!be.isPowered())
                CESGLang.forGoggles(tooltip, prefix + ".unpowered", ChatFormatting.GRAY);
        }
    }
}
