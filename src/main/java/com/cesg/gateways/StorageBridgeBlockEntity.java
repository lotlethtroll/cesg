package com.cesg.gateways;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.cesg.CESGConfig;
import com.cesg.init.CESGBlockEntities;
import com.cesg.init.CESGSounds;
import com.cesg.network.TerminalContentPacket;
import com.cesg.storage.network.StorageNetwork;
import com.cesg.util.CESGLang;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import org.jetbrains.annotations.Nullable;

/**
 * Phase 7A Cross-Dimensional Storage Bridge. The Bridge is BOTH a {@link StorageNetwork} member (so the
 * local terminal's cluster walk includes it) AND a ring-attached gateway endpoint (it finds its Core via
 * the ring BFS and routes over the active channel's partner, exactly like {@link GatewayPortBlockEntity}).
 *
 * <p>It surfaces the <em>partner</em> network's contents on the local terminal as a separate labeled
 * section (a lazily-refreshed, TTL-cached snapshot resolved server-side — both dimensions live on one
 * server, so no cross-client serialization is needed). Item movement — manual (terminal click) or passive
 * (the filtered bidirectional auto-transfer) — always models a move as extract-from-source then
 * insert-into-destination, holding partially-moved items in an internal buffer that persists across a
 * partner unload and drops on break. That is the same dupe/void-safe discipline the Gateway Port uses for
 * its send buffer, so a cross-dimension move can never commit before the source extract has succeeded.
 */
public class StorageBridgeBlockEntity extends BlockEntity implements IHaveGoggleInformation {
    /** In-transit buffer width per direction (items removed from one network, not yet delivered). */
    public static final int BUFFER_SLOTS = 9;
    /** Ghost filter width per direction. */
    public static final int FILTER_SLOTS = 9;
    private static final int FLUSH_INTERVAL = 10;
    private static final int IDLE_BACKOFF = 40;
    private static final int SYNC_INTERVAL = 10;
    /** How often partner liveness is re-resolved when no transfer is happening to resolve it for us. */
    private static final int STATUS_INTERVAL = 20;
    private static final int RING_SCAN_LIMIT = 64;

    /**
     * Liveness of the partner section, mirroring the Port's 3-state model. Also drives
     * {@link StorageBridgeBlock#STATUS} so the state is legible on the block itself, not only through
     * goggles — hence {@link StringRepresentable}.
     */
    public enum RemoteStatus implements StringRepresentable {
        /** Partner unloaded / gateway unbound: silent, greyed section (not an error). */
        OFFLINE,
        /** Partner resolved and its network scanned. */
        LIVE,
        /** Bound and loaded, but no partner Bridge or no operational partner network: a verified fault. */
        FAULT;

        private final String serialized = name().toLowerCase(java.util.Locale.ROOT);

        @Override
        public String getSerializedName() {
            return serialized;
        }
    }

    private long nextFlushTime;
    private long nextStatusTime;
    private long nextTransferFxTime;
    private boolean syncDirty;

    // Items removed from the LOCAL network, awaiting insert into the REMOTE network (push, local -> remote).
    final ItemStackHandler outBuffer = bufferHandler();
    // Items removed from the REMOTE network, awaiting insert into the LOCAL network (pull, remote -> local).
    final ItemStackHandler inBuffer = bufferHandler();

    // Per-direction filters (ghost items). Empty whitelist = nothing passes; empty blacklist = everything.
    final ItemStackHandler sendFilter = filterHandler();
    final ItemStackHandler pullFilter = filterHandler();
    private boolean sendBlacklist;
    private boolean pullBlacklist;
    private boolean pushEnabled;
    private boolean pullEnabled;
    /** When set, remote gateways may not EXTRACT from this Bridge's network. See {@link #isLocked}. */
    private boolean locked;

    // Cached view of the partner network, refreshed lazily when a terminal reads it.
    private List<TerminalContentPacket.Entry> remoteEntries = List.of();
    private RemoteStatus remoteStatus = RemoteStatus.OFFLINE;
    private long remoteExpiresAt;

    private ItemStackHandler bufferHandler() {
        return new ItemStackHandler(BUFFER_SLOTS) {
            @Override
            protected void onContentsChanged(int slot) {
                contentsChanged();
            }
        };
    }

