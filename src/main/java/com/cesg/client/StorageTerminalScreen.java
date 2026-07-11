package com.cesg.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.cesg.network.TerminalActionPacket;
import com.cesg.network.TerminalContentPacket;
import com.cesg.storage.network.StorageTerminalMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Storage terminal UI (Phase 6D): searchable aggregated network view plus a 3×3 crafting grid.
 * Shift-click storage withdraws to inventory; shift-click the result batch-crafts; the × button
 * clears the crafting grid back into the network.
 */
@OnlyIn(Dist.CLIENT)
public class StorageTerminalScreen extends AbstractContainerScreen<StorageTerminalMenu> {
    private static final int COLS = 9;
    private static final int ROWS = 6;
    private static final int GRID_X = 8;
    private static final int GRID_Y = 30;
    private static final int SEARCH_Y = 16;
    private static final int SEARCH_WIDTH = StorageTerminalMenu.MAIN_PANEL_WIDTH - GRID_X * 2;
    private static final int CELL = 18;

    private static final int PANEL_COLOR = 0xFFC6C6C6;
    private static final int HIGHLIGHT_COLOR = 0xFFFFFFFF;
    private static final int SHADOW_COLOR = 0xFF555555;
    private static final int SLOT_BG = 0xFF8B8B8B;
    private static final int SLOT_SHADOW = 0xFF373737;
    private static final int SLOT_HIGHLIGHT = 0xFFFFFFFF;
    private static final int LABEL_COLOR = 0x404040;
    private static final int CLEAR_NORMAL = 0xFFAA3333;
    private static final int CLEAR_HOVER = 0xFFFF5555;

    private List<TerminalContentPacket.Entry> entries = List.of();
    private List<TerminalContentPacket.Entry> filtered = List.of();
    private EditBox searchBox;
    private int scrollRow;

    public StorageTerminalScreen(StorageTerminalMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = StorageTerminalMenu.IMAGE_WIDTH;
        imageHeight = 234;
        titleLabelX = GRID_X;
        inventoryLabelY = StorageTerminalMenu.PLAYER_INV_Y - 11;
        titleLabelY = 5;
    }

    @Override
    protected void init() {
        super.init();
        searchBox = new EditBox(font, leftPos + GRID_X, topPos + SEARCH_Y, SEARCH_WIDTH, 12,
                Component.translatable("cesg.network.search"));
        searchBox.setBordered(true);
        searchBox.setResponder(text -> refilter());
        addRenderableWidget(searchBox);
    }

    public static void acceptContent(TerminalContentPacket packet) {
        if (Minecraft.getInstance().screen instanceof StorageTerminalScreen screen
                && screen.menu.containerId == packet.containerId())
            screen.updateEntries(packet.entries());
    }

    private void updateEntries(List<TerminalContentPacket.Entry> entries) {
        List<TerminalContentPacket.Entry> sorted = new ArrayList<>(entries);
        sorted.sort((a, b) -> {
            int byCount = Integer.compare(b.total(), a.total());
            if (byCount != 0)
                return byCount;
            return a.sample().getHoverName().getString()
                    .compareToIgnoreCase(b.sample().getHoverName().getString());
        });
        this.entries = sorted;
        refilter();
    }

    private void refilter() {
        String query = searchBox == null ? "" : searchBox.getValue().toLowerCase(Locale.ROOT).trim();
        if (query.isEmpty()) {
            filtered = entries;
        } else {
            List<TerminalContentPacket.Entry> match = new ArrayList<>();
            for (TerminalContentPacket.Entry entry : entries)
                if (entry.sample().getHoverName().getString().toLowerCase(Locale.ROOT).contains(query))
                    match.add(entry);
            filtered = match;
        }
        scrollRow = Math.max(0, Math.min(scrollRow, maxScrollRow()));
    }

