package com.cesg.client;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.cesg.network.StationConfigurationPacket;
import com.cesg.storage.station.AbstractShulkerStationBlockEntity;
import com.cesg.storage.station.StationFullnessMode;
import com.cesg.storage.station.StationRetentionMode;
import com.cesg.storage.beltloader.ShulkerBeltLoaderBlockEntity;
import com.cesg.storage.beltunloader.ShulkerBeltUnloaderBlockEntity;
import com.cesg.storage.unloader.ShulkerUnloaderBlockEntity;

import net.createmod.catnip.gui.AbstractSimiScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class ShulkerStationConfigScreen extends AbstractSimiScreen {
    private final AbstractShulkerStationBlockEntity blockEntity;
    private final boolean unloader;
    private final boolean beltLoader;
    private final boolean beltUnloader;

    // ---- Panel geometry ----
    private static final int PANEL_W = 224;
    private static final int TITLE_H = 24;
    private static final int FOOTER_H = 28;
    private static final int BODY_TOP_PAD = 8;
    private static final int BODY_BOTTOM_PAD = 6;

    // ---- Row layout ----
    private static final int BAR_X = 36;          // gray bar left edge within the panel
    private static final int BAR_W = 144;         // gray bar width
    private static final int BAR_H = 18;          // row/bar height
    private static final int ROW_SPACING = 24;    // distance between row tops
    private static final int ARROW_SIZE = 18;     // Directional button size
    private static final int ARROW_GAP = 3;

    private static final int BAR_FILL = 0xFF756180;
    private static final int BAR_BORDER = 0xFF555B50;
    private static final int TITLE_TEXT = 0x34392F;
    private static final int LABEL_TEXT = 0xFFE7D9B5;

    private ThresholdSlider thresholdSlider;
    private EditBox nameField;

    private List<Component> retentionOptions;
    private List<Component> fullnessOptions;

    private int retention;
    private int fullness;
    private int threshold;
    private String name;

    private boolean showFullnessRow;
    private boolean showThresholdRow;
    private int panelH;

    public ShulkerStationConfigScreen(AbstractShulkerStationBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
        this.beltUnloader = blockEntity instanceof ShulkerBeltUnloaderBlockEntity;
        this.unloader = beltUnloader || blockEntity instanceof ShulkerUnloaderBlockEntity;
        this.beltLoader = blockEntity instanceof ShulkerBeltLoaderBlockEntity;
        this.retention = blockEntity.getRetentionMode().ordinal();
        this.fullness = blockEntity.getFullnessMode().ordinal();
        this.threshold = blockEntity.getThreshold();
        this.name = blockEntity.getStationName();
    }

    /** The name row is always shown; retention is always shown; the rest depend on the mode. */
    private int visibleRowCount() {
        int n = 2;
        if (showFullnessRow) n++;
        if (showThresholdRow) n++;
        return n;
    }

    private void computePanelHeight() {
        int bodyH = BODY_TOP_PAD + visibleRowCount() * ROW_SPACING + BODY_BOTTOM_PAD;
        panelH = TITLE_H + bodyH + FOOTER_H;
    }

    /** Y of a row's TOP edge (bar top), absolute screen coords. */
    private int rowTop(int row) {
        return guiTop + TITLE_H + BODY_TOP_PAD + row * ROW_SPACING;
    }

    @Override
    protected void init() {
        retentionOptions = Arrays.stream(StationRetentionMode.values())
                .map(mode -> (Component) Component.translatable(mode.getTranslationKey()))
                .collect(Collectors.toList());
        fullnessOptions = Arrays.stream(StationFullnessMode.values())
                .map(mode -> (Component) Component.translatable(
                        mode.getTranslationKey() + (unloader ? ".unload" : ".load")))
                .collect(Collectors.toList());

        recomputeVisibleRows();
        computePanelHeight();
        setWindowSize(PANEL_W, panelH);
        super.init();
        clearWidgets();
        buildWidgets();
    }

    private void recomputeVisibleRows() {
        boolean autoEject = retention == StationRetentionMode.AUTO_EJECT.ordinal();
        showFullnessRow = autoEject;
        showThresholdRow = autoEject && fullness == StationFullnessMode.SLOT_THRESHOLD.ordinal();
    }

    private void buildWidgets() {
        clearWidgets();
        int x = guiLeft;
        int row = 0;

        int nameTop = rowTop(row++);
        nameField = new EditBox(font, x + BAR_X, nameTop + (BAR_H - 12) / 2, BAR_W, 12, Component.empty());
        nameField.setMaxLength(AbstractShulkerStationBlockEntity.MAX_NAME_LENGTH);
        nameField.setValue(name);
        nameField.setHint(Component.translatable("cesg.station.name.hint"));
        nameField.setResponder(value -> name = value);
        addRenderableWidget(nameField);

        addArrows(row++, () -> changeRetention(-1), () -> changeRetention(+1));

        if (showFullnessRow)
            addArrows(row++, () -> changeFullness(-1), () -> changeFullness(+1));

        if (showThresholdRow) {
            int barTop = rowTop(row);
            addArrows(row, () -> nudgeThreshold(-1), () -> nudgeThreshold(+1));
            thresholdSlider = new ThresholdSlider(x + BAR_X, barTop, BAR_W, BAR_H);
            thresholdSlider.syncFromThreshold();
            addRenderableWidget(thresholdSlider);
            row++;
        }

        addRenderableWidget(new MechanicalButton(x + PANEL_W - 56,
                guiTop + panelH - FOOTER_H + 4, 48, 18, Component.translatable("gui.done"),
                b -> onClose(), () -> false));
    }

    /** Places left/right arrows vertically centered on the bar midline. */
    private void addArrows(int row, Runnable onLeft, Runnable onRight) {
        int x = guiLeft;
        int barTop = rowTop(row);
        int arrowY = barTop + (BAR_H - ARROW_SIZE) / 2;

        addRenderableWidget(new MechanicalButton(x + BAR_X - ARROW_SIZE - ARROW_GAP,
                arrowY, ARROW_SIZE, ARROW_SIZE, Component.literal("<"), b -> onLeft.run(), () -> false));
        addRenderableWidget(new MechanicalButton(x + BAR_X + BAR_W + ARROW_GAP,
                arrowY, ARROW_SIZE, ARROW_SIZE, Component.literal(">"), b -> onRight.run(), () -> false));
    }

    private void changeRetention(int delta) {
        retention = Math.floorMod(retention + delta, StationRetentionMode.values().length);
        rebuildAfterStateChange();
    }

    private void changeFullness(int delta) {
        fullness = Math.floorMod(fullness + delta, StationFullnessMode.values().length);
        rebuildAfterStateChange();
    }

    private void rebuildAfterStateChange() {
        recomputeVisibleRows();
        computePanelHeight();
        setWindowSize(PANEL_W, panelH);
        super.init();         // re-centers guiLeft/guiTop for the new size
        clearWidgets();
        buildWidgets();
    }

    private void nudgeThreshold(int delta) {
        setThreshold(threshold + delta);
        if (thresholdSlider != null) thresholdSlider.syncFromThreshold();
    }

    private void setThreshold(int value) {
        threshold = Math.max(1, Math.min(AbstractShulkerStationBlockEntity.MAX_THRESHOLD, value));
    }

    private Component thresholdLabel(int value) {
        String key = unloader ? "cesg.station.threshold.value.unload" : "cesg.station.threshold.value.load";
        return Component.translatable(key, value);
    }

    @Override
    public void onClose() {
        super.onClose();
        PacketDistributor.sendToServer(
                new StationConfigurationPacket(blockEntity.getBlockPos(), retention, fullness, threshold, name));
    }

    @Override
    protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int x = guiLeft;
        int y = guiTop;

        drawPanel(graphics, x, y);

        String titleKey = beltUnloader ? "cesg.station.config.title.belt_unloader"
                : unloader ? "cesg.station.config.title.unloader"
                : beltLoader ? "cesg.station.config.title.belt_loader"
                : "cesg.station.config.title.loader";
        String header = font.plainSubstrByWidth(Component.translatable(titleKey).getString(), PANEL_W - 16);
        int stripTop = y + 8;
        int stripBottom = y + TITLE_H;
        int titleY = stripTop + (stripBottom - stripTop - font.lineHeight) / 2 + 1;
        graphics.drawString(font, header,
                x + PANEL_W / 2 - font.width(header) / 2, titleY, TITLE_TEXT, false);

        int row = 0;
        drawRowLabel(graphics, row++, Component.translatable("cesg.station.name.label"));
        // name field (EditBox) renders itself as a widget
        drawBar(graphics, row++, retentionOptions.get(retention));
        if (showFullnessRow)
            drawBar(graphics, row++, fullnessOptions.get(fullness));
        // threshold row's bar + label are drawn by the slider widget itself
        if (showThresholdRow) row++;
    }

    /** Right-aligned caption in the left gutter (used for rows whose body is a custom widget). */
    private void drawRowLabel(GuiGraphics g, int row, Component label) {
        int barY = rowTop(row);
        String text = font.plainSubstrByWidth(label.getString(), BAR_X - ARROW_GAP - 5);
        int textX = guiLeft + BAR_X - ARROW_GAP - font.width(text);
        int textY = barY + (BAR_H - font.lineHeight) / 2 + 1;
        g.drawString(font, text, textX, textY, TITLE_TEXT, false);
    }

    /** Tiled mechanical panel with matching brass title and steel footer. */
    private void drawPanel(GuiGraphics g, int x, int y) {
        MechanicalPanel.panel(g, x, y, PANEL_W, panelH);
        g.fill(x + 4, y + 4, x + PANEL_W - 4, y + TITLE_H, 0xFFA89B79);
        g.fill(x + 4, y + TITLE_H - 1, x + PANEL_W - 4, y + TITLE_H, 0xFF756B51);
        g.fill(x + 4, y + panelH - FOOTER_H, x + PANEL_W - 4, y + panelH - 4, 0xFF929582);
    }

    /** Identical bar for non-slider rows, centered label. */
    private void drawBar(GuiGraphics g, int row, Component label) {
        int barX = guiLeft + BAR_X;
        int barY = rowTop(row);
        g.fill(barX - 1, barY - 1, barX + BAR_W + 1, barY + BAR_H + 1, BAR_BORDER);
        g.fill(barX, barY, barX + BAR_W, barY + BAR_H, BAR_FILL);
        String text = font.plainSubstrByWidth(label.getString(), BAR_W - 6);
        int textX = barX + (BAR_W - font.width(text)) / 2;
        int textY = barY + (BAR_H - font.lineHeight) / 2 + 1;
        g.drawString(font, text, textX, textY, LABEL_TEXT, false);
    }

    /** Native keyboard/mouse slider with a matching metal track and brass handle. */
    private class ThresholdSlider extends AbstractSliderButton {
        ThresholdSlider(int x, int y, int w, int h) {
            super(x, y, w, h, Component.empty(), 0.0);
        }

        void syncFromThreshold() {
            int max = AbstractShulkerStationBlockEntity.MAX_THRESHOLD;
            this.value = (threshold - 1) / (double) (max - 1);
            updateMessage();
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            g.fill(x-1, y-1, x+w+1, y+h+1, isHoveredOrFocused() ? 0xFFE0C78D : BAR_BORDER);
            g.fill(x, y, x+w, y+h, BAR_FILL);
            int handle = x + (int) Math.round(value * (w-6));
            g.fill(handle, y, handle+6, y+h, 0xFFB39759);
            g.fill(handle+1, y+1, handle+5, y+2, 0xFFE0C78D);
            String text = font.plainSubstrByWidth(getMessage().getString(), w-6);
            g.drawString(font, text, x+(w-font.width(text))/2, y+(h-font.lineHeight)/2, LABEL_TEXT, true);
        }

        @Override
        protected void updateMessage() {
            setMessage(thresholdLabel(threshold));
        }

        @Override
        protected void applyValue() {
            int max = AbstractShulkerStationBlockEntity.MAX_THRESHOLD;
            setThreshold(1 + (int) Math.round(value * (max - 1)));
        }
    }
}