    /** Ghost-filter storage: edits persist and mark the BE dirty, but never hold real items. */
    private ItemStackHandler filterHandler() {
        return new ItemStackHandler(FILTER_SLOTS) {
            @Override
            protected void onContentsChanged(int slot) {
                contentsChanged();
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1; // filters are type-only; never stack
            }
        };
    }

    private void contentsChanged() {
        setChanged();
        syncDirty = true;
    }

    public StorageBridgeBlockEntity(BlockPos pos, BlockState state) {
        super(CESGBlockEntities.STORAGE_BRIDGE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, StorageBridgeBlockEntity be) {
        if (be.syncDirty && level.getGameTime() % SYNC_INTERVAL == 0) {
            be.syncDirty = false;
            level.sendBlockUpdated(pos, state, state, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
        // Liveness is a property of the wiring, not of having work to do, so it is resolved on its own
        // cadence rather than as a side effect of a transfer. A bridge with push and pull both off (or a
        // gateway that is bound but out of fuel) never reaches the resolve inside flushPassive(), and used
        // to sit at OFFLINE until something else — a terminal opening, say — happened to resolve it.
        if (level.getGameTime() >= be.nextStatusTime && level instanceof ServerLevel serverLevel) {
            be.resolveStatus(serverLevel);
            be.syncStatusToState(level, pos);
        }
        if (level.getGameTime() < be.nextFlushTime)
            return;
        be.nextFlushTime = level.getGameTime() + be.flushPassive();
        be.syncStatusToState(level, pos);
    }

    /**
     * {@link #resolveRemote} plus a stamp on the status cadence, so a flush-driven resolve counts as this
     * interval's status check and a busy bridge does not walk the ring twice.
     */
    @Nullable
    private RemoteEndpoint resolveStatus(ServerLevel level) {
        nextStatusTime = level.getGameTime() + STATUS_INTERVAL;
        return resolveRemote(level);
    }

    /**
     * Publishes {@link #remoteStatus} onto {@link StorageBridgeBlock#STATUS}, and only writes when the
     * value actually changed, so an idle bridge never issues block updates. Re-reads the state rather than
     * trusting the ticker's copy, which goes stale the moment this writes.
     */
    private void syncStatusToState(Level level, BlockPos pos) {
        BlockState current = level.getBlockState(pos);
        if (!current.hasProperty(StorageBridgeBlock.STATUS)
                || current.getValue(StorageBridgeBlock.STATUS) == remoteStatus)
            return;
        RemoteStatus previous = current.getValue(StorageBridgeBlock.STATUS);
        level.setBlock(pos, current.setValue(StorageBridgeBlock.STATUS, remoteStatus),
                net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        if (level instanceof ServerLevel serverLevel && previous != remoteStatus) {
            if (remoteStatus == RemoteStatus.LIVE) {
                serverLevel.playSound(null, pos, CESGSounds.LINK_LIVE.value(), SoundSource.BLOCKS, 0.7f, 1.0f);
                serverLevel.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.65,
                        pos.getZ() + 0.5, 8, 0.22, 0.3, 0.22, 0.025);
            } else if (remoteStatus == RemoteStatus.FAULT) {
                serverLevel.playSound(null, pos, CESGSounds.LINK_FAULT.value(), SoundSource.BLOCKS, 0.7f, 1.0f);
                serverLevel.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.65,
                        pos.getZ() + 0.5, 5, 0.18, 0.2, 0.18, 0.015);
            }
        }
    }

    // ---- Remote resolution ---------------------------------------------------------------------

    /** The partner Bridge's network anchor (its own position) plus the level it lives in. */
    private record RemoteEndpoint(ServerLevel level, BlockPos anchor) {}

    /**
     * Resolves the partner Bridge across the active gateway, updating {@link #remoteStatus}. Returns null
     * (with a status set) whenever the remote view cannot be established — unbound/unloaded is OFFLINE,
     * a bound-and-loaded ring with no partner Bridge is a FAULT.
     */
    /**
     * Whether the Bridge on the far side has locked its network against being drawn from. Checked at the
     * point of extraction rather than when resolving the partner, so a locked partner still shows LIVE and
     * still accepts pushes — only taking from it is refused.
     */
    private static boolean partnerLocked(RemoteEndpoint remote) {
        return remote.level.getBlockEntity(remote.anchor) instanceof StorageBridgeBlockEntity partner
                && partner.isLocked();
    }

    @Nullable
    private RemoteEndpoint resolveRemote(ServerLevel level) {
        CrossDimensionalGatewayCoreBlockEntity core = GatewayFuelHandler.findCore(level, worldPosition);
        if (core == null) {
            remoteStatus = RemoteStatus.OFFLINE; // not attached to a ring yet
            return null;
        }
        com.cesg.gateways.teleport.GatewayPartner partner = core.getPartner();
        if (!partner.isBound()) {
            remoteStatus = RemoteStatus.OFFLINE;
            return null;
        }
        ServerLevel partnerLevel = level.getServer().getLevel(partner.dimension());
        if (partnerLevel == null || !partnerLevel.isLoaded(partner.position())) {
            remoteStatus = RemoteStatus.OFFLINE; // partner chunk unloaded: silent, keep buffering
            return null;
        }
        if (!(partnerLevel.getBlockEntity(partner.position()) instanceof CrossDimensionalGatewayCoreBlockEntity partnerCore)) {
            remoteStatus = RemoteStatus.FAULT;
            return null;
        }
        List<StorageBridgeBlockEntity> bridges = findBridges(partnerLevel, partnerCore.getBlockPos());
        if (bridges.isEmpty()) {
            remoteStatus = RemoteStatus.FAULT; // bound + loaded, but the partner ring has no Bridge
            return null;
        }
        remoteStatus = RemoteStatus.LIVE;
        return new RemoteEndpoint(partnerLevel, bridges.get(0).worldPosition);
    }

    /**
     * Status-free partner resolver for a specific channel binding (used by fan-out routing, which visits
     * many channels per flush and must not thrash {@link #remoteStatus}). Returns null whenever that
     * channel's partner network can't be reached.
     */
    @Nullable
    private RemoteEndpoint resolvePartner(ServerLevel level, com.cesg.gateways.teleport.GatewayPartner partner) {
        if (!partner.isBound())
            return null;
        ServerLevel partnerLevel = level.getServer().getLevel(partner.dimension());
        if (partnerLevel == null || !partnerLevel.isLoaded(partner.position()))
            return null;
        if (!(partnerLevel.getBlockEntity(partner.position()) instanceof CrossDimensionalGatewayCoreBlockEntity partnerCore))
            return null;
        List<StorageBridgeBlockEntity> bridges = findBridges(partnerLevel, partnerCore.getBlockPos());
        if (bridges.isEmpty())
            return null;
        return new RemoteEndpoint(partnerLevel, bridges.get(0).worldPosition);
    }

    /** All Storage Bridges attached to the ring containing {@code corePos} (bridges sit beside ring blocks). */
    private static List<StorageBridgeBlockEntity> findBridges(Level level, BlockPos corePos) {
        List<StorageBridgeBlockEntity> bridges = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Set<BlockPos> checkedNeighbors = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(corePos.immutable());
        visited.add(corePos.immutable());
        while (!queue.isEmpty() && visited.size() <= RING_SCAN_LIMIT) {
            BlockPos pos = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (!level.isLoaded(next))
                    continue;
                if (checkedNeighbors.add(next)
                        && level.getBlockEntity(next) instanceof StorageBridgeBlockEntity bridge)
                    bridges.add(bridge);
                if (!visited.contains(next) && GatewayFuelHandler.isOwnRingBlock(level, next, corePos)) {
                    visited.add(next);
                    queue.add(next);
                }
            }
        }
        return bridges;
    }

    // ---- Remote snapshot (read by the terminal) --------------------------------------------------

    /** Terminal-facing: the partner network's aggregated contents, refreshed if the TTL has lapsed. */
    public List<TerminalContentPacket.Entry> remoteSnapshot() {
        if (level instanceof ServerLevel serverLevel)
            refreshRemoteSnapshot(serverLevel);
        return remoteEntries;
    }

    public RemoteStatus remoteStatus() {
        return remoteStatus;
    }

    private void refreshRemoteSnapshot(ServerLevel level) {
        long now = level.getGameTime();
        if (now < remoteExpiresAt)
            return;
        remoteExpiresAt = now + CESGConfig.bridgeSnapshotTtl();
        RemoteEndpoint remote = resolveRemote(level);
        if (remote == null) {
            remoteEntries = List.of();
            return;
        }
        StorageNetwork.Scan scan = StorageNetwork.scan(remote.level, remote.anchor);
        if (!scan.operational()) {
            remoteStatus = RemoteStatus.FAULT; // partner network has no controller
            remoteEntries = List.of();
            return;
        }
        Object2IntMap<ItemStack> totals = StorageNetwork.aggregate(scan);
        List<TerminalContentPacket.Entry> entries = new ArrayList<>(totals.size());
        for (Object2IntMap.Entry<ItemStack> entry : totals.object2IntEntrySet())
            entries.add(new TerminalContentPacket.Entry(entry.getKey(), entry.getIntValue()));
        remoteEntries = entries;
    }

    private void invalidateRemoteSnapshot() {
        remoteExpiresAt = 0;
    }

    // ---- Passive bidirectional transfer (D3) -----------------------------------------------------

    /** One passive-transfer attempt; returns the delay (ticks) until the next attempt. */
    private int flushPassive() {
        if (!(level instanceof ServerLevel serverLevel))
            return FLUSH_INTERVAL;
        boolean hasBuffered = !isEmpty(outBuffer) || !isEmpty(inBuffer);
        if (!pushEnabled && !pullEnabled && !hasBuffered)
            return FLUSH_INTERVAL; // nothing to do

        CrossDimensionalGatewayCoreBlockEntity core = GatewayFuelHandler.findCore(level, worldPosition);
        if (core == null || !core.canTravel())
            return IDLE_BACKOFF; // no active gateway: hold whatever is buffered, retry slowly

        RemoteEndpoint remote = resolveStatus(serverLevel); // active channel; also sets remoteStatus
        boolean routing = core.isRouteMode();
        // Non-route: nothing moves while the active partner is down. Route: push may still fan out to
        // other channels, so only bail when there is no push work AND the active channel is down.
        if (remote == null && !routing)
            return IDLE_BACKOFF;
        if (remote == null && routing && !pushEnabled && isEmpty(outBuffer))
            return IDLE_BACKOFF;

        // A Gateway Flux Battery on the ring gates this against its reserve so automation never starves
        // player travel — identical to the Gateway Port's flush.
        if (!core.tryConsumeAutomationFuel(CESGConfig.bridgeTransferCost()))
            return IDLE_BACKOFF;

        int max = CESGConfig.bridgeMaxItemsPerFlush();
        int moved = 0;
        // PUSH: pull items out of the LOCAL network into outBuffer, then deliver. In route mode the
        // per-channel filters are the only gate (extract whatever some channel accepts, then fan out);
        // otherwise the Bridge's own send filter decides, delivered to the active partner.
        if (pushEnabled) {
            if (routing)
                moved += fillBufferFromNetwork(serverLevel, worldPosition, outBuffer, max,
                        s -> core.routeChannel(s) >= 0);
            else
                moved += fillBufferFromNetwork(serverLevel, worldPosition, outBuffer, sendFilter, sendBlacklist, max);
        }
        if (routing)
            moved += flushBufferRouted(serverLevel, core, outBuffer);
        else if (remote != null) // non-route already bailed above when the partner is down
            moved += flushBufferToNetwork(remote.level, remote.anchor, outBuffer);
        // PULL (active channel only — fan-out is a distribution/send concern). Draining inBuffer into the
        // LOCAL network always runs, since local is reachable even when the partner is down.
        if (remote != null && pullEnabled && !partnerLocked(remote))
            moved += fillBufferFromNetwork(remote.level, remote.anchor, inBuffer, pullFilter, pullBlacklist, max);
        moved += flushBufferToNetwork(serverLevel, worldPosition, inBuffer);
        if (moved > 0)
            emitTransferFx(serverLevel);
        return FLUSH_INTERVAL;
    }

    /** Delivers each buffered stack to the partner network of the channel whose filter accepts it. */
    private int flushBufferRouted(ServerLevel level, CrossDimensionalGatewayCoreBlockEntity core,
            ItemStackHandler buffer) {
        Map<Integer, RemoteEndpoint> cache = new HashMap<>();
        int moved = 0;
        for (int slot = 0; slot < buffer.getSlots(); slot++) {
            ItemStack stack = buffer.getStackInSlot(slot);
            if (stack.isEmpty())
                continue;
            int channel = core.routeChannel(stack);
            if (channel < 0)
                continue; // no channel filter accepts it: leave it buffered
            RemoteEndpoint remote = cache.computeIfAbsent(channel, c -> resolvePartner(level, core.getBinding(c)));
            if (remote == null)
                continue; // that channel's partner is down: hold, retry next flush
            ItemStack remainder = StorageNetwork.insert(remote.level, remote.anchor, stack.copy());
            buffer.setStackInSlot(slot, remainder);
            moved += stack.getCount() - remainder.getCount();
        }
        return moved;
    }

    /** Extracts up to {@code max} filtered items out of a network into {@code buffer} (never overfills it). */
    private static int fillBufferFromNetwork(Level level, BlockPos anchor, ItemStackHandler buffer,
            ItemStackHandler filter, boolean blacklist, int max) {
        return fillBufferFromNetwork(level, anchor, buffer, max, sample -> passesFilter(sample, filter, blacklist));
    }

    /** As above, but with an arbitrary acceptance test (route mode extracts whatever any channel accepts). */
    private static int fillBufferFromNetwork(Level level, BlockPos anchor, ItemStackHandler buffer, int max,
            java.util.function.Predicate<ItemStack> accept) {
        StorageNetwork.Scan scan = StorageNetwork.scan(level, anchor);
        if (!scan.operational())
            return 0;
        int budget = max - countItems(buffer);
        if (budget <= 0)
            return 0;
        int moved = 0;
        Object2IntMap<ItemStack> totals = StorageNetwork.aggregate(scan);
        for (Object2IntMap.Entry<ItemStack> entry : totals.object2IntEntrySet()) {
            if (budget <= 0)
                break;
            ItemStack sample = entry.getKey();
            if (!accept.test(sample))
                continue;
            ItemStack pulled = StorageNetwork.extract(level, anchor, sample, budget);
            if (pulled.isEmpty())
                continue;
            // Any part that does not fit the buffer is returned to the network it came from.
            ItemStack overflow = insertIntoBuffer(buffer, pulled);
            if (!overflow.isEmpty())
                StorageNetwork.insert(level, anchor, overflow);
            int inserted = pulled.getCount() - overflow.getCount();
            moved += inserted;
            budget -= inserted;
        }
        return moved;
    }

    /** Delivers everything in {@code buffer} into a network; whatever does not fit stays buffered. */
    private static int flushBufferToNetwork(Level level, BlockPos anchor, ItemStackHandler buffer) {
        int moved = 0;
        for (int slot = 0; slot < buffer.getSlots(); slot++) {
            ItemStack stack = buffer.getStackInSlot(slot);
            if (stack.isEmpty())
                continue;
            ItemStack remainder = StorageNetwork.insert(level, anchor, stack.copy());
            buffer.setStackInSlot(slot, remainder);
            moved += stack.getCount() - remainder.getCount();
        }
        return moved;
    }

    private void emitTransferFx(ServerLevel level) {
        if (level.getGameTime() < nextTransferFxTime)
            return;
        nextTransferFxTime = level.getGameTime() + 20;
        level.playSound(null, worldPosition, CESGSounds.TRANSFER.value(), SoundSource.BLOCKS, 0.35f, 1.0f);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.65, worldPosition.getZ() + 0.5,
                4, 0.16, 0.2, 0.16, 0.025);
    }

