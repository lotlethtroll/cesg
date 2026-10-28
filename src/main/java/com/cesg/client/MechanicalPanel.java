package com.cesg.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Tiled CESG material with fixed-width rails; no texture stretching at wider capacities. */
public final class MechanicalPanel {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "cesg", "textures/gui/mechanical_panel.png");
    private MechanicalPanel() {}

    public static void body(GuiGraphics g, int x, int y, int width, int height) {
        for (int dy = 0; dy < height; dy += 24)
            for (int dx = 0; dx < width; dx += 24)
                g.blit(TEXTURE, x+dx, y+dy, 4, 4, Math.min(24, width-dx), Math.min(24, height-dy), 32, 32);
    }

    public static void panel(GuiGraphics g, int x, int y, int width, int height) {
        body(g, x+4, y+4, width-8, height-8);
        for (int dx = 4; dx < width-4; dx += 24) {
            int length = Math.min(24, width-4-dx);
            g.blit(TEXTURE, x+dx, y, 4, 0, length, 4, 32, 32);
            g.blit(TEXTURE, x+dx, y+height-4, 4, 28, length, 4, 32, 32);
        }
        for (int dy = 4; dy < height-4; dy += 24) {
            int length = Math.min(24, height-4-dy);
            g.blit(TEXTURE, x, y+dy, 0, 4, 4, length, 32, 32);
            g.blit(TEXTURE, x+width-4, y+dy, 28, 4, 4, length, 32, 32);
        }
        for (int cx = 0; cx < 2; cx++)
            for (int cy = 0; cy < 2; cy++)
                g.blit(TEXTURE, x+cx*(width-4), y+cy*(height-4), cx*28, cy*28, 4, 4, 32, 32);
    }
}
