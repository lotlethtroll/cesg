package com.cesg.upgrades;

import com.cesg.init.CESGRegistration;
import com.cesg.client.MechanicalPanel;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Tiled mechanical GUI. The panel and every slot background are painted to fit the
 * menu's slot positions exactly, so the window grows wider (not taller) as capacity increases and
 * never relies on a fixed-width vanilla texture that would bleed or misalign. The upgrade sidebar
 * renders as a NOTCH only as tall as its slots (plus the filter slot while one is configured).
 */
public class EnhancedShulkerScreen extends AbstractContainerScreen<EnhancedShulkerMenu> {
    private static final int VANILLA_SLOT = 18;
    private static final int TEXTURE_SIZE = 18;
    private static final int SLOT_U = 0;
    private static final int SLOT_V = 0;

    private static final int HIGHLIGHT_COLOR = 0xFFE0C78D;
    private static final int SHADOW_COLOR = 0xFF424B45;
    private static final int LABEL_COLOR = 0x34392F;
    /** Aqua accent marking the filter configuration slot apart from storage/upgrade slots. */
    private static final int FILTER_ACCENT = 0xFF3D9E9C;
    /** Crimson accent for the void-list configuration slot. */
    private static final int VOID_ACCENT = 0xFF9E3D50;

    private static final int SHRINK_DIGIT_THRESHOLD = 3;
    private static final float SHRUNK_COUNT_SCALE = 0.75f;
    /** Empty sidebar slots draw their placeholder icon at this opacity; occupied slots render solid. */
    private static final float PLACEHOLDER_ALPHA = 0.4f;

    /** Only the 18×18 single-slot sprite is sampled from this texture; the panel itself is drawn with fills. */
    private static final ResourceLocation SLOT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("cesg", "textures/gui/slot.png");

    private final int labelLeft;
    private final int playerLabelLeft;
    private final ItemStack filterGhost;
    private final ItemStack voidGhost;
    private final ItemStack upgradeGhost;

    public EnhancedShulkerScreen(EnhancedShulkerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.labelLeft = menu.getStorageSlotLeft();
        this.playerLabelLeft = menu.getPlayerInventoryLeft();
        this.imageWidth = menu.getImageWidth();
        this.imageHeight = menu.getImageHeight();
        this.inventoryLabelY = menu.getPlayerInventoryTopY() - 11;
        this.filterGhost = new ItemStack(com.simibubi.create.AllItems.FILTER.get());
        this.voidGhost = new ItemStack(CESGRegistration.VOID_UPGRADE.get());
        this.upgradeGhost = new ItemStack(CESGRegistration.STACK_DEPTH_UPGRADE_T1.get());
    }

    @Override
    protected void init() {
        int normalHeight = menu.getImageHeight();
        int hotbarOffset = height < normalHeight + 48 ? -4 : 0;
        imageHeight = normalHeight + hotbarOffset;
        menu.positionHotbar(hotbarOffset);
        super.init();
        topPos = Math.max(0, (height - 24 - imageHeight) / 2);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // Main panel starts just left of the storage grid; the sidebar hangs off it as a notch.
        int mainLeft = menu.hasUpgradeColumn()
                ? EnhancedShulkerMenu.UPGRADE_COLUMN_WIDTH - EnhancedShulkerMenu.MAIN_PANEL_LEFT_BORDER
                : 0;
        drawPanel(graphics, leftPos + mainLeft, topPos, imageWidth - mainLeft, imageHeight);

        if (menu.hasUpgradeColumn()) {
            int slotsBottom = EnhancedShulkerMenu.UPGRADE_SLOT_Y
                    + menu.getUpgradeSlotCount() * EnhancedShulkerMenu.UPGRADE_SLOT_HEIGHT;
            if (menu.isVoidSlotActive())
                slotsBottom = menu.getVoidConfigSlotY() + VANILLA_SLOT;
            else if (menu.isFilterSlotActive())
                slotsBottom = menu.getFilterConfigSlotY() + VANILLA_SLOT;
            int notchHeight = slotsBottom + EnhancedShulkerMenu.UPGRADE_SLOT_Y;
            drawNotchPanel(graphics, leftPos, topPos,
                    EnhancedShulkerMenu.UPGRADE_COLUMN_WIDTH + 1, notchHeight, mainLeft);
        }

        for (Slot slot : menu.slots) {
            if (slot.isActive()) {
                graphics.blit(SLOT_TEXTURE, leftPos + slot.x - 1, topPos + slot.y - 1, SLOT_U, SLOT_V,
                        VANILLA_SLOT, VANILLA_SLOT, TEXTURE_SIZE, TEXTURE_SIZE);
            }
        }

        // Accent frames mark config slots apart from storage: aqua = filter, crimson = void-list.
        if (menu.isFilterSlotActive())
            drawAccent(graphics, menu.slots.get(menu.getFilterSlotIndex()), FILTER_ACCENT);
        if (menu.isVoidSlotActive())
            drawAccent(graphics, menu.slots.get(menu.getVoidSlotIndex()), VOID_ACCENT);
    }