    private static ItemStack insertIntoBuffer(ItemStackHandler buffer, ItemStack stack) {
        ItemStack remainder = stack.copy();
        for (int slot = 0; slot < buffer.getSlots() && !remainder.isEmpty(); slot++)
            remainder = buffer.insertItem(slot, remainder, false);
        return remainder;
    }

    private static boolean passesFilter(ItemStack sample, ItemStackHandler filter, boolean blacklist) {
        boolean listed = false;
        boolean anyFilter = false;
        for (int slot = 0; slot < filter.getSlots(); slot++) {
            ItemStack f = filter.getStackInSlot(slot);
            if (f.isEmpty())
                continue;
            anyFilter = true;
            if (ItemStack.isSameItemSameComponents(f, sample)) {
                listed = true;
                break;
            }
        }
        if (blacklist)
            return !listed; // empty blacklist -> everything passes
        return anyFilter && listed; // empty whitelist -> nothing passes
    }

    // ---- Terminal actions on the remote section (immediate, both sides loaded) --------------------

    /**
     * Tells the player why a terminal transfer did nothing. Without this the click is silently ignored,
     * which reads as a broken terminal — the gateway can look perfectly healthy (green, fuelled) and
     * still refuse, because a Gateway Flux Battery on the ring holds back the travel reserve.
     */
    private static void notifyLocked(ServerPlayer player) {
        if (player != null)
            player.displayClientMessage(Component.translatable("cesg.network.remote.locked"), true);
    }

