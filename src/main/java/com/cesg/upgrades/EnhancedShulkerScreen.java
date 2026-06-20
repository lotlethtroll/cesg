package com.cesg.upgrades;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class EnhancedShulkerScreen extends AbstractContainerScreen<EnhancedShulkerMenu> {
    public EnhancedShulkerScreen(EnhancedShulkerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = 180;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xC0101010);
    }
}
