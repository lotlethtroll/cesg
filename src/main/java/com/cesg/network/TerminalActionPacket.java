package com.cesg.network;

import com.cesg.CESG;
import com.cesg.gateways.StorageBridgeBlockEntity;
import com.cesg.storage.network.StorageNetwork;
import com.cesg.storage.network.StorageTerminalMenu;
import com.simibubi.create.foundation.utility.AdventureUtil;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> server: a click on the terminal's virtual grid (Phase 6D). Follows the standard
 * storage-terminal contract: withdraw to cursor, shift-withdraw to inventory, deposit the carried
 * stack. For deposits {@code sample} is ignored (the server trusts only its own carried stack).
 */
public record TerminalActionPacket(int containerId, int mode, ItemStack sample, int count)
        implements CustomPacketPayload {
    public static final Type<TerminalActionPacket> TYPE = new Type<>(CESG.id("terminal_action"));

    public static final int WITHDRAW_TO_CURSOR = 0;
    public static final int WITHDRAW_TO_INVENTORY = 1;
    public static final int DEPOSIT = 2;
    public static final int CLEAR_CRAFT = 3;
    // Partner-section (Storage Bridge) variants: same gestures, routed across the gateway.
    public static final int REMOTE_WITHDRAW_TO_CURSOR = 4;
    public static final int REMOTE_WITHDRAW_TO_INVENTORY = 5;
    public static final int REMOTE_DEPOSIT = 6;

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalActionPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, TerminalActionPacket::containerId,
                    ByteBufCodecs.VAR_INT, TerminalActionPacket::mode,
                    ItemStack.OPTIONAL_STREAM_CODEC, TerminalActionPacket::sample,
                    ByteBufCodecs.VAR_INT, TerminalActionPacket::count,
                    TerminalActionPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TerminalActionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player == null || player.isSpectator() || AdventureUtil.isAdventure(player))
                return;
            if (!(player.containerMenu instanceof StorageTerminalMenu menu)
                    || menu.containerId != packet.containerId()
                    || !menu.stillValid(player))
                return;

            switch (packet.mode()) {
                case WITHDRAW_TO_CURSOR -> withdrawToCursor(player, menu, packet);
                case WITHDRAW_TO_INVENTORY -> withdrawToInventory(player, menu, packet);
                case DEPOSIT -> deposit(player, menu, packet);
                case CLEAR_CRAFT -> menu.clearCraftingGrid();
                case REMOTE_WITHDRAW_TO_CURSOR -> remoteWithdrawToCursor(player, menu, packet);
                case REMOTE_WITHDRAW_TO_INVENTORY -> remoteWithdrawToInventory(player, menu, packet);
                case REMOTE_DEPOSIT -> remoteDeposit(player, menu, packet);
                default -> {
                }
            }
            menu.requestRefresh();
        });
    }

    private static void withdrawToCursor(ServerPlayer player, StorageTerminalMenu menu, TerminalActionPacket packet) {
        ItemStack sample = packet.sample();
        if (sample.isEmpty())
            return;
        ItemStack carried = menu.getCarried();
        if (!carried.isEmpty() && !ItemStack.isSameItemSameComponents(carried, sample))
            return; // cursor holds something else; ignore the click
        int room = sample.getMaxStackSize() - carried.getCount();
        int want = Math.min(packet.count(), room);
        if (want <= 0)
            return;
        ItemStack pulled = StorageNetwork.extract(player.level(), menu.getTerminalPos(), sample, want);
        if (pulled.isEmpty())
            return;
        if (carried.isEmpty())
            menu.setCarried(pulled);
        else
            carried.grow(pulled.getCount());
    }

    private static void withdrawToInventory(ServerPlayer player, StorageTerminalMenu menu, TerminalActionPacket packet) {
        if (packet.sample().isEmpty())
            return;
        ItemStack pulled = StorageNetwork.extract(player.level(), menu.getTerminalPos(),
                packet.sample(), packet.count());
        if (!pulled.isEmpty())
            player.getInventory().placeItemBackInInventory(pulled);
    }

    private static void deposit(ServerPlayer player, StorageTerminalMenu menu, TerminalActionPacket packet) {
        ItemStack carried = menu.getCarried();
        if (carried.isEmpty())
            return;
        int amount = Math.min(Math.max(packet.count(), 0), carried.getCount());
        if (amount <= 0)
            return;
        ItemStack remainder = StorageNetwork.insert(player.level(), menu.getTerminalPos(),
                carried.copyWithCount(amount));
        int inserted = amount - remainder.getCount();
        if (inserted <= 0)
            return;
        carried.shrink(inserted);
        if (carried.isEmpty())
            menu.setCarried(ItemStack.EMPTY);
    }

    // ---- Partner-section variants (routed across the gateway via network Storage Bridges) ---------

    private static void remoteWithdrawToCursor(ServerPlayer player, StorageTerminalMenu menu,
            TerminalActionPacket packet) {
        ItemStack sample = packet.sample();
        StorageBridgeBlockEntity bridge = menu.primaryBridge();
        if (sample.isEmpty() || bridge == null)
            return;
        ItemStack carried = menu.getCarried();
        if (!carried.isEmpty() && !ItemStack.isSameItemSameComponents(carried, sample))
            return;
        int want = Math.min(packet.count(), sample.getMaxStackSize() - carried.getCount());
        if (want <= 0)
            return;
        ItemStack pulled = bridge.terminalWithdrawRemote(sample, want, player);
        if (pulled.isEmpty())
            return;
        if (carried.isEmpty())
            menu.setCarried(pulled);
        else
            carried.grow(pulled.getCount());
    }

    private static void remoteWithdrawToInventory(ServerPlayer player, StorageTerminalMenu menu,
            TerminalActionPacket packet) {
        StorageBridgeBlockEntity bridge = menu.primaryBridge();
        if (packet.sample().isEmpty() || bridge == null)
            return;
        ItemStack pulled = bridge.terminalWithdrawRemote(packet.sample(), packet.count(), player);
        if (!pulled.isEmpty())
            player.getInventory().placeItemBackInInventory(pulled);
    }

    private static void remoteDeposit(ServerPlayer player, StorageTerminalMenu menu,
            TerminalActionPacket packet) {
        ItemStack carried = menu.getCarried();
        StorageBridgeBlockEntity bridge = menu.primaryBridge();
        if (carried.isEmpty() || bridge == null)
            return;
        int amount = Math.min(Math.max(packet.count(), 0), carried.getCount());
        if (amount <= 0)
            return;
        ItemStack remainder = bridge.terminalDepositRemote(carried.copyWithCount(amount), player);
        int inserted = amount - remainder.getCount();
        if (inserted <= 0)
            return;
        carried.shrink(inserted);
        if (carried.isEmpty())
            menu.setCarried(ItemStack.EMPTY);
    }
}