    private static void notifyGated(ServerPlayer player) {
        if (player != null)
            player.displayClientMessage(Component.translatable("cesg.network.remote.unfuelled"), true);
    }

    /**
     * Terminal withdraw: pull up to {@code count} of {@code sample} out of the REMOTE network to hand to
     * the player. Fuel is charged only on a successful extract (bounced back to the remote net if the
     * gateway can't pay), so a click that finds nothing — or an unfuelled gateway — costs nothing.
     * Returns the extracted stack ({@link ItemStack#EMPTY} if nothing moved).
     */
    public ItemStack terminalWithdrawRemote(ItemStack sample, int count, ServerPlayer player) {
        if (!(level instanceof ServerLevel serverLevel) || sample.isEmpty() || count <= 0)
            return ItemStack.EMPTY;
        CrossDimensionalGatewayCoreBlockEntity core = GatewayFuelHandler.findCore(level, worldPosition);
        if (core == null || !core.canTravel())
            return ItemStack.EMPTY;
        RemoteEndpoint remote = resolveRemote(serverLevel);
        if (remote == null)
            return ItemStack.EMPTY;
        if (partnerLocked(remote)) {
            notifyLocked(player);
            return ItemStack.EMPTY;
        }
        ItemStack pulled = StorageNetwork.extract(remote.level, remote.anchor, sample, count);
        if (pulled.isEmpty())
            return ItemStack.EMPTY;
        if (!core.tryConsumeAutomationFuel(CESGConfig.bridgeTransferCost())) {
            StorageNetwork.insert(remote.level, remote.anchor, pulled); // unfuelled: put it back
            notifyGated(player);
            return ItemStack.EMPTY;
        }
        invalidateRemoteSnapshot();
        return pulled;
    }

