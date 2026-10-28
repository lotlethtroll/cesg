package com.cesg.client;

import java.util.function.BooleanSupplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Standard narrated/focusable button with CESG's metal and brass treatment. */
final class MechanicalButton extends Button {
    private final BooleanSupplier selected;

    MechanicalButton(int x, int y, int width, int height, Component label, OnPress action,
            BooleanSupplier selected) {
        super(x, y, width, height, label, action, DEFAULT_NARRATION);
        this.selected = selected;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        int border = selected.getAsBoolean() ? 0xFFC8A65D
                : (isHoveredOrFocused() && active ? 0xFFE7D9B5 : 0xFF555B50);
        graphics.fill(x, y, x+w, y+h, border);
        graphics.fill(x+1, y+1, x+w-1, y+h-1,
                selected.getAsBoolean() ? 0xFF756180 : (active ? 0xFF747B6E : 0xFF929582));
        graphics.fill(x+1, y+1, x+w-1, y+2, 0xFFE7D9B5);
        graphics.fill(x+1, y+h-2, x+w-1, y+h-1, 0xFF424B45);
        var font = Minecraft.getInstance().font;
        int color = active || selected.getAsBoolean() ? 0xFFE7D9B5 : 0xFF555B50;
        if (font.width(getMessage()) <= w-6) {
            graphics.drawString(font, getMessage(), x+(w-font.width(getMessage()))/2, y+(h-8)/2, color, false);
        } else {
            String text = font.plainSubstrByWidth(getMessage().getString(), w-6);
            graphics.drawString(font, text, x+(w-font.width(text))/2, y+(h-8)/2, color, false);
        }
    }
}
