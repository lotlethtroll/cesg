package com.cesg.storage.station;

import com.cesg.storage.ShulkerInventoryAccess;
import com.cesg.upgrades.EnhancedShulkerContents;
import com.cesg.upgrades.EnhancedShulkerItemStackHandler;
import com.cesg.storage.util.ItemHandlerUnwrap;
import com.cesg.storage.util.NotifyingComponentItemHandler;
import com.cesg.storage.util.NotifyingItemHandler;
import com.cesg.storage.util.TransferBudget;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;
import java.util.function.Predicate;

public abstract class AbstractShulkerStationBlockEntity extends KineticBlockEntity implements ShulkerStation {
    private static final ItemStackHandler EMPTY = new ItemStackHandler(0);
    /** Largest enhanced shulker (tier 4); slot-threshold config clamps to the held box when smaller. */
    public static final int MAX_THRESHOLD = EnhancedShulkerContents.forTier(EnhancedShulkerContents.MAX_TIER).slotCount();

    /**
     * Rotation speed (|RPM|) that grants one item of throughput per tick. Throughput scales linearly
     * with speed, mirroring how Create machines run faster the more they are driven.
     */
    private static final double SPEED_PER_ITEM_PER_TICK = 16.0;
    /** Caps banked throughput so an idle-but-powered station cannot release an unbounded burst. */
    private static final double MAX_TRANSFER_BUDGET = 64.0;

    private double transferAccumulator;
    private final TransferBudget budgetView = new TransferBudget() {
        @Override
        public int available() {
            return (int) transferAccumulator;
        }

        @Override
        public void consume(int amount) {
            if (amount > 0)
                transferAccumulator = Math.max(0, transferAccumulator - amount);
        }
    };

    private ItemStack heldShulker = ItemStack.EMPTY;
    private IItemHandler contentsHandler = emptyHandler();
    private final HoldingHandler holdingHandler;
    private final ShulkerBoxEjectHandler[] ejectHandlers = new ShulkerBoxEjectHandler[6];
    /**
     * Stable per-side capability instances handed to funnels. They dispatch to the correct internal
     * handler (holding / fill / eject) live on every call, so the exposed handler can change with state
     * without needing capability invalidation - the cached instance the funnel holds never goes stale.
     */
    private final SideDispatchHandler[] sideHandlers = new SideDispatchHandler[6];
    private final SideDispatchHandler nullSideHandler;
    /** Tracks whether the held box is currently ejectable, so we only invalidate caps on a real flip. */
    private boolean ejectExposureLatch;

    protected int retentionOrdinal = StationRetentionMode.HOLD.ordinal();
    protected int fullnessOrdinal = StationFullnessMode.ALL_SLOTS.ordinal();
    protected int threshold = 27;
    /** Custom name stamped onto shulkers held here. Empty = leave the box's own name. */
    protected String stationName = "";
    public FilteringBehaviour filtering;