    /**
     * Terminal deposit: push {@code stack} from the player into the REMOTE network. Fuel is charged up
     * front (an insert can't be cleanly rolled back), so an unfuelled gateway rejects the deposit
     * outright rather than moving items for free. Returns whatever did not fit.
     */
    public ItemStack terminalDepositRemote(ItemStack stack, ServerPlayer player) {
        if (!(level instanceof ServerLevel serverLevel) || stack.isEmpty())
            return stack;
        CrossDimensionalGatewayCoreBlockEntity core = GatewayFuelHandler.findCore(level, worldPosition);
        if (core == null || !core.canTravel())
            return stack;
        RemoteEndpoint remote = resolveRemote(serverLevel);
        if (remote == null)
            return stack;
        if (!core.tryConsumeAutomationFuel(CESGConfig.bridgeTransferCost())) {
            notifyGated(player);
            return stack;
        }
        ItemStack remainder = StorageNetwork.insert(remote.level, remote.anchor, stack.copy());
        if (remainder.getCount() < stack.getCount())
            invalidateRemoteSnapshot();
        return remainder;
    }

    // ---- Direction / filter configuration --------------------------------------------------------

    public boolean isPushEnabled() {
        return pushEnabled;
    }

    public boolean isPullEnabled() {
        return pullEnabled;
    }

