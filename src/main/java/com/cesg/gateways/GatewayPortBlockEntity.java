package com.cesg.gateways;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.cesg.gateways.teleport.GatewayPartner;
import com.cesg.init.CESGBlockEntities;
import com.cesg.util.CESGLang;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Phase 6A logistics endpoint. Attach next to a Gateway Frame/Core: pipes and funnels insert into the
 * port's SEND buffers, which are flushed through the gateway (while it is powered, bound, and fueled)
 * into the RECEIVE buffers of any port on the partner ring — where automation extracts them.
 *
 * <p>Send and receive are strictly separated, so two facing ports can never ping-pong the same items.
 * Transfers only commit when the partner's chunk is loaded; otherwise the send buffer simply holds
 * (buffer + retry, per the cross-dimension safety rules). Transported fluid never touches the fuel tanks.
 */
public class GatewayPortBlockEntity extends BlockEntity implements IHaveGoggleInformation {
    public static final int SLOTS = 9;
    public static final int TANK_CAPACITY = 4000;
    /** Flush cadence in ticks; also bounds the per-flush fluid quantum below. */
    private static final int FLUSH_INTERVAL = 10;
    /** Retry delay while the gateway is inactive or the partner is unloaded — no point re-scanning fast. */
    private static final int IDLE_BACKOFF = 40;
    private static final int FLUID_PER_FLUSH = 1000;
    private static final int RING_SCAN_LIMIT = 64;

    /** Client sync cadence for the goggle readout — buffers change constantly under automation. */
    private static final int SYNC_INTERVAL = 10;

    private long nextFlushTime;
    private boolean syncDirty;

