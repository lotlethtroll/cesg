package com.cesg.client;

import com.cesg.CESGConfig;
import com.cesg.gateways.CrossDimensionalGatewayCoreBlockEntity;
import com.cesg.gateways.teleport.GatewayPartner;
import com.cesg.network.OpenGatewayFilterPacket;
import com.cesg.network.SetGatewayChannelPacket;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            "cesg", "textures/gui/gateway_channels.png");
    private int panelLeft, panelTop, panelHeight;
    private final CrossDimensionalGatewayCoreBlockEntity core;
    private EditBox nameBox;
    private int gridTop;
    private int gridLeft;
    private boolean sent;
    private boolean chunkLoading;
    private boolean routeMode;

    public GatewayChannelScreen(CrossDimensionalGatewayCoreBlockEntity core) {
        super(Component.translatable("cesg.gateway.channel_screen"));
        this.core = core;
        this.chunkLoading = core.isChunkLoading();
        this.routeMode = core.isRouteMode();
    }

    @Override
    protected void init() {
        super.init();
        int gridWidth = COLS * BUTTON_SIZE + (COLS - 1) * GAP;
        int footerRows = (CESGConfig.gatewayChunkLoadingAllowed() ? 1 : 0)
                + (CESGConfig.gatewayFanOutAllowed() ? 1 : 0);
        panelHeight = 190 + footerRows * 20;
        panelLeft = (width - 176) / 2;
        panelTop = Math.max(0, (height - panelHeight) / 2);
        int x0 = panelLeft + (176 - gridWidth) / 2;
        int y0 = panelTop + 58;
        gridTop = y0;
        gridLeft = x0;

        String pendingName = nameBox == null ? core.getGatewayName() : nameBox.getValue();
        nameBox = new EditBox(font, panelLeft + 8, panelTop + 22, 160, 14,
                Component.translatable("cesg.gateway.name_hint"));
        nameBox.setMaxLength(32);
        nameBox.setHint(Component.translatable("cesg.gateway.name_hint")
                .withStyle(ChatFormatting.DARK_GRAY));
        nameBox.setValue(pendingName);
        addRenderableWidget(nameBox);

        if (com.cesg.CESGConfig.gatewayChunkLoadingAllowed()) {
            int rowsTotal = CrossDimensionalGatewayCoreBlockEntity.CHANNEL_COUNT / COLS;
            int gridBottom = y0 + rowsTotal * BUTTON_SIZE + (rowsTotal - 1) * GAP;
            Button loadButton = new MechanicalButton(panelLeft + 8, gridBottom + 6, 160, 16,
                    chunkLoadLabel(), b -> {
                        chunkLoading = !chunkLoading;
                        b.setMessage(chunkLoadLabel());
                    }, () -> chunkLoading);
            loadButton.setTooltip(Tooltip.create(Component.translatable("cesg.gateway.chunkload_tooltip")));
            addRenderableWidget(loadButton);
        }

        if (CESGConfig.gatewayFanOutAllowed()) {
            int rowsTotal = CrossDimensionalGatewayCoreBlockEntity.CHANNEL_COUNT / COLS;
            int gridBottom = y0 + rowsTotal * BUTTON_SIZE + (rowsTotal - 1) * GAP;
            int extra = com.cesg.CESGConfig.gatewayChunkLoadingAllowed() ? 20 : 0;
            Button routeButton = new MechanicalButton(panelLeft + 8, gridBottom + 6 + extra, 160, 16,
                    routeLabel(), b -> {
                        routeMode = !routeMode;
                        b.setMessage(routeLabel());
                    }, () -> routeMode);
            routeButton.setTooltip(Tooltip.create(Component.translatable("cesg.gateway.route_tooltip")));
            addRenderableWidget(routeButton);
        }

        for (int channel = 0; channel < CrossDimensionalGatewayCoreBlockEntity.CHANNEL_COUNT; channel++) {
            int col = channel % COLS;
            int row = channel / COLS;
            final int picked = channel;
            Button button = new MechanicalButton(x0 + col * (BUTTON_SIZE + GAP),
                    y0 + row * (BUTTON_SIZE + GAP), BUTTON_SIZE, BUTTON_SIZE,
                    label(channel), b -> select(picked), () -> picked == core.getActiveChannel());
            button.setTooltip(Tooltip.create(describe(channel)));
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
            return Component.translatable("cesg.gateway.channel_bound_named",
                    binding.displayName(core.getLevel()), // live, so a renamed destination shows through
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

    private Component routeLabel() {
        return Component.translatable(routeMode ? "cesg.gateway.route_on" : "cesg.gateway.route_off");
    }

    private void select(int channel) {
        PacketDistributor.sendToServer(new SetGatewayChannelPacket(
                core.getBlockPos(), channel, nameBox.getValue(), chunkLoading, routeMode));
        sent = true;
        onClose();
    }

    /** Right-click a channel to edit its routing filter (commits pending settings first). */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 && CESGConfig.gatewayFanOutAllowed()) {
            int channel = channelAt(mouseX, mouseY);
            if (channel >= 0) {
                PacketDistributor.sendToServer(new SetGatewayChannelPacket(
                        core.getBlockPos(), -1, nameBox.getValue(), chunkLoading, routeMode));
                PacketDistributor.sendToServer(new OpenGatewayFilterPacket(core.getBlockPos(), channel));
                sent = true;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int channelAt(double mouseX, double mouseY) {
        for (int channel = 0; channel < CrossDimensionalGatewayCoreBlockEntity.CHANNEL_COUNT; channel++) {
            int bx = gridLeft + (channel % COLS) * (BUTTON_SIZE + GAP);
            int by = gridTop + (channel / COLS) * (BUTTON_SIZE + GAP);
            if (mouseX >= bx && mouseX < bx + BUTTON_SIZE && mouseY >= by && mouseY < by + BUTTON_SIZE)
                return channel;
        }
        return -1;
    }

    @Override
    public void onClose() {
        // Closing without picking a channel still commits the name + chunk-load + route toggles.
        if (!sent && (!nameBox.getValue().equals(core.getGatewayName())
                || chunkLoading != core.isChunkLoading() || routeMode != core.isRouteMode()))
            PacketDistributor.sendToServer(new SetGatewayChannelPacket(
                    core.getBlockPos(), -1, nameBox.getValue(), chunkLoading, routeMode));
        super.onClose();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(BACKGROUND, panelLeft, panelTop, 0, 0, 176, panelHeight - 4, 176, 230);
        graphics.blit(BACKGROUND, panelLeft, panelTop + panelHeight - 4, 0, 226, 176, 4, 176, 230);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawString(font, font.plainSubstrByWidth(title.getString(), 160),
                panelLeft + 8, panelTop + 6, 0x34392F, false);
        Component current = Component.translatable("cesg.gateway.channel_current", core.getActiveChannel() + 1);
        String text = font.plainSubstrByWidth(current.getString(), 156);
        graphics.drawString(font, text, (width - font.width(text)) / 2, panelTop + 42, 0x34392F, false);
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