    public void setPushEnabled(boolean value) {
        pushEnabled = value;
        contentsChanged();
    }

    public void setPullEnabled(boolean value) {
        pullEnabled = value;
        contentsChanged();
    }

    public void togglePushEnabled() {
        setPushEnabled(!pushEnabled);
    }

    public void togglePullEnabled() {
        setPullEnabled(!pullEnabled);
    }

    /**
     * Whether this Bridge refuses to let the far side take items out of its network. Pull reaches across
     * the gateway and extracts from the partner, so by default any bound gateway with pull enabled can
     * help itself to this network's contents. Locking is the per-node opt-out: it blocks extraction
     * through this Bridge — passive pull and manual terminal withdrawals alike — while still allowing
     * this side to push out, and still allowing the far side to deposit in and to view the contents.
     * Other Bridges elsewhere are unaffected.
     */
    public boolean isLocked() {
        return locked;
    }

    public void toggleLocked() {
        locked = !locked;
        contentsChanged();
    }

    /** Push filter (local → partner). Ghost items backing the terminal-independent auto-transfer. */
    public ItemStackHandler getSendFilter() {
        return sendFilter;
    }

    /** Pull filter (partner → local). */
    public ItemStackHandler getPullFilter() {
        return pullFilter;
    }

    public boolean isSendBlacklist() {
        return sendBlacklist;
    }

