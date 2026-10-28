package com.cesg.client;

import java.util.List;

import com.cesg.gateways.GatewayFilterMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Editor for one gateway channel's routing filter (Phase 7B): a row of ghost-filter slots and a
 * whitelist/blacklist toggle. Slot items render through the container screen; this draws the textured
 * panel, slot boxes, the mode button, and labels.
 */
@OnlyIn(Dist.CLIENT)
public class GatewayFilterScreen extends AbstractContainerScreen<GatewayFilterMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            "cesg", "textures/gui/gateway_filter.png");
    private static final ResourceLocation SLOT_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "cesg", "textures/gui/slot.png");
    private static final int HIGHLIGHT_COLOR = 0xFFE7D9B5;
    private static final int SHADOW_COLOR = 0xFF555B50;
    private static final int LABEL_COLOR = 0x34392F;
    private static final int WL_FILL = 0xFF756180;
    private static final int BL_FILL = 0xFF925A4C;
    private static final int BTN_TEXT = 0xFFE7D9B5;

    private static final int MODE_Y = 18;
    private static final int MODE_W = 76;
    private static final int MODE_H = 14;

    public GatewayFilterScreen(GatewayFilterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = GatewayFilterMenu.PANEL_W;
        imageHeight = GatewayFilterMenu.IMAGE_H;
        titleLabelX = 8;
        titleLabelY = 6;
        inventoryLabelX = 8;
        inventoryLabelY = GatewayFilterMenu.PLAYER_INV_Y - 11;
    }

    private int modeX() {
        return (imageWidth - MODE_W) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x0 = leftPos;
        int y0 = topPos;
        graphics.blit(BACKGROUND, x0, y0, 0, 0, imageWidth, imageHeight, 176, 156);

        for (int col = 0; col < GatewayFilterMenu.FILTER_COLS; col++)
            drawSlotBox(graphics, x0 + GatewayFilterMenu.FILTER_X + col * GatewayFilterMenu.SLOT,
                    y0 + GatewayFilterMenu.FILTER_Y);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                drawSlotBox(graphics, x0 + GatewayFilterMenu.FILTER_X + col * GatewayFilterMenu.SLOT,
                        y0 + GatewayFilterMenu.PLAYER_INV_Y - 1 + row * GatewayFilterMenu.SLOT);
        for (int col = 0; col < 9; col++)
            drawSlotBox(graphics, x0 + GatewayFilterMenu.FILTER_X + col * GatewayFilterMenu.SLOT,
                    y0 + GatewayFilterMenu.HOTBAR_Y - 1);

        boolean bl = menu.isBlacklist();
        int px = leftPos + modeX();
        int py = topPos + MODE_Y;
        boolean hovered = isModeHovered(mouseX, mouseY);
        graphics.fill(px - 1, py - 1, px + MODE_W + 1, py + MODE_H + 1, hovered ? HIGHLIGHT_COLOR : SHADOW_COLOR);
        graphics.fill(px, py, px + MODE_W, py + MODE_H, bl ? BL_FILL : WL_FILL);
        graphics.fill(px, py, px + MODE_W, py + 1, HIGHLIGHT_COLOR);
        graphics.fill(px, py + MODE_H - 1, px + MODE_W, py + MODE_H, SHADOW_COLOR);
        Component label = Component.translatable(bl ? "cesg.gateway.filter.blacklist" : "cesg.gateway.filter.whitelist");
        String text = font.plainSubstrByWidth(label.getString(), MODE_W - 4);
        graphics.drawString(font, text, px + (MODE_W - font.width(text)) / 2, py + 3, BTN_TEXT, true);
    }

    private static void drawSlotBox(GuiGraphics graphics, int x, int y) {
        int cell = GatewayFilterMenu.SLOT;
        graphics.blit(SLOT_TEXTURE, x, y, 0, 0, cell, cell, cell, cell);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        Component header = Component.translatable("cesg.gateway.filter.title", menu.channel() + 1);
        graphics.drawString(font, font.plainSubstrByWidth(header.getString(), imageWidth - 16), titleLabelX, titleLabelY, LABEL_COLOR, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LABEL_COLOR, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (isModeHovered(mouseX, mouseY))
            graphics.renderTooltip(font, Component.translatable(menu.isBlacklist()
                    ? "cesg.gateway.filter.blacklist.tip" : "cesg.gateway.filter.whitelist.tip"), mouseX, mouseY);
        else if (hoveredSlot != null && hoveredSlot.index < GatewayFilterMenu.FILTER_COLS && !hoveredSlot.hasItem())
            graphics.renderComponentTooltip(font,
                    List.of(Component.translatable("cesg.gateway.filter.hint")), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isModeHovered(mouseX, mouseY)) {
            if (minecraft != null && minecraft.gameMode != null)
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, GatewayFilterMenu.BTN_BLACKLIST);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isModeHovered(double mouseX, double mouseY) {
        double rx = mouseX - leftPos - modeX();
        double ry = mouseY - topPos - MODE_Y;
        return rx >= 0 && ry >= 0 && rx < MODE_W && ry < MODE_H;
    }
}