    protected AbstractShulkerStationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        holdingHandler = new HoldingHandler(this);
        for (Direction direction : Direction.values()) {
            ejectHandlers[direction.ordinal()] = new ShulkerBoxEjectHandler(this, direction);
            sideHandlers[direction.ordinal()] = new SideDispatchHandler(direction);
        }
        nullSideHandler = new SideDispatchHandler(null);
    }

    public static final int MAX_NAME_LENGTH = 48;

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(filtering = new FilteringBehaviour(this, new MultiFaceFilterSlotPositioning())
                .withPredicate(stack -> !ShulkerInventoryAccess.isShulkerBox(stack))
                .withCallback(stack -> syncItemFilterToHandlers()));
        super.addBehaviours(behaviours);
    }

    @Override
    public void initialize() {
        super.initialize();
        syncItemFilterToHandlers();
    }

    protected Predicate<ItemStack> itemFilterPredicate() {
        return stack -> filtering == null || filtering.test(stack);
    }

    /** Pushes the shared filter predicate into loader/unloader item handlers. */
    protected abstract void syncItemFilterToHandlers();

    @Override
    public boolean hasHeldShulker() {
        return !heldShulker.isEmpty();
    }

    public boolean isPowered() {
        return Math.abs(getSpeed()) > 0;
    }

    /** Per-tick throughput allowance shared with the content handlers; scales with rotation speed. */
    protected TransferBudget transferBudget() {
        return budgetView;
    }

    private void accrueTransferBudget() {
        if (!isPowered()) {
            transferAccumulator = 0;
            return;
        }
        transferAccumulator = Math.min(MAX_TRANSFER_BUDGET,
                transferAccumulator + Math.abs(getSpeed()) / SPEED_PER_ITEM_PER_TICK);
    }

    public StationRetentionMode getRetentionMode() {
        return StationRetentionMode.values()[Mth.clamp(retentionOrdinal, 0, StationRetentionMode.values().length - 1)];
    }

    public StationFullnessMode getFullnessMode() {
        return StationFullnessMode.values()[Mth.clamp(fullnessOrdinal, 0, StationFullnessMode.values().length - 1)];
    }

    public int getThreshold() {
        int raw = Mth.clamp(threshold, 1, MAX_THRESHOLD);
        if (hasHeldShulker())
            return ShulkerInventoryAccess.clampThreshold(heldShulker, raw);
        return raw;
    }

    public String getStationName() {
        return stationName;
    }

    public void applyConfig(int retention, int fullness, int threshold, String name) {
        retentionOrdinal = Mth.clamp(retention, 0, StationRetentionMode.values().length - 1);
        fullnessOrdinal = Mth.clamp(fullness, 0, StationFullnessMode.values().length - 1);
        this.threshold = Mth.clamp(threshold, 1, MAX_THRESHOLD);
        this.stationName = sanitizeName(name);
        applyNameToHeldShulker();
        updateHandlerLimits();
        onInventoryChanged();
    }

    private static String sanitizeName(String name) {
        if (name == null)
            return "";
        return name.length() > MAX_NAME_LENGTH ? name.substring(0, MAX_NAME_LENGTH) : name;
    }

    /** Stamps (or clears) the held shulker's display name to match {@link #stationName}. */
    protected void applyNameToHeldShulker() {
        if (heldShulker.isEmpty())
            return;
        if (stationName.isEmpty())
            heldShulker.remove(DataComponents.CUSTOM_NAME);
        else
            heldShulker.set(DataComponents.CUSTOM_NAME, Component.literal(stationName));
    }

    @Override
    public ItemStack getHeldShulker() {
        return heldShulker;
    }

    @Override
    public void setHeldShulker(ItemStack stack) {
        this.heldShulker = stack.copy();
        applyNameToHeldShulker();
        rebuildContentsHandlers();
        setChanged();
        sendData();
        // Holding -> fill swap: tell cached funnels to re-resolve so they stop trying to insert boxes.
        invalidateCapabilities();
        onInventoryChanged();
    }

    @Override
    public void clearHeldShulkerAfterEject() {
        heldShulker = ItemStack.EMPTY;
        contentsHandler = emptyHandler();
        clearHeldHandlers();
        updateHandlerLimits();
        ejectExposureLatch = false;
        setChanged();
        sendData();
        // Eject -> holding swap: cached funnels must re-resolve to accept the next box.
        invalidateCapabilities();
    }

    /** Stable capability instance for {@code side}; safe to cache because it dispatches live. */
    IItemHandler resolveItemHandler(Direction side) {
        return side == null ? nullSideHandler : sideHandlers[side.ordinal()];
    }

    /** Chooses the live handler for the requested side based on the station's current state. */
    private IItemHandler activeHandler(Direction side) {
        if (!hasHeldShulker())
            return holdingHandler;

        if (side != null && isPowered() && canExposeShulkerForEject(side))
            return ejectHandlers[side.ordinal()];

        return getHeldItemHandler();
    }

    protected static IItemHandler emptyHandler() {
        return EMPTY;
    }

    @Override
    public boolean canExposeShulkerForEject(Direction side) {
        if (level == null)
            return false;
        if (getRetentionMode() != StationRetentionMode.AUTO_EJECT)
            return false;
        if (!meetsEjectCondition())
            return false;
        return StationFunnelConnection.hasOutputFunnel(level, worldPosition, side);
    }

    protected void rebuildContentsHandlers() {
        if (heldShulker.isEmpty() || !ShulkerInventoryAccess.isShulkerBox(heldShulker)) {
            contentsHandler = emptyHandler();
            clearHeldHandlers();
            updateHandlerLimits();
            return;
        }

        Runnable notify = this::onInventoryChanged;
        if (ShulkerInventoryAccess.isVanillaShulker(heldShulker)) {
            contentsHandler = new NotifyingComponentItemHandler(
                    heldShulker, DataComponents.CONTAINER, 27, notify);
        } else {
            contentsHandler = new NotifyingItemHandler(ShulkerInventoryAccess.wrap(heldShulker, level), notify);
        }
        bindContentsHandler(contentsHandler);
        updateHandlerLimits();
        syncItemFilterToHandlers();
    }

    protected void onInventoryChanged() {
        setChanged();
        if (level != null && !level.isClientSide)
            sendData();
    }

    protected abstract IItemHandler getHeldItemHandler();

    protected abstract void bindContentsHandler(IItemHandler handler);

    protected abstract void clearHeldHandlers();

    protected abstract void updateHandlerLimits();

    protected abstract boolean meetsEjectCondition();

    /** Inserts into the held shulker's contents inventory (used by belt unloaders). */
    protected void insertIntoContents(int slot, ItemStack stack, boolean simulate) {
        contentsHandler.insertItem(slot, stack, simulate);
    }

    public boolean meetsEjectConditionPublic() {
        return meetsEjectCondition();
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide)
            return;
        accrueTransferBudget();
        if (heldShulker.isEmpty()) {
            contentsHandler = emptyHandler();
            clearHeldHandlers();
        } else {
            tickHeldShulkerCompacting();
        }
        refreshEjectExposure();
    }

    /** Drains compacting backlog on docked enhanced shulkers between automation inserts. */
    private void tickHeldShulkerCompacting() {
        EnhancedShulkerItemStackHandler handler = ItemHandlerUnwrap.enhancedShulkerHandler(getHeldItemHandler());
        if (handler != null)
            handler.compactAutomationPass();
    }

    /**
     * The fill -> full transition (and power changes) happen with no block update, so a funnel that
     * already polled the station and saw nothing to extract would never re-check. Invalidate the
     * exposed capability the moment the box becomes (or stops being) ejectable so funnels re-resolve.
     */
    private void refreshEjectExposure() {
        boolean ejectable = hasHeldShulker() && isPowered() && meetsEjectCondition();
        if (ejectable != ejectExposureLatch) {
            ejectExposureLatch = ejectable;
            invalidateCapabilities();
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("StationRetention", retentionOrdinal);
        tag.putInt("StationFullness", fullnessOrdinal);
        tag.putInt("StationThreshold", threshold);
        tag.putString("StationName", stationName);
        if (!heldShulker.isEmpty())
            tag.put("HeldShulker", heldShulker.save(registries));
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        retentionOrdinal = tag.getInt("StationRetention");
        fullnessOrdinal = tag.getInt("StationFullness");
        threshold = tag.contains("StationThreshold") ? tag.getInt("StationThreshold") : 27;
        stationName = tag.getString("StationName");
        applyConfig(retentionOrdinal, fullnessOrdinal, threshold, stationName);
        heldShulker = tag.contains("HeldShulker")
                ? ItemStack.parse(registries, tag.getCompound("HeldShulker")).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        rebuildContentsHandlers();
    }

    /**
     * Forwards every call to whichever internal handler is currently active for its side. Funnels cache
     * this single instance, so state-driven handler swaps (holding -> fill -> eject) take effect without
     * any capability invalidation.
     */
    private final class SideDispatchHandler implements IItemHandler {
        private final Direction side;

        private SideDispatchHandler(Direction side) {
            this.side = side;
        }

        private IItemHandler active() {
            return activeHandler(side);
        }

        @Override
        public int getSlots() {
            return active().getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return active().getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return active().insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return active().extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return active().getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return active().isItemValid(slot, stack);
        }
    }

    /** Accepts shulker boxes into the station when no box is held yet. */
    public static final class HoldingHandler implements IItemHandler {
        private final ShulkerStation station;

        HoldingHandler(ShulkerStation station) {
            this.station = station;
        }

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != 0 || stack.isEmpty() || station.hasHeldShulker())
                return stack;
            if (!ShulkerInventoryAccess.isShulkerBox(stack))
                return stack;

            if (!simulate)
                station.setHeldShulker(stack.copyWithCount(1));

            return stack.getCount() <= 1 ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - 1);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return ShulkerInventoryAccess.isShulkerBox(stack);
        }
    }
}
