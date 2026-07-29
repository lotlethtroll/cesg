package com.cesg.client;

import java.util.List;

import com.cesg.gateways.StorageBridgeMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Config screen for the Storage Bridge's passive auto-transfer (Phase 7A/D3). Two ghost-filter rows
 * (push local→partner, pull partner→local), each with an enable toggle and a whitelist/blacklist mode
 * toggle. Slot items and their tooltips render through the container screen; this class draws the flat
 * panel, slot boxes, section headers, and the four toggle buttons.
 */
@OnlyIn(Dist.CLIENT)
public class StorageBridgeScreen extends AbstractContainerScreen<StorageBridgeMenu> {
    private static final int PANEL_COLOR = 0xFFC6C6C6;
    private static final int HIGHLIGHT_COLOR = 0xFFFFFFFF;
    private static final int SHADOW_COLOR = 0xFF555555;
    private static final int SLOT_BG = 0xFF8B8B8B;
    private static final int SLOT_SHADOW = 0xFF373737;
    private static final int SLOT_HIGHLIGHT = 0xFFFFFFFF;
    private static final int LABEL_COLOR = 0x404040;
    private static final int DISABLED_LABEL = 0x808080;
    private static final int DISABLED_VEIL = 0xA0C6C6C6;

    private static final int ON_FILL = 0xFF4C9A4C;
    private static final int OFF_FILL = 0xFF8B8B8B;
    private static final int WL_FILL = 0xFF4C7AC0;
    private static final int BL_FILL = 0xFFC05050;
    private static final int BTN_TEXT = 0xFFFFFFFF;

    private static final int PUSH_HEADER_Y = 20;
    private static final int PULL_HEADER_Y = 62;
    private static final int EN_X = 90;
    private static final int MODE_X = 130;
    private static final int BTN_W = 38;
    private static final int BTN_H = 12;

    public StorageBridgeScreen(StorageBridgeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = StorageBridgeMenu.PANEL_W;
        imageHeight = StorageBridgeMenu.IMAGE_H;
        titleLabelX = 8;
        titleLabelY = 6;
        inventoryLabelX = 8;
        inventoryLabelY = StorageBridgeMenu.PLAYER_INV_Y - 11;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x0 = leftPos;
        int y0 = topPos;
        graphics.fill(x0 - 1, y0 - 1, x0 + imageWidth + 1, y0 + imageHeight + 1, SHADOW_COLOR);
        graphics.fill(x0, y0, x0 + imageWidth, y0 + imageHeight, PANEL_COLOR);
        graphics.fill(x0, y0, x0 + imageWidth, y0 + 1, HIGHLIGHT_COLOR);
        graphics.fill(x0, y0, x0 + 1, y0 + imageHeight, HIGHLIGHT_COLOR);

        drawFilterRow(graphics, x0 + StorageBridgeMenu.SEND_X, y0 + StorageBridgeMenu.SEND_Y);
        drawFilterRow(graphics, x0 + StorageBridgeMenu.PULL_X, y0 + StorageBridgeMenu.PULL_Y);
        // Route mode bypasses the send filter, so veil it rather than leaving a live-looking control.
        if (menu.isRouteMode()) {
            int rx = x0 + StorageBridgeMenu.SEND_X;
            int ry = y0 + StorageBridgeMenu.SEND_Y;
            graphics.fill(rx, ry, rx + StorageBridgeMenu.FILTER_COLS * StorageBridgeMenu.SLOT,
                    ry + StorageBridgeMenu.SLOT, DISABLED_VEIL);
        }

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                drawSlotBox(graphics, x0 + StorageBridgeMenu.SEND_X + col * StorageBridgeMenu.SLOT,
                        y0 + StorageBridgeMenu.PLAYER_INV_Y - 1 + row * StorageBridgeMenu.SLOT);
        for (int col = 0; col < 9; col++)
            drawSlotBox(graphics, x0 + StorageBridgeMenu.SEND_X + col * StorageBridgeMenu.SLOT,
                    y0 + StorageBridgeMenu.HOTBAR_Y - 1);

        drawToggle(graphics, EN_X, PUSH_HEADER_Y, menu.isPushEnabled() ? ON_FILL : OFF_FILL,
                enableLabel(menu.isPushEnabled()), isBtnHovered(mouseX, mouseY, EN_X, PUSH_HEADER_Y));
        drawToggle(graphics, MODE_X, PUSH_HEADER_Y, menu.isSendBlacklist() ? BL_FILL : WL_FILL,
                modeLabel(menu.isSendBlacklist()), isBtnHovered(mouseX, mouseY, MODE_X, PUSH_HEADER_Y));
        drawToggle(graphics, EN_X, PULL_HEADER_Y, menu.isPullEnabled() ? ON_FILL : OFF_FILL,
                enableLabel(menu.isPullEnabled()), isBtnHovered(mouseX, mouseY, EN_X, PULL_HEADER_Y));
        drawToggle(graphics, MODE_X, PULL_HEADER_Y, menu.isPullBlacklist() ? BL_FILL : WL_FILL,
                modeLabel(menu.isPullBlacklist()), isBtnHovered(mouseX, mouseY, MODE_X, PULL_HEADER_Y));
    }