    public boolean isPullBlacklist() {
        return pullBlacklist;
    }

    public void toggleSendBlacklist() {
        sendBlacklist = !sendBlacklist;
        contentsChanged();
    }

    public void togglePullBlacklist() {
        pullBlacklist = !pullBlacklist;
        contentsChanged();
    }

    // ---- Helpers ---------------------------------------------------------------------------------

    private static boolean isEmpty(ItemStackHandler handler) {
        return countItems(handler) == 0;
    }

    private static int countItems(ItemStackHandler handler) {
        int total = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++)
            total += handler.getStackInSlot(slot).getCount();
        return total;
    }

    /** Buffered (in-transit) items dropped when the block is broken — the anti-void guarantee. */
    public List<ItemStack> bufferedContents() {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStackHandler buffer : new ItemStackHandler[] { outBuffer, inBuffer })
            for (int slot = 0; slot < buffer.getSlots(); slot++)
                if (!buffer.getStackInSlot(slot).isEmpty())
                    out.add(buffer.getStackInSlot(slot));
        return out;
    }

    // ---- Persistence + sync ----------------------------------------------------------------------

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("OutBuffer", outBuffer.serializeNBT(registries));
        tag.put("InBuffer", inBuffer.serializeNBT(registries));
        tag.put("SendFilter", sendFilter.serializeNBT(registries));
        tag.put("PullFilter", pullFilter.serializeNBT(registries));
        tag.putBoolean("SendBlacklist", sendBlacklist);
        tag.putBoolean("PullBlacklist", pullBlacklist);
        tag.putBoolean("PushEnabled", pushEnabled);
        tag.putBoolean("PullEnabled", pullEnabled);
        tag.putBoolean("Locked", locked);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        outBuffer.deserializeNBT(registries, tag.getCompound("OutBuffer"));
        inBuffer.deserializeNBT(registries, tag.getCompound("InBuffer"));
        sendFilter.deserializeNBT(registries, tag.getCompound("SendFilter"));
        pullFilter.deserializeNBT(registries, tag.getCompound("PullFilter"));
        sendBlacklist = tag.getBoolean("SendBlacklist");
        pullBlacklist = tag.getBoolean("PullBlacklist");
        pushEnabled = tag.getBoolean("PushEnabled");
        pullEnabled = tag.getBoolean("PullEnabled");
        locked = tag.getBoolean("Locked");
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        CESGLang.forGoggles(tooltip, "cesg.goggles.bridge.title", ChatFormatting.WHITE);
        ChatFormatting statusColor = switch (remoteStatus) {
            case LIVE -> ChatFormatting.GREEN;
            case FAULT -> ChatFormatting.RED;
            case OFFLINE -> ChatFormatting.GRAY;
        };
        CESGLang.forGoggles(tooltip, "cesg.goggles.bridge.status." + remoteStatus.name().toLowerCase(), statusColor);
        if (locked)
            CESGLang.forGoggles(tooltip, "cesg.goggles.bridge.locked", ChatFormatting.GOLD);
        CESGLang.forGoggles(tooltip, "cesg.goggles.bridge.transit", ChatFormatting.AQUA,
                countItems(outBuffer), countItems(inBuffer));
        return true;
    }
}
