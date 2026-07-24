package com.cesg.storage.network;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import com.cesg.init.CESGRegistration;
import com.cesg.storage.station.AbstractShulkerStationBlock;
import com.cesg.upgrades.EnhancedShulkerBoxBlock;

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * Phase 6D connection model: a storage network is the set of member blocks (controller, terminals,
 * enhanced shulker boxes, shulker stations) transitively touching each other — block adjacency, no
 * cables. A network is only operational while it contains at least one Storage Network Controller.
 *
 * <p>Aggregation is computed on demand (terminal open / periodic refresh) rather than incrementally;
 * with the scan cap that is far simpler and fast enough, and it can never go stale.
 */
public final class StorageNetwork {
    /** Upper bound on member blocks per network, to bound the BFS. */
    public static final int SCAN_LIMIT = 256;

    private StorageNetwork() {}

    public record Scan(List<IItemHandler> handlers, boolean hasController, int memberCount) {
        public boolean operational() {
            return hasController;
        }
    }

    public static boolean isMember(BlockState state) {
        return state.is(CESGRegistration.STORAGE_NETWORK_CONTROLLER.get())
                || state.is(CESGRegistration.STORAGE_TERMINAL.get())
                || state.is(CESGRegistration.STORAGE_BRIDGE.get())
                || state.getBlock() instanceof EnhancedShulkerBoxBlock
                || state.getBlock() instanceof AbstractShulkerStationBlock
                || state.getBlock() instanceof com.cesg.storage.enderbarrel.EnderBarrelBlock;
    }

    /**
     * Cluster membership (the BFS) is cached briefly per anchor: the walk touches up to 6x256 block
     * states, and open terminals rescan every 10 ticks. Handlers are NOT cached — enhanced-shulker
     * handlers snapshot contents at wrap time, so they must be created fresh per operation.
     */
    private static final int CLUSTER_CACHE_TICKS = 20;
    private static final java.util.Map<net.minecraft.core.GlobalPos, CachedCluster> CLUSTER_CACHE =
            new java.util.HashMap<>();

    private record CachedCluster(long expiresAt, List<BlockPos> members, boolean hasController, int memberCount) {}

    /** Walks (or recalls) the member cluster containing {@code start} and collects fresh item handlers. */
    public static Scan scan(Level level, BlockPos start) {
        net.minecraft.core.GlobalPos key = net.minecraft.core.GlobalPos.of(level.dimension(), start.immutable());
        long now = level.getGameTime();
        CachedCluster cluster = CLUSTER_CACHE.get(key);
        if (cluster == null || now >= cluster.expiresAt()) {
            cluster = walkCluster(level, start, now);
            CLUSTER_CACHE.values().removeIf(entry -> now >= entry.expiresAt());
            CLUSTER_CACHE.put(key, cluster);
        }
        List<IItemHandler> handlers = new ArrayList<>(cluster.members().size());
        Set<java.util.UUID> seenPairs = new HashSet<>();
        for (BlockPos pos : cluster.members()) {
            IItemHandler handler = memberHandler(level, pos, seenPairs);
            if (handler != null)
                handlers.add(handler);
        }
        return new Scan(handlers, cluster.hasController(), cluster.memberCount());
    }

