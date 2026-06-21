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
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;

import net.createmod.catnip.gui.AbstractSimiScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.network.PacketDistributor;

public class ShulkerStationConfigScreen extends AbstractSimiScreen {
    private final AbstractShulkerStationBlockEntity blockEntity;
    private final boolean unloader;
    private final boolean beltLoader;
    private final boolean beltUnloader;

    // ---- Panel geometry (flat-drawn, brown/brass) ----
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
    private static final int ARROW_SIZE = 18;     // IconButton visual size (Create arrows ~18px)
    private static final int ARROW_GAP = 3;

    // ---- Brown/brass palette ----
    private static final int BORDER_DARK = 0xFF3A2A1C;
    private static final int BORDER_MID  = 0xFF5A4632;
    private static final int PANEL_FILL  = 0xFF6B5440;
    private static final int TITLE_FILL  = 0xFFCAA85A;
    private static final int FOOTER_FILL = 0xFF4A3826;
    private static final int BAR_FILL    = 0xFF8A8A8A;
    private static final int BAR_BORDER  = 0xFF5A5A5A;
    private static final int TITLE_TEXT  = 0x592424;
    private static final int LABEL_TEXT  = 0xFFFFFFFF;

    // ---- Arrow icons: swap if these don't resolve / aren't real arrows ----
    private static final AllIcons ICON_LEFT = AllIcons.I_MOVE_GAUGE;
    private static final AllIcons ICON_RIGHT = AllIcons.I_MOVE_GAUGE;

    private IconButton confirmButton;
    private ThresholdSlider thresholdSlider;

    private List<Component> retentionOptions;
    private List<Component> fullnessOptions;

    private int retention;
    private int fullness;
    private int threshold;

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
    }

    private int visibleRowCount() {
        int n = 1;
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

        addArrows(row++, () -> changeRetention(-1), () -> changeRetention(+1));

        if (showFullnessRow)
            addArrows(row++, () -> changeFullness(-1), () -> changeFullness(+1));

        if (showThresholdRow) {
            int barTop = rowTop(row);
            addArrows(row, () -> nudgeThreshold(-1), () -> nudgeThreshold(+1));
            thresholdSlider = new ThresholdSlider(x + BAR_X, barTop, BAR_W, BAR_H);
            addRenderableWidget(thresholdSlider);
            row++;
        }

        confirmButton = new IconButton(
                x + PANEL_W - 30, guiTop + panelH - FOOTER_H + 4, AllIcons.I_CONFIRM);
        confirmButton.withCallback(this::onClose);
        addRenderableWidget(confirmButton);
    }

    /** Places left/right arrows vertically centered on the bar midline. */
    private void addArrows(int row, Runnable onLeft, Runnable onRight) {
        int x = guiLeft;
        int barTop = rowTop(row);
        int arrowY = barTop + (BAR_H - ARROW_SIZE) / 2;

        IconButton left = new IconButton(x + BAR_X - ARROW_SIZE - ARROW_GAP, arrowY, ICON_LEFT);
        left.withCallback(onLeft);
        addRenderableWidget(left);

        IconButton right = new IconButton(x + BAR_X + BAR_W + ARROW_GAP, arrowY, ICON_RIGHT);
        right.withCallback(onRight);
        addRenderableWidget(right);
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
                new StationConfigurationPacket(blockEntity.getBlockPos(), retention, fullness, threshold));
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
        MutableComponent header = Component.translatable(titleKey);
        int stripTop = y + 8;
        int stripBottom = y + TITLE_H;
        int titleY = stripTop + (stripBottom - stripTop - font.lineHeight) / 2 + 1;
        graphics.drawString(font, header,
                x + PANEL_W / 2 - font.width(header) / 2, titleY, TITLE_TEXT, false);

        int row = 0;
        drawBar(graphics, row++, retentionOptions.get(retention));
        if (showFullnessRow)
            drawBar(graphics, row++, fullnessOptions.get(fullness));
        // threshold row's bar + label are drawn by the slider widget itself
        if (showThresholdRow) row++;
    }

    /** Flat brown/brass panel: nested border layers, brass title, footer strip. */
    private void drawPanel(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + PANEL_W, y + panelH, BORDER_DARK);
        g.fill(x + 3, y + 3, x + PANEL_W - 3, y + panelH - 3, BORDER_MID);
        g.fill(x + 6, y + 6, x + PANEL_W - 6, y + panelH - 6, PANEL_FILL);
        // title strip
        g.fill(x + 8, y + 8, x + PANEL_W - 8, y + TITLE_H, TITLE_FILL);
        // footer strip
        g.fill(x + 6, y + panelH - FOOTER_H, x + PANEL_W - 6, y + panelH - 6, FOOTER_FILL);
    }

    /** Identical bar for non-slider rows, centered label. */
    private void drawBar(GuiGraphics g, int row, Component label) {
        int barX = guiLeft + BAR_X;
        int barY = rowTop(row);
        g.fill(barX - 1, barY - 1, barX + BAR_W + 1, barY + BAR_H + 1, BAR_BORDER);
        g.fill(barX, barY, barX + BAR_W, barY + BAR_H, BAR_FILL);
        int textX = barX + (BAR_W - font.width(label)) / 2;
        int textY = barY + (BAR_H - font.lineHeight) / 2 + 1;
        g.drawString(font, label, textX, textY, LABEL_TEXT, true);
    }

    /** Vanilla slider mapped to 1..MAX_THRESHOLD, draws its own bar + handle. */
    private class ThresholdSlider extends AbstractSliderButton {
        ThresholdSlider(int x, int y, int w, int h) {
            super(x, y, w, h, Component.empty(), 0.0);
            syncFromThreshold();
        }

        void syncFromThreshold() {
            int max = AbstractShulkerStationBlockEntity.MAX_THRESHOLD;
            this.value = (threshold - 1) / (double) (max - 1);
            updateMessage();
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