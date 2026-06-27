package com.cesg.farming;

import java.util.List;

import com.cesg.util.CESGLang;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

public final class ShulkerCageGoggleTooltip {
    private ShulkerCageGoggleTooltip() {}

    public static void append(ShulkerCageBlockEntity cage, List<Component> tooltip) {
        Level level = cage.getLevel();
        if (level != null && !level.dimension().equals(Level.END))
            CESGLang.forGoggles(tooltip, "cesg.goggles.shulker_cage.end_only", ChatFormatting.YELLOW);

        if (!cage.hasTrappedShulker()) {
            CESGLang.forGoggles(tooltip, "cesg.goggles.shulker_cage.empty", ChatFormatting.GRAY);
            CESGLang.forGoggles(tooltip, "cesg.goggles.shulker_cage.trap_hint", ChatFormatting.DARK_GRAY);
            return;
        }

        CESGLang.forGoggles(tooltip, "cesg.goggles.shulker_cage.held", ChatFormatting.AQUA,
                cage.trappedDisplayName());

        if (cage.getCooldown() > 0)
            CESGLang.forGoggles(tooltip, "cesg.goggles.shulker_cage.cooldown", ChatFormatting.GRAY,
                    ticksToSeconds(cage.getCooldown()));
        else if (level != null && level.dimension().equals(Level.END))
            CESGLang.forGoggles(tooltip, "cesg.goggles.shulker_cage.ready", ChatFormatting.GREEN);
    }

    private static int ticksToSeconds(int ticks) {
        return Math.max(1, (ticks + 19) / 20);
    }
}