    private static CachedCluster walkCluster(Level level, BlockPos start, long now) {
        List<BlockPos> members = new ArrayList<>();
        boolean hasController = false;
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        long expiry = now + CLUSTER_CACHE_TICKS;
        if (!isMember(level.getBlockState(start)))
            return new CachedCluster(expiry, List.of(), false, 0);
        queue.add(start.immutable());
        visited.add(start.immutable());
        while (!queue.isEmpty() && visited.size() <= SCAN_LIMIT) {
            BlockPos pos = queue.poll();
            BlockState state = level.getBlockState(pos);
            if (state.is(CESGRegistration.STORAGE_NETWORK_CONTROLLER.get()))
                hasController = true;
            members.add(pos);
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (!visited.contains(next) && isMember(level.getBlockState(next))) {
                    visited.add(next.immutable());
                    queue.add(next.immutable());
                }
            }
        }
        return new CachedCluster(expiry, members, hasController, visited.size());
    }

    /**
     * Placed enhanced shulkers keep their inventory inside the block entity's item stack, so they are
     * wrapped directly (with a change listener persisting the BE); everything else goes through the
     * regular block item-handler capability.
     */
    private static IItemHandler memberHandler(Level level, BlockPos pos, Set<java.util.UUID> seenPairs) {
        // Ender Barrel twins share ONE pool — index each pair id once, or the network would
        // double-count (and double-serve) the same inventory when both twins touch the cluster.
        if (level.getBlockEntity(pos) instanceof com.cesg.storage.enderbarrel.EnderBarrelBlockEntity barrel) {
            var pairId = barrel.getPairId();
            if (pairId == null || !seenPairs.add(pairId))
                return null;
        }
        // Everything else (stations, enhanced shulkers, barrels) goes through the block capability;
        // the enhanced-shulker handler already re-wraps per change and goes inert while viewed.
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
    }

    /**
     * Member block positions of the cluster containing {@code start} (reuses the brief BFS cache).
     * Handy for finding typed members — e.g. Storage Bridges — without re-walking per lookup.
     */
    public static List<BlockPos> memberPositions(Level level, BlockPos start) {
        net.minecraft.core.GlobalPos key = net.minecraft.core.GlobalPos.of(level.dimension(), start.immutable());
        long now = level.getGameTime();
        CachedCluster cluster = CLUSTER_CACHE.get(key);
        if (cluster == null || now >= cluster.expiresAt()) {
            cluster = walkCluster(level, start, now);
            CLUSTER_CACHE.values().removeIf(entry -> now >= entry.expiresAt());
            CLUSTER_CACHE.put(key, cluster);
        }
        return cluster.members();
    }

    /** Aggregated view: one entry per distinct item+components, summed across the network. */
    public static Object2IntMap<ItemStack> aggregate(Scan scan) {
        Object2IntMap<ItemStack> totals = new Object2IntLinkedOpenCustomHashMap<>(ItemStackLinkedSet.TYPE_AND_TAG);
        for (IItemHandler handler : scan.handlers()) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (stack.isEmpty())
                    continue;
                totals.mergeInt(stack.copyWithCount(1), stack.getCount(), Integer::sum);
            }
        }
        return totals;
    }

    /** Pulls up to {@code count} items matching {@code sample} out of the network. */
    public static ItemStack extract(Level level, BlockPos anchor, ItemStack sample, int count) {
        Scan scan = scan(level, anchor);
        if (!scan.operational() || sample.isEmpty() || count <= 0)
            return ItemStack.EMPTY;
        int remaining = Math.min(count, sample.getMaxStackSize());
        int extracted = 0;
        for (IItemHandler handler : scan.handlers()) {
            for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
                ItemStack inSlot = handler.getStackInSlot(slot);
                if (inSlot.isEmpty() || !ItemStack.isSameItemSameComponents(inSlot, sample))
                    continue;
                ItemStack pulled = handler.extractItem(slot, remaining, false);
                extracted += pulled.getCount();
                remaining -= pulled.getCount();
            }
            if (remaining <= 0)
                break;
        }
        return extracted == 0 ? ItemStack.EMPTY : sample.copyWithCount(extracted);
    }

    /** Distributes {@code stack} into the network; returns what did not fit. */
    public static ItemStack insert(Level level, BlockPos anchor, ItemStack stack) {
        Scan scan = scan(level, anchor);
        if (!scan.operational() || stack.isEmpty())
            return stack;
        ItemStack remainder = stack;
        for (IItemHandler handler : scan.handlers()) {
            remainder = ItemHandlerHelper.insertItemStacked(handler, remainder, false);
            if (remainder.isEmpty())
                break;
        }
        return remainder;
    }
}