    final ItemStackHandler sendItems = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            contentsChanged();
        }
    };
    final ItemStackHandler receiveItems = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            contentsChanged();
        }
    };
    final FluidTank sendTank = new FluidTank(TANK_CAPACITY) {
        @Override
        protected void onContentsChanged() {
            contentsChanged();
        }
    };
    final FluidTank receiveTank = new FluidTank(TANK_CAPACITY) {
        @Override
        protected void onContentsChanged() {
            contentsChanged();
        }
    };

    /** Goggles read the CLIENT-side copy of the buffers, so changes must be pushed, not just saved. */
    private void contentsChanged() {
        setChanged();
        syncDirty = true;
    }

    public GatewayPortBlockEntity(BlockPos pos, BlockState state) {
        super(CESGBlockEntities.GATEWAY_PORT.get(), pos, state);
    }

    /** Pipe-facing handler: insert -> send buffer, extract -> receive buffer. */
    public IItemHandler createItemHandler() {
        return new PortItemHandler(this);
    }

    public IFluidHandler createFluidHandler() {
        return new PortFluidHandler(this);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GatewayPortBlockEntity be) {
        if (be.syncDirty && level.getGameTime() % SYNC_INTERVAL == 0) {
            be.syncDirty = false;
            level.sendBlockUpdated(pos, state, state, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
        if (level.getGameTime() < be.nextFlushTime)
            return;
        be.nextFlushTime = level.getGameTime() + be.flush();
    }

    private boolean hasOutgoing() {
        if (!sendTank.isEmpty())
            return true;
        for (int slot = 0; slot < sendItems.getSlots(); slot++)
            if (!sendItems.getStackInSlot(slot).isEmpty())
                return true;
        return false;
    }

    /** Attempts a transfer; returns the delay (ticks) until the next attempt. */
    private int flush() {
        if (!(level instanceof ServerLevel serverLevel) || !hasOutgoing())
            return FLUSH_INTERVAL;
        CrossDimensionalGatewayCoreBlockEntity core = GatewayFuelHandler.findCore(level, worldPosition);
        if (core == null || !core.canTravel())
            return IDLE_BACKOFF; // no active gateway: don't re-run the ring scan at full cadence
        return core.isRouteMode() ? flushRouted(serverLevel, core) : flushActive(serverLevel, core);
    }

    /** Single-active-channel transfer (1.0/default behaviour): everything goes to the active partner. */
    private int flushActive(ServerLevel serverLevel, CrossDimensionalGatewayCoreBlockEntity core) {
        List<GatewayPortBlockEntity> targets = resolveTargets(serverLevel, core.getPartner());
        if (targets == null || targets.isEmpty())
            return IDLE_BACKOFF; // partner unloaded or portless: keep buffering, retry later

        // Automated transfer costs fuel (config; default 0). A Gateway Flux Battery on the ring gates
        // this against its reserve floor, so automation never starves player travel — see the Core.
        if (!core.tryConsumeAutomationFuel(com.cesg.CESGConfig.gatewayPortTransferCost()))
            return IDLE_BACKOFF;

        pushItems(targets);
        pushFluid(targets);
        return FLUSH_INTERVAL;
    }

    /**
     * Fan-out transfer (7B route mode): each send item goes to the first bound channel whose filter
     * accepts it; fluid follows the active channel (filters are item-only). Partner rings are resolved
     * once per flush and cached.
     */
    private int flushRouted(ServerLevel serverLevel, CrossDimensionalGatewayCoreBlockEntity core) {
        Map<Integer, List<GatewayPortBlockEntity>> targetsByChannel = new HashMap<>();
        boolean anyReachable = false;
        for (int channel = 0; channel < CrossDimensionalGatewayCoreBlockEntity.CHANNEL_COUNT; channel++) {
            GatewayPartner binding = core.getBinding(channel);
            if (!binding.isBound())
                continue;
            List<GatewayPortBlockEntity> targets = resolveTargets(serverLevel, binding);
            targetsByChannel.put(channel, targets == null ? List.of() : targets);
            anyReachable |= targets != null && !targets.isEmpty();
        }
        if (!anyReachable)
            return IDLE_BACKOFF; // no channel reachable: hold everything, no fuel spent

        if (!core.tryConsumeAutomationFuel(com.cesg.CESGConfig.gatewayPortTransferCost()))
            return IDLE_BACKOFF;

        for (int slot = 0; slot < sendItems.getSlots(); slot++) {
            ItemStack stack = sendItems.getStackInSlot(slot);
            if (stack.isEmpty())
                continue;
            int channel = core.routeChannel(stack);
            List<GatewayPortBlockEntity> targets = channel < 0 ? null : targetsByChannel.get(channel);
            if (targets == null || targets.isEmpty())
                continue; // unroutable or that channel is down: leave it buffered
            ItemStack remainder = stack.copy();
            for (GatewayPortBlockEntity target : targets) {
                remainder = ItemHandlerHelper.insertItemStacked(target.receiveItems, remainder, false);
                if (remainder.isEmpty())
                    break;
            }
            sendItems.setStackInSlot(slot, remainder);
        }

        List<GatewayPortBlockEntity> activeTargets = targetsByChannel.get(core.getActiveChannel());
        if (activeTargets != null && !activeTargets.isEmpty())
            pushFluid(activeTargets);
        return FLUSH_INTERVAL;
    }

    /**
     * Ports on {@code partner}'s ring, excluding this one. Null when the partner is unbound, unloaded,
     * or not a Core (hold + retry); an empty list means reachable but portless.
     */
    private List<GatewayPortBlockEntity> resolveTargets(ServerLevel serverLevel, GatewayPartner partner) {
        if (!partner.isBound())
            return null;
        ServerLevel partnerLevel = serverLevel.getServer().getLevel(partner.dimension());
        if (partnerLevel == null || !partnerLevel.isLoaded(partner.position()))
            return null;
        if (!(partnerLevel.getBlockEntity(partner.position()) instanceof CrossDimensionalGatewayCoreBlockEntity partnerCore))
            return null;
        List<GatewayPortBlockEntity> targets = findPorts(partnerLevel, partnerCore.getBlockPos());
        targets.remove(this); // self-bound rings must never loop items back into their own port
        return targets;
    }

    private void pushItems(List<GatewayPortBlockEntity> targets) {
        for (int slot = 0; slot < sendItems.getSlots(); slot++) {
            ItemStack stack = sendItems.getStackInSlot(slot);
            if (stack.isEmpty())
                continue;
            ItemStack remainder = stack.copy();
            for (GatewayPortBlockEntity target : targets) {
                remainder = ItemHandlerHelper.insertItemStacked(target.receiveItems, remainder, false);
                if (remainder.isEmpty())
                    break;
            }
            sendItems.setStackInSlot(slot, remainder);
        }
    }

    private void pushFluid(List<GatewayPortBlockEntity> targets) {
        if (sendTank.isEmpty())
            return;
        for (GatewayPortBlockEntity target : targets) {
            FluidStack offer = sendTank.drain(FLUID_PER_FLUSH, IFluidHandler.FluidAction.SIMULATE);
            if (offer.isEmpty())
                return;
            int accepted = target.receiveTank.fill(offer, IFluidHandler.FluidAction.EXECUTE);
            if (accepted > 0)
                sendTank.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    /** All Gateway Ports attached to the ring containing {@code corePos} (ports sit beside ring blocks). */
    private static List<GatewayPortBlockEntity> findPorts(Level level, BlockPos corePos) {
        List<GatewayPortBlockEntity> ports = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Set<BlockPos> checkedNeighbors = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(corePos.immutable());
        visited.add(corePos.immutable());
        while (!queue.isEmpty() && visited.size() <= RING_SCAN_LIMIT) {
            BlockPos pos = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                // Never step into unloaded chunks: getBlockState/getBlockEntity on a ServerLevel
                // force-load synchronously, and this walk runs on the partner dimension every flush.
                if (!level.isLoaded(next))
                    continue;
                if (checkedNeighbors.add(next)
                        && level.getBlockEntity(next) instanceof GatewayPortBlockEntity port)
                    ports.add(port);
                if (!visited.contains(next) && GatewayFuelHandler.isRingBlock(level.getBlockState(next))) {
                    visited.add(next);
                    queue.add(next);
                }
            }
        }
        return ports;
    }

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
        tag.put("SendItems", sendItems.serializeNBT(registries));
        tag.put("ReceiveItems", receiveItems.serializeNBT(registries));
        tag.put("SendTank", sendTank.writeToNBT(registries, new CompoundTag()));
        tag.put("ReceiveTank", receiveTank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        sendItems.deserializeNBT(registries, tag.getCompound("SendItems"));
        receiveItems.deserializeNBT(registries, tag.getCompound("ReceiveItems"));
        sendTank.readFromNBT(registries, tag.getCompound("SendTank"));
        receiveTank.readFromNBT(registries, tag.getCompound("ReceiveTank"));
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        CESGLang.forGoggles(tooltip, "cesg.goggles.port.title", ChatFormatting.WHITE);
        CESGLang.forGoggles(tooltip, "cesg.goggles.port.outgoing", ChatFormatting.AQUA,
                countItems(sendItems), sendTank.getFluidAmount());
        CESGLang.forGoggles(tooltip, "cesg.goggles.port.incoming", ChatFormatting.GREEN,
                countItems(receiveItems), receiveTank.getFluidAmount());
        return true;
    }

    private static int countItems(ItemStackHandler handler) {
        int total = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++)
            total += handler.getStackInSlot(slot).getCount();
        return total;
    }

    /** Slots 0-8: send (insert only). Slots 9-17: receive (extract only). */
    private record PortItemHandler(GatewayPortBlockEntity port) implements IItemHandler {
        @Override
        public int getSlots() {
            return SLOTS * 2;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot < SLOTS ? port.sendItems.getStackInSlot(slot)
                    : port.receiveItems.getStackInSlot(slot - SLOTS);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot >= SLOTS)
                return stack;
            return port.sendItems.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot < SLOTS)
                return ItemStack.EMPTY;
            return port.receiveItems.extractItem(slot - SLOTS, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot < SLOTS;
        }
    }

    /** Tank 0: send (fill only). Tank 1: receive (drain only). */
    private record PortFluidHandler(GatewayPortBlockEntity port) implements IFluidHandler {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? port.sendTank.getFluid() : port.receiveTank.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return TANK_CAPACITY;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return port.sendTank.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !FluidStack.isSameFluidSameComponents(resource, port.receiveTank.getFluid()))
                return FluidStack.EMPTY;
            return port.receiveTank.drain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return port.receiveTank.drain(maxDrain, action);
        }
    }
}
