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
        BlockPos anchor = bridges.get(0).worldPosition;
        if (!StorageNetwork.scan(partnerLevel, anchor).operational()) {
            remoteStatus = RemoteStatus.FAULT;
            return null;
        }
        remoteStatus = RemoteStatus.LIVE;
        return new RemoteEndpoint(partnerLevel, anchor);
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
        return level != null && level.isClientSide && getBlockState().hasProperty(StorageBridgeBlock.STATUS)
                ? getBlockState().getValue(StorageBridgeBlock.STATUS) : remoteStatus;
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

    /** On-demand server diagnostics. Simulated insertion never reserves fuel or extracts items. */
    public TransferDiagnostic transferDiagnostic(boolean push) {
        if (!(level instanceof ServerLevel server)) return TransferDiagnostic.IDLE;
        ItemStackHandler buffer = push ? outBuffer : inBuffer;
        boolean enabled = push ? pushEnabled : pullEnabled;
        if (!enabled && isEmpty(buffer)) return TransferDiagnostic.DISABLED;
        var local = StorageNetwork.scan(level, worldPosition);
        if (!local.operational()) return TransferDiagnostic.NETWORK_UNAVAILABLE;
        if (local.limited()) return TransferDiagnostic.SCAN_LIMIT;
        // Incoming items already paid for the crossing; report their local delivery first.
        if (!push && !isEmpty(inBuffer)) {
            for (int i = 0; i < inBuffer.getSlots(); i++)
                if (canInsert(local, inBuffer.getStackInSlot(i))) return TransferDiagnostic.READY;
            return TransferDiagnostic.DESTINATION_FULL;
        }
        var core = GatewayFuelHandler.findCore(level, worldPosition);
        if (core == null || core.getSpeed() == 0) return TransferDiagnostic.GATEWAY_INACTIVE;
        boolean routed = push && core.isRouteMode();
        RemoteEndpoint active = routed ? null : resolvePartner(server, core.getPartner());
        if (!routed && active == null) return TransferDiagnostic.PARTNER_OFFLINE;
        var source = push ? local : StorageNetwork.scan(active.level, active.anchor);
        if (!source.operational()) return TransferDiagnostic.NETWORK_UNAVAILABLE;
        if (source.limited()) return TransferDiagnostic.SCAN_LIMIT;
        if (!push && partnerLocked(active)) return TransferDiagnostic.PARTNER_LOCKED;
        List<ItemStack> candidates = new ArrayList<>();
        if (push) for (int i = 0; i < outBuffer.getSlots(); i++) {
            ItemStack held = outBuffer.getStackInSlot(i);
            if (!held.isEmpty()) candidates.add(held);
        }
        if (enabled) for (ItemStack sample : extractableSamples(source)) {
            if (push && !net.neoforged.neoforge.items.ItemHandlerHelper.insertItemStacked(
                    outBuffer, sample.copyWithCount(1), true).isEmpty()) continue;
            if (routed || passesFilter(sample, push ? sendFilter : pullFilter,
                    push ? sendBlacklist : pullBlacklist)) candidates.add(sample);
        }
        if (candidates.isEmpty()) return TransferDiagnostic.IDLE;
        TransferDiagnostic failure = null;
        for (ItemStack sample : candidates) {
            int channel = routed ? core.routeChannel(sample) : core.getActiveChannel();
            TransferDiagnostic result;
            if (channel < 0) result = TransferDiagnostic.NO_ROUTE;
            else {
                var binding = core.getBinding(channel);
                var remote = routed ? resolvePartner(server, binding) : active;
                if (remote == null) result = TransferDiagnostic.PARTNER_OFFLINE;
                else {
                    var destination = push ? StorageNetwork.scan(remote.level, remote.anchor) : local;
                    if (!destination.operational()) result = TransferDiagnostic.NETWORK_UNAVAILABLE;
                    else if (destination.limited()) result = TransferDiagnostic.SCAN_LIMIT;
                    else if (core.protectsAutomationReserve(CESGConfig.bridgeTransferCost(), core.isCrossDimension(binding)))
                        result = TransferDiagnostic.PROTECTED_RESERVE;
                    else if (!core.canAutomateTo(binding, CESGConfig.bridgeTransferCost()))
                        result = TransferDiagnostic.INSUFFICIENT_FUEL;
                    else result = canInsert(destination, sample) ? TransferDiagnostic.READY : TransferDiagnostic.DESTINATION_FULL;
                }
            }
            // Any usable route wins. Otherwise enum order defines deterministic precedence.
            if (result == TransferDiagnostic.READY) return result;
            if (failure == null || result.ordinal() < failure.ordinal()) failure = result;
        }
        return failure;
    }

    private static List<ItemStack> extractableSamples(StorageNetwork.Scan source) {
        var samples = new it.unimi.dsi.fastutil.objects.ObjectLinkedOpenCustomHashSet<ItemStack>(
                net.minecraft.world.item.ItemStackLinkedSet.TYPE_AND_TAG);
        for (var handler : source.handlers()) for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack sample = handler.extractItem(slot, 1, true);
            if (!sample.isEmpty()) samples.add(sample);
        }
        return new ArrayList<>(samples);
    }

    private static boolean canInsert(StorageNetwork.Scan destination, ItemStack sample) {
        if (sample.isEmpty()) return false;
        for (var handler : destination.handlers())
            if (net.neoforged.neoforge.items.ItemHandlerHelper.insertItemStacked(handler,
                    sample.copyWithCount(1), true).isEmpty()) return true;
        return false;
    }

    // ---- Passive bidirectional transfer (D3) -----------------------------------------------------

    /** One passive-transfer attempt; returns the delay (ticks) until the next attempt. */
    private int flushPassive() {
        if (!(level instanceof ServerLevel serverLevel))
            return FLUSH_INTERVAL;
        // These items have already crossed the gateway. Deliver locally even if the link is now down.
        int moved = flushBufferToNetwork(serverLevel, worldPosition, inBuffer);
        if (!pushEnabled && !pullEnabled && isEmpty(outBuffer)) {
            if (moved > 0)
                emitTransferFx(serverLevel);
            return FLUSH_INTERVAL;
        }
        CrossDimensionalGatewayCoreBlockEntity core = GatewayFuelHandler.findCore(level, worldPosition);
        if (core == null || core.getSpeed() == 0)
            return IDLE_BACKOFF;

        RemoteEndpoint remote = resolveStatus(serverLevel);
        boolean routing = core.isRouteMode();
        int cost = CESGConfig.bridgeTransferCost();
        AutomationFuelBudget fuel = core.automationFuelBudget(cost);
        int max = CESGConfig.bridgeMaxItemsPerFlush();
        if (pushEnabled && (routing || sendBlacklist || !isEmpty(sendFilter))) {
            fillBufferFromNetwork(serverLevel, worldPosition, outBuffer, max, sample -> {
                int channel = routing ? core.routeChannel(sample) : core.getActiveChannel();
                return channel >= 0 && core.canAutomateTo(core.getBinding(channel), cost)
                        && (routing || passesFilter(sample, sendFilter, sendBlacklist));
            });
        }
        if (routing)
            moved += flushBufferRouted(serverLevel, core, outBuffer, fuel);
        else if (remote != null && !isEmpty(outBuffer))
            moved += fuel.transfer(core.isCrossDimension(core.getPartner()),
                    () -> flushBufferToNetwork(remote.level, remote.anchor, outBuffer));

        if (remote != null && pullEnabled && (pullBlacklist || !isEmpty(pullFilter)) && !partnerLocked(remote))
            moved += fuel.transfer(core.isCrossDimension(core.getPartner()),
                    () -> fillBufferFromNetwork(remote.level, remote.anchor, inBuffer, max,
                            sample -> passesFilter(sample, pullFilter, pullBlacklist)));
        moved += flushBufferToNetwork(serverLevel, worldPosition, inBuffer);
        if (moved > 0)
            emitTransferFx(serverLevel);
        return FLUSH_INTERVAL;
    }

    /** Delivers each buffered stack to the partner network of the channel whose filter accepts it. */
    private int flushBufferRouted(ServerLevel level, CrossDimensionalGatewayCoreBlockEntity core,
            ItemStackHandler buffer, AutomationFuelBudget fuel) {
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
            final int bufferSlot = slot;
            moved += fuel.transfer(core.isCrossDimension(core.getBinding(channel)), () -> {
                ItemStack remainder = StorageNetwork.insert(remote.level, remote.anchor, stack.copy());
                buffer.setStackInSlot(bufferSlot, remainder);
                return stack.getCount() - remainder.getCount();
            });
        }
        return moved;
    }

    /** Stages up to max items, reserving space before each source extraction. */
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
            // Reserve a concrete buffer slot BEFORE extracting. No rollback into a filtered source:
            // a source may allow extraction while refusing the exact same item on reinsertion.
            for (int slot = 0; slot < buffer.getSlots() && budget > 0; slot++) {
                ItemStack held = buffer.getStackInSlot(slot);
                if (!held.isEmpty() && !ItemStack.isSameItemSameComponents(held, sample))
                    continue;
                int room = Math.min(buffer.getSlotLimit(slot), sample.getMaxStackSize()) - held.getCount();
                if (room <= 0)
                    continue;
                ItemStack pulled = StorageNetwork.extract(level, anchor, sample, Math.min(room, budget));
                if (pulled.isEmpty())
                    break;
                ItemStack combined = pulled.copyWithCount(held.getCount() + pulled.getCount());
                buffer.setStackInSlot(slot, combined);
                moved += pulled.getCount();
                budget -= pulled.getCount();
            }
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
     * the player. Fuel is reserved before extraction and refunded if nothing is found. No items
     * need to be returned to a source that may refuse reinsertion.
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
        int cost = CESGConfig.bridgeTransferCost();
        boolean crossDim = core.isCrossDimension(core.getPartner());
        if (!core.tryConsumeAutomationFuel(cost, crossDim)) {
            notifyGated(player);
            return ItemStack.EMPTY;
        }
        ItemStack pulled = StorageNetwork.extract(remote.level, remote.anchor, sample, count);
        if (pulled.isEmpty()) {
            core.refundAutomationFuel(cost, crossDim);
            return ItemStack.EMPTY;
        }
        invalidateRemoteSnapshot();
        return pulled;
    }

    /**
     * Terminal deposit: push {@code stack} from the player into the REMOTE network. Fuel is charged up
     * front (an insert can't be cleanly rolled back), so an unfuelled gateway rejects the deposit
     * outright rather than moving items for free. A full destination refunds the reservation.
     * Returns whatever did not fit.
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
        int cost = CESGConfig.bridgeTransferCost();
        boolean crossDim = core.isCrossDimension(core.getPartner());
        if (!core.tryConsumeAutomationFuel(cost, crossDim)) {
            notifyGated(player);
            return stack;
        }
        ItemStack remainder = StorageNetwork.insert(remote.level, remote.anchor, stack.copy());
        if (remainder.getCount() < stack.getCount())
            invalidateRemoteSnapshot();
        else
            core.refundAutomationFuel(cost, crossDim);
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
        RemoteStatus status = remoteStatus();
        ChatFormatting statusColor = switch (status) {
            case LIVE -> ChatFormatting.GREEN;
            case FAULT -> ChatFormatting.RED;
            case OFFLINE -> ChatFormatting.GRAY;
        };
        CESGLang.forGoggles(tooltip, "cesg.goggles.bridge.status." + status.getSerializedName(), statusColor);
        if (locked)
            CESGLang.forGoggles(tooltip, "cesg.goggles.bridge.locked", ChatFormatting.GOLD);
        CESGLang.forGoggles(tooltip, "cesg.goggles.bridge.transit", ChatFormatting.AQUA,
                countItems(outBuffer), countItems(inBuffer));
        return true;
    }
}