    private void drawAccent(GuiGraphics graphics, Slot slot, int color) {
        int x0 = leftPos + slot.x - 2;
        int y0 = topPos + slot.y - 2;
        int x1 = leftPos + slot.x + VANILLA_SLOT;
        int y1 = topPos + slot.y + VANILLA_SLOT;
        graphics.fill(x0, y0, x1, y0 + 1, color);
        graphics.fill(x0, y1 - 1, x1, y1, color);
        graphics.fill(x0, y0, x0 + 1, y1, color);
        graphics.fill(x1 - 1, y0, x1, y1, color);
    }

    /**
     * Like {@link #drawPanel} but open on the right: the notch flows seamlessly into the main
     * panel — no right border, and the bottom shadow stops where the main panel body begins.
     */
    private static void drawNotchPanel(GuiGraphics graphics, int x, int y, int width, int height, int mainLeft) {
        MechanicalPanel.panel(graphics, x, y, mainLeft+4, height);
        MechanicalPanel.body(graphics, x+mainLeft, y+4, 4, height-4);
        graphics.fill(x+mainLeft-4, y, x+mainLeft+4, y+1, SHADOW_COLOR);
        graphics.fill(x+mainLeft-4, y+1, x+mainLeft+4, y+2, 0xFFB39759);
        graphics.fill(x+mainLeft-4, y+2, x+mainLeft+4, y+3, HIGHLIGHT_COLOR);
        graphics.fill(x+mainLeft-4, y+3, x+mainLeft+4, y+4, SHADOW_COLOR);
    }

    private static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        MechanicalPanel.panel(graphics, x, y, width, height);
        graphics.fill(x+4, y+4, x+width-4, y+16, 0xFFA89B79);
    }

    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot) {
        int slotIndex = menu.slots.indexOf(slot);
        if (slot.isActive() && slotIndex >= 0) {
            if (slotIndex == menu.getFilterSlotIndex()) {
                if (slot.hasItem())
                    renderSolidItem(graphics, slot.getItem(), slot.x, slot.y);
                else
                    renderTranslucentPlaceholder(graphics, filterGhost, slot.x, slot.y);
                return;
            }
            if (slotIndex == menu.getVoidSlotIndex()) {
                if (slot.hasItem())
                    renderSolidItem(graphics, slot.getItem(), slot.x, slot.y);
                else
                    renderTranslucentPlaceholder(graphics, voidGhost, slot.x, slot.y);
                return;
            }
            if (slotIndex < menu.getUpgradeSlotCount()) {
                if (slot.hasItem()) {
                    super.renderSlot(graphics, slot);
                } else {
                    renderTranslucentPlaceholder(graphics, upgradeGhost, slot.x, slot.y);
                }
                return;
            }
        }

        ItemStack stack = slot.getItem();
        if (stack.getCount() < 100 || !slot.isActive()) {
            super.renderSlot(graphics, slot);
            return;
        }

        int x = slot.x;
        int y = slot.y;
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 100.0F);
        graphics.renderItem(stack, x, y, x + y * this.imageWidth);
        graphics.renderItemDecorations(this.font, stack, x, y, "");
        drawShrunkCount(graphics, stack.getCount(), x, y);
        graphics.pose().popPose();
    }

    /** Sidebar placeholder while a config or upgrade slot is empty. */
    private static void renderTranslucentPlaceholder(GuiGraphics graphics, ItemStack stack, int x, int y) {
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 100.0F);
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, PLACEHOLDER_ALPHA);
        graphics.renderFakeItem(stack, x, y);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.pose().popPose();
    }

    /** Configured filter/void item at full opacity (still a ghost slot — not a real inventory item). */
    private void renderSolidItem(GuiGraphics graphics, ItemStack stack, int x, int y) {
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 100.0F);
        graphics.renderItem(stack, x, y);
        graphics.renderItemDecorations(font, stack, x, y, null);
        graphics.pose().popPose();
    }

    private void drawShrunkCount(GuiGraphics graphics, int count, int x, int y) {
        String text = String.valueOf(count);
        float scale = Math.min(text.length() >= SHRINK_DIGIT_THRESHOLD ? SHRUNK_COUNT_SCALE : 1.0f,
                16.0f / Math.max(1, font.width(text)));
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 200.0F);
        graphics.pose().translate(x + 16, y + 16, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(this.font, text, -this.font.width(text), -this.font.lineHeight, 0xFFFFFFFF, true);
        graphics.pose().popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, font.plainSubstrByWidth(title.getString(), imageWidth-labelLeft-8),
                labelLeft, 6, LABEL_COLOR, false);
        graphics.drawString(font, playerInventoryTitle, playerLabelLeft, inventoryLabelY, LABEL_COLOR, false);
    }
}
