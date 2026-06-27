package com.cesg.upgrades;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Procedurally drawn chest-style GUI. The panel and every slot background are painted to fit the
 * menu's slot positions exactly, so the window grows wider (not taller) as capacity increases and
 * never relies on a fixed-width vanilla texture that would bleed or misalign.
 */
public class EnhancedShulkerScreen extends AbstractContainerScreen<EnhancedShulkerMenu> {
    private static final int VANILLA_SLOT = 18;
    private static final int TEXTURE_SIZE = 256;
    private static final int SLOT_U = 7;
    private static final int SLOT_V = 17;

    private static final int BODY_COLOR = 0xFFC6C6C6;
    private static final int HIGHLIGHT_COLOR = 0xFFFFFFFF;
    private static final int SHADOW_COLOR = 0xFF555555;
    private static final int LABEL_COLOR = 0x404040;

    private static final int SHRINK_DIGIT_THRESHOLD = 3;
    private static final float SHRUNK_COUNT_SCALE = 0.75f;

    /** Only the 18×18 single-slot sprite is sampled from this texture; the panel itself is drawn with fills. */
    private static final ResourceLocation SLOT_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    private final int labelLeft;
    private final int playerLabelLeft;

    public EnhancedShulkerScreen(EnhancedShulkerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.labelLeft = menu.getStorageSlotLeft();
        this.playerLabelLeft = menu.getPlayerInventoryLeft();
        this.imageWidth = menu.getImageWidth();
        this.imageHeight = menu.getImageHeight();
        this.inventoryLabelY = menu.getPlayerInventoryTopY() - 11;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
        for (Slot slot : menu.slots) {
            if (slot.isActive()) {
                graphics.blit(SLOT_TEXTURE, leftPos + slot.x - 1, topPos + slot.y - 1, SLOT_U, SLOT_V,
                        VANILLA_SLOT, VANILLA_SLOT, TEXTURE_SIZE, TEXTURE_SIZE);
            }
        }
    }

    private static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, BODY_COLOR);
        graphics.fill(x, y, x + width, y + 1, HIGHLIGHT_COLOR);
        graphics.fill(x, y, x + 1, y + height, HIGHLIGHT_COLOR);
        graphics.fill(x + width - 1, y, x + width, y + height, SHADOW_COLOR);
        graphics.fill(x, y + height - 1, x + width, y + height, SHADOW_COLOR);
    }

    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot) {
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

    private void drawShrunkCount(GuiGraphics graphics, int count, int x, int y) {
        String text = String.valueOf(count);
        float scale = text.length() >= SHRINK_DIGIT_THRESHOLD ? SHRUNK_COUNT_SCALE : 1.0f;
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 200.0F);
        graphics.pose().translate(x + VANILLA_SLOT - 1, y + VANILLA_SLOT - 1, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(this.font, text, -this.font.width(text), -this.font.lineHeight, 0xFFFFFFFF, true);
        graphics.pose().popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, labelLeft, 6, LABEL_COLOR, false);
        graphics.drawString(font, playerInventoryTitle, playerLabelLeft, inventoryLabelY, LABEL_COLOR, false);
    }
}
