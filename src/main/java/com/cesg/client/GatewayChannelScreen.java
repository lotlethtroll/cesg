package com.cesg.client;

import com.cesg.gateways.CrossDimensionalGatewayCoreBlockEntity;
import com.cesg.gateways.teleport.GatewayPartner;
import com.cesg.network.SetGatewayChannelPacket;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Destination picker for a Gateway Core (Phase 6A): a name box for THIS gateway plus a 4x4 grid of
 * channels. Bound channels show their partner's name and position as a tooltip; picking one sends the
 * selection (with the name) to the server; closing without picking still saves the name.
 */
@OnlyIn(Dist.CLIENT)
public class GatewayChannelScreen extends Screen {
    private static final int COLS = 4;
    private static final int BUTTON_SIZE = 28;
    private static final int GAP = 4;

    private final CrossDimensionalGatewayCoreBlockEntity core;
    private EditBox nameBox;
    private int gridTop;
    private boolean sent;
    private boolean chunkLoading;

    public GatewayChannelScreen(CrossDimensionalGatewayCoreBlockEntity core) {
        super(Component.translatable("cesg.gateway.channel_screen"));
        this.core = core;
        this.chunkLoading = core.isChunkLoading();
    }

    @Override
    protected void init() {
        super.init();
        int rows = CrossDimensionalGatewayCoreBlockEntity.CHANNEL_COUNT / COLS;
        int gridWidth = COLS * BUTTON_SIZE + (COLS - 1) * GAP;
        int gridHeight = rows * BUTTON_SIZE + (rows - 1) * GAP;
        int x0 = (width - gridWidth) / 2;
        int y0 = (height - gridHeight) / 2 + 22; // header: title, name box, active-channel line
        gridTop = y0;

        nameBox = new EditBox(font, x0, y0 - 32, gridWidth, 14,
                Component.translatable("cesg.gateway.name_hint"));
        nameBox.setMaxLength(32);
        nameBox.setHint(Component.translatable("cesg.gateway.name_hint")
                .withStyle(ChatFormatting.DARK_GRAY));
        nameBox.setValue(core.getGatewayName());
        addRenderableWidget(nameBox);

        if (com.cesg.CESGConfig.gatewayChunkLoadingAllowed()) {
            int rowsTotal = CrossDimensionalGatewayCoreBlockEntity.CHANNEL_COUNT / COLS;
            int gridBottom = y0 + rowsTotal * BUTTON_SIZE + (rowsTotal - 1) * GAP;
            addRenderableWidget(Button.builder(chunkLoadLabel(), b -> {
                chunkLoading = !chunkLoading;
                b.setMessage(chunkLoadLabel());
            }).bounds(x0, gridBottom + 6, gridWidth, 16)
                    .tooltip(Tooltip.create(Component.translatable("cesg.gateway.chunkload_tooltip")))
                    .build());
        }

        for (int channel = 0; channel < CrossDimensionalGatewayCoreBlockEntity.CHANNEL_COUNT; channel++) {
            int col = channel % COLS;
            int row = channel / COLS;
            final int picked = channel;
            Button button = Button.builder(label(channel), b -> select(picked))
                    .bounds(x0 + col * (BUTTON_SIZE + GAP), y0 + row * (BUTTON_SIZE + GAP), BUTTON_SIZE, BUTTON_SIZE)
                    .tooltip(Tooltip.create(describe(channel)))
                    .build();
            button.active = channel != core.getActiveChannel();
            addRenderableWidget(button);
        }
    }

    private Component label(int channel) {
        String number = String.valueOf(channel + 1);
        if (channel == core.getActiveChannel())
            return Component.literal(number).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        return core.getBinding(channel).isBound()
                ? Component.literal(number).withStyle(ChatFormatting.GREEN)
                : Component.literal(number).withStyle(ChatFormatting.GRAY);
    }

    private Component describe(int channel) {
        GatewayPartner binding = core.getBinding(channel);
        if (!binding.isBound())
            return Component.translatable("cesg.gateway.channel_unbound");
        if (binding.hasName())
            return Component.translatable("cesg.gateway.channel_bound_named", binding.name(),
                    binding.dimension().location().toString(),
                    binding.position().getX(), binding.position().getY(), binding.position().getZ());
        return Component.translatable("cesg.gateway.channel_bound",
                binding.dimension().location().toString(),
                binding.position().getX(), binding.position().getY(), binding.position().getZ());
    }

    private Component chunkLoadLabel() {
        return Component.translatable(chunkLoading
                ? "cesg.gateway.chunkload_on" : "cesg.gateway.chunkload_off");
    }

    private void select(int channel) {
        PacketDistributor.sendToServer(
                new SetGatewayChannelPacket(core.getBlockPos(), channel, nameBox.getValue(), chunkLoading));
        sent = true;
        onClose();
    }

    @Override
    public void onClose() {
        // Closing without picking a channel still commits the name + chunk-load toggle.
        if (!sent && (!nameBox.getValue().equals(core.getGatewayName())
                || chunkLoading != core.isChunkLoading()))
            PacketDistributor.sendToServer(
                    new SetGatewayChannelPacket(core.getBlockPos(), -1, nameBox.getValue(), chunkLoading));
        super.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, gridTop - 46, 0xFFFFFF);
        graphics.drawCenteredString(font,
                Component.translatable("cesg.gateway.channel_current", core.getActiveChannel() + 1),
                width / 2, gridTop - 13, 0xAAAAAA);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Typing a name must not trigger the inventory-close key.
        if (nameBox.isFocused() && keyCode != 256 /* ESC */)
            return nameBox.keyPressed(keyCode, scanCode, modifiers) || nameBox.canConsumeInput()
                    || super.keyPressed(keyCode, scanCode, modifiers);
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