    private void drawFilterRow(GuiGraphics graphics, int x, int y) {
        for (int col = 0; col < StorageBridgeMenu.FILTER_COLS; col++)
            drawSlotBox(graphics, x + col * StorageBridgeMenu.SLOT, y);
    }

    private static void drawSlotBox(GuiGraphics graphics, int x, int y) {
        int cell = StorageBridgeMenu.SLOT;
        graphics.fill(x, y, x + cell, y + cell, SLOT_SHADOW);
        graphics.fill(x + 1, y + 1, x + cell, y + cell, SLOT_HIGHLIGHT);
        graphics.fill(x + 1, y + 1, x + cell - 1, y + cell - 1, SLOT_BG);
    }

    private void drawToggle(GuiGraphics graphics, int x, int y, int fill, Component label, boolean hovered) {
        int px = leftPos + x;
        int py = topPos + y;
        graphics.fill(px - 1, py - 1, px + BTN_W + 1, py + BTN_H + 1, hovered ? HIGHLIGHT_COLOR : SHADOW_COLOR);
        graphics.fill(px, py, px + BTN_W, py + BTN_H, fill);
        int tx = px + (BTN_W - font.width(label)) / 2;
        graphics.drawString(font, label, tx, py + 2, BTN_TEXT, true);
    }

    private static Component enableLabel(boolean on) {
        return Component.translatable(on ? "cesg.bridge.on" : "cesg.bridge.off");
    }

    private static Component modeLabel(boolean blacklist) {
        return Component.translatable(blacklist ? "cesg.bridge.blacklist_short" : "cesg.bridge.whitelist_short");
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, LABEL_COLOR, false);
        // Name where items actually go. "Push → Partner" never said which partner, and in route mode
        // the answer is "whichever channel filter accepts it" — not this row's filter at all.
        boolean routed = menu.isRouteMode();
        Component push = routed
                ? Component.translatable("cesg.bridge.push.routed")
                : Component.translatable("cesg.bridge.push.to", menu.destinationLabel());
        graphics.drawString(font, push, 8, PUSH_HEADER_Y + 2, routed ? DISABLED_LABEL : LABEL_COLOR, false);
        graphics.drawString(font, Component.translatable("cesg.bridge.pull.from", menu.destinationLabel()),
                8, PULL_HEADER_Y + 2, LABEL_COLOR, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LABEL_COLOR, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderButtonTooltip(graphics, mouseX, mouseY);
        renderGhostHint(graphics, mouseX, mouseY);
    }

    private void renderButtonTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        Component tip = null;
        if (isBtnHovered(mouseX, mouseY, EN_X, PUSH_HEADER_Y) || isBtnHovered(mouseX, mouseY, EN_X, PULL_HEADER_Y))
            tip = Component.translatable("cesg.bridge.enable.tip");
        else if (isBtnHovered(mouseX, mouseY, MODE_X, PUSH_HEADER_Y))
            tip = Component.translatable(menu.isSendBlacklist() ? "cesg.bridge.blacklist.tip" : "cesg.bridge.whitelist.tip");
        else if (isBtnHovered(mouseX, mouseY, MODE_X, PULL_HEADER_Y))
            tip = Component.translatable(menu.isPullBlacklist() ? "cesg.bridge.blacklist.tip" : "cesg.bridge.whitelist.tip");
        if (tip != null)
            graphics.renderTooltip(font, tip, mouseX, mouseY);
    }

    /** Empty ghost slots have no item tooltip of their own — hint how to configure them. */
    private void renderGhostHint(GuiGraphics graphics, int mouseX, int mouseY) {
        if (hoveredSlot == null || hoveredSlot.index >= 2 * StorageBridgeMenu.FILTER_COLS)
            return;
        // The send row is inert in route mode, so say so on hover instead of the "click to filter" hint.
        if (hoveredSlot.index < StorageBridgeMenu.FILTER_COLS && menu.isRouteMode()) {
            graphics.renderComponentTooltip(font,
                    List.of(Component.translatable("cesg.bridge.push.routed.tip")), mouseX, mouseY);
            return;
        }
        if (!hoveredSlot.hasItem())
            graphics.renderComponentTooltip(font,
                    List.of(Component.translatable("cesg.bridge.filter.hint")), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int id = clickedButton(mouseX, mouseY);
            if (id >= 0) {
                if (minecraft != null && minecraft.gameMode != null)
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int clickedButton(double mouseX, double mouseY) {
        if (isBtnHovered(mouseX, mouseY, EN_X, PUSH_HEADER_Y))
            return StorageBridgeMenu.BTN_PUSH;
        if (isBtnHovered(mouseX, mouseY, MODE_X, PUSH_HEADER_Y))
            return StorageBridgeMenu.BTN_SEND_BLACKLIST;
        if (isBtnHovered(mouseX, mouseY, EN_X, PULL_HEADER_Y))
            return StorageBridgeMenu.BTN_PULL;
        if (isBtnHovered(mouseX, mouseY, MODE_X, PULL_HEADER_Y))
            return StorageBridgeMenu.BTN_PULL_BLACKLIST;
        return -1;
    }

    private boolean isBtnHovered(double mouseX, double mouseY, int x, int y) {
        double rx = mouseX - leftPos - x;
        double ry = mouseY - topPos - y;
        return rx >= 0 && ry >= 0 && rx < BTN_W && ry < BTN_H;
    }
}