    private int maxScrollRow() {
        int totalRows = (filtered.size() + COLS - 1) / COLS;
        return Math.max(0, totalRows - ROWS);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x0 = leftPos;
        int y0 = topPos;
        int mainW = StorageTerminalMenu.MAIN_PANEL_WIDTH;
        int notchW = imageWidth - mainW;
        int notchH = StorageTerminalMenu.CRAFT_NOTCH_HEIGHT;

        drawOutline(graphics, x0, y0, mainW, imageHeight, notchW, notchH);
        drawMainPanel(graphics, x0, y0, mainW, imageHeight, notchH);
        drawCraftNotch(graphics, x0 + mainW, y0, notchW, notchH);

        for (int row = 0; row < ROWS; row++)
            for (int col = 0; col < COLS; col++)
                drawSlotBox(graphics, x0 + GRID_X + col * CELL, y0 + GRID_Y + row * CELL);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 3; col++)
                drawSlotBox(graphics, x0 + StorageTerminalMenu.CRAFT_GRID_X - 1 + col * CELL,
                        y0 + StorageTerminalMenu.CRAFT_GRID_Y - 1 + row * CELL);
        drawSlotBox(graphics, x0 + StorageTerminalMenu.RESULT_X - 1, y0 + StorageTerminalMenu.RESULT_Y - 1);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < COLS; col++)
                drawSlotBox(graphics, x0 + StorageTerminalMenu.PLAYER_INV_X - 1 + col * CELL,
                        y0 + StorageTerminalMenu.PLAYER_INV_Y - 1 + row * CELL);
        for (int col = 0; col < COLS; col++)
            drawSlotBox(graphics, x0 + StorageTerminalMenu.PLAYER_INV_X - 1 + col * CELL,
                    y0 + StorageTerminalMenu.HOTBAR_Y - 1);
    }

    /** L-shaped outer border: right edge only as tall as the crafting notch. */
    private static void drawOutline(GuiGraphics graphics, int x, int y, int mainW, int mainH,
            int notchW, int notchH) {
        int totalW = mainW + notchW;
        graphics.fill(x - 1, y - 1, x + totalW + 1, y, SHADOW_COLOR);
        graphics.fill(x - 1, y, x, y + mainH + 1, SHADOW_COLOR);
        graphics.fill(x - 1, y + mainH, x + totalW + 1, y + mainH + 1, SHADOW_COLOR);
        graphics.fill(x + totalW, y, x + totalW + 1, y + notchH, SHADOW_COLOR);
    }

    /** Main terminal body — right edge appears only below the crafting notch. */
    private static void drawMainPanel(GuiGraphics graphics, int x, int y, int width, int height,
            int notchHeight) {
        graphics.fill(x, y, x + width, y + height, PANEL_COLOR);
        graphics.fill(x, y, x + width, y + 1, HIGHLIGHT_COLOR);
        graphics.fill(x, y, x + 1, y + height, HIGHLIGHT_COLOR);
        graphics.fill(x + width - 1, y + notchHeight, x + width, y + height, SHADOW_COLOR);
        graphics.fill(x, y + height - 1, x + width, y + height, SHADOW_COLOR);
    }

    /** Short crafting extension on the right — open on the left, no full-height column. */
    private static void drawCraftNotch(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, PANEL_COLOR);
        graphics.fill(x, y, x + width, y + 1, HIGHLIGHT_COLOR);
        graphics.fill(x + width - 1, y, x + width, y + height, SHADOW_COLOR);
        graphics.fill(x, y + height - 1, x + width, y + height, SHADOW_COLOR);
    }

    private static void drawSlotBox(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + CELL, y + CELL, SLOT_SHADOW);
        graphics.fill(x + 1, y + 1, x + CELL, y + CELL, SLOT_HIGHLIGHT);
        graphics.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, SLOT_BG);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int hovered = hoveredEntry(mouseX, mouseY);
        if (hovered >= 0 && menu.getCarried().isEmpty()) {
            TerminalContentPacket.Entry entry = filtered.get(hovered);
            List<Component> tooltip = new ArrayList<>(getTooltipFromItem(Minecraft.getInstance(), entry.sample()));
            tooltip.add(Component.translatable("cesg.network.count", entry.total())
                    .withStyle(ChatFormatting.GRAY));
            graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
        } else if (isClearCraftHovered(mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("cesg.network.clear_craft"), mouseX, mouseY);
        }

        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, LABEL_COLOR, false);
        graphics.drawString(font, Component.translatable("cesg.network.crafting"),
                StorageTerminalMenu.CRAFT_GRID_X, 6, LABEL_COLOR, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LABEL_COLOR, false);

        int clearColor = isClearCraftHovered(mouseX, mouseY) ? CLEAR_HOVER : CLEAR_NORMAL;
        drawThickClearX(graphics, StorageTerminalMenu.CLEAR_CRAFT_X, StorageTerminalMenu.CLEAR_CRAFT_Y, clearColor);

        for (int cell = 0; cell < ROWS * COLS; cell++) {
            int index = (scrollRow + cell / COLS) * COLS + cell % COLS;
            if (index >= filtered.size())
                break;
            TerminalContentPacket.Entry entry = filtered.get(index);
            int x = GRID_X + (cell % COLS) * CELL + 1;
            int y = GRID_Y + (cell / COLS) * CELL + 1;
            graphics.renderItem(entry.sample(), x, y);
            drawCount(graphics, x, y, entry.total());
        }

        if (isInGrid(mouseX, mouseY)) {
            int gx = GRID_X + ((mouseX - leftPos - GRID_X) / CELL) * CELL + 1;
            int gy = GRID_Y + ((mouseY - topPos - GRID_Y) / CELL) * CELL + 1;
            graphics.fillGradient(net.minecraft.client.renderer.RenderType.guiOverlay(),
                    gx, gy, gx + 16, gy + 16, 0x80FFFFFF, 0x80FFFFFF, 0);
        }
    }

    /** Symmetric 2px-thick × above the left edge of the crafting grid. */
    private static void drawThickClearX(GuiGraphics graphics, int x, int y, int color) {
        int size = 8;
        int thick = 2;
        for (int i = 0; i < size; i++) {
            graphics.fill(x + i, y + i, x + i + thick, y + i + thick, color);
            int px = x + size - 1 - i;
            graphics.fill(px, y + i, px + thick, y + i + thick, color);
        }
    }

    private void drawCount(GuiGraphics graphics, int x, int y, int total) {
        if (total <= 1)
            return;
        String text = shorten(total);
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(0, 0, 200);
        pose.pushPose();
        pose.scale(0.5f, 0.5f, 1);
        graphics.drawString(font, text,
                (x + 17) * 2 - font.width(text), (y + 13) * 2, 0xFFFFFF, true);
        pose.popPose();
        pose.popPose();
    }

    private static String shorten(int total) {
        if (total >= 1_000_000)
            return (total / 100_000) / 10.0 + "M";
        if (total >= 10_000)
            return total / 1000 + "K";
        if (total >= 1000)
            return (total / 100) / 10.0 + "K";
        return String.valueOf(total);
    }

    private int hoveredEntry(double mouseX, double mouseY) {
        int gx = (int) mouseX - leftPos - GRID_X;
        int gy = (int) mouseY - topPos - GRID_Y;
        if (gx < 0 || gy < 0 || gx >= COLS * CELL || gy >= ROWS * CELL)
            return -1;
        int index = (scrollRow + gy / CELL) * COLS + gx / CELL;
        return index < filtered.size() ? index : -1;
    }

    private boolean isInGrid(double mouseX, double mouseY) {
        double gx = mouseX - leftPos - GRID_X;
        double gy = mouseY - topPos - GRID_Y;
        return gx >= 0 && gy >= 0 && gx < COLS * CELL && gy < ROWS * CELL;
    }

    private boolean isClearCraftHovered(double mouseX, double mouseY) {
        double x = mouseX - leftPos - StorageTerminalMenu.CLEAR_CRAFT_X;
        double y = mouseY - topPos - StorageTerminalMenu.CLEAR_CRAFT_Y;
        int size = StorageTerminalMenu.CLEAR_CRAFT_SIZE;
        return x >= 0 && y >= 0 && x < size && y < size;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isClearCraftHovered(mouseX, mouseY)) {
            PacketDistributor.sendToServer(new TerminalActionPacket(menu.containerId,
                    TerminalActionPacket.CLEAR_CRAFT, ItemStack.EMPTY, 0));
            return true;
        }
        if (isInGrid(mouseX, mouseY)) {
            ItemStack carried = menu.getCarried();
            if (!carried.isEmpty()) {
                int amount = button == 1 ? 1 : carried.getCount();
                PacketDistributor.sendToServer(new TerminalActionPacket(menu.containerId,
                        TerminalActionPacket.DEPOSIT, ItemStack.EMPTY, amount));
                return true;
            }
            int hovered = hoveredEntry(mouseX, mouseY);
            if (hovered >= 0) {
                TerminalContentPacket.Entry entry = filtered.get(hovered);
                ItemStack sample = entry.sample().copyWithCount(1);
                int mode = hasShiftDown() && button == 0
                        ? TerminalActionPacket.WITHDRAW_TO_INVENTORY
                        : TerminalActionPacket.WITHDRAW_TO_CURSOR;
                int count = button == 1 ? 1 : Math.min(entry.total(), entry.sample().getMaxStackSize());
                PacketDistributor.sendToServer(
                        new TerminalActionPacket(menu.containerId, mode, sample, count));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (hoveredEntry(mouseX, mouseY) >= 0 || maxScrollRow() > 0) {
            scrollRow = Math.max(0, Math.min(scrollRow - (int) Math.signum(scrollY), maxScrollRow()));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBox.isFocused() && searchBox.keyPressed(keyCode, scanCode, modifiers))
            return true;
        if (searchBox.isFocused() && keyCode != 256 /* ESC */)
            return searchBox.canConsumeInput() || super.keyPressed(keyCode, scanCode, modifiers);
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
