package com.cesg.network;

import java.util.List;

import com.cesg.CESG;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server -> client: the aggregated contents of the storage network a terminal is viewing (Phase 6D),
 * plus an optional partner-network section surfaced by a Storage Bridge on the same network (Phase 7A).
 * {@code remoteStatus} tells the client whether to show the Partner tab and how to colour it.
 */
public record TerminalContentPacket(int containerId, List<Entry> entries, List<Entry> remoteEntries,
        int remoteStatus) implements CustomPacketPayload {
    public static final Type<TerminalContentPacket> TYPE = new Type<>(CESG.id("terminal_content"));

    /** No Storage Bridge on the network — the client hides the Partner tab entirely. */
    public static final int REMOTE_NONE = 0;
    /** A Bridge exists but its partner is unbound/unloaded — greyed, silent (not an error). */
    public static final int REMOTE_OFFLINE = 1;
    /** Partner network linked and readable. */
    public static final int REMOTE_LIVE = 2;
    /** Bound + loaded, but no partner Bridge / no partner controller — a verified fault. */
    public static final int REMOTE_FAULT = 3;

    /** One distinct item (components included); {@code sample} always has count 1. */
    public record Entry(ItemStack sample, int total) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, Entry::sample,
                ByteBufCodecs.VAR_INT, Entry::total,
                Entry::new);

        /** Record equality would compare ItemStacks by identity; compare by item + components instead. */
        public boolean matches(Entry other) {
            return total == other.total && ItemStack.isSameItemSameComponents(sample, other.sample);
        }
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalContentPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, TerminalContentPacket::containerId,
                    Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), TerminalContentPacket::entries,
                    Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), TerminalContentPacket::remoteEntries,
                    ByteBufCodecs.VAR_INT, TerminalContentPacket::remoteStatus,
                    TerminalContentPacket::new);

    public static boolean sameEntries(List<Entry> a, List<Entry> b) {
        if (a.size() != b.size())
            return false;
        for (int i = 0; i < a.size(); i++)
            if (!a.get(i).matches(b.get(i)))
                return false;
        return true;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TerminalContentPacket packet, IPayloadContext context) {
        // Runs on the client; the client class is only loaded when the lambda body executes there.
        context.enqueueWork(() -> com.cesg.client.StorageTerminalScreen.acceptContent(packet));
    }
}
