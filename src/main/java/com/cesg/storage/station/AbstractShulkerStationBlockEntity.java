package com.cesg.storage.station;

import java.util.EnumMap;
import java.util.Map;

import com.cesg.storage.ShulkerInventoryAccess;
import com.cesg.storage.util.NotifyingItemHandler;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

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
import net.neoforged.neoforge.items.ComponentItemHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public abstract class AbstractShulkerStationBlockEntity extends KineticBlockEntity implements ShulkerStation {
    protected static final IItemHandler EMPTY_HANDLER = new ItemStackHandler(0);
    public static final int MAX_THRESHOLD = 54;

    protected ItemStack heldShulker = ItemStack.EMPTY;
    protected IItemHandler contentsHandler = EMPTY_HANDLER;
    private final IItemHandler holdingHandler;
    private final Map<Direction, IItemHandler> ejectHandlers = new EnumMap<>(Direction.class);

    protected int retentionOrdinal = StationRetentionMode.HOLD.ordinal();
    protected int fullnessOrdinal = StationFullnessMode.ALL_SLOTS.ordinal();
    protected int threshold = 27;
    /** Custom name stamped onto shulkers held here. Empty = leave the box's own name. */
    protected String stationName = "";
    public static final int MAX_NAME_LENGTH = 48;

    protected AbstractShulkerStationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        holdingHandler = new HoldingHandler(this);
        for (Direction direction : Direction.values())
            ejectHandlers.put(direction, ShulkerBoxEjectHandler.create(this, direction));
    }

    @Override
    public boolean hasHeldShulker() {
        return !heldShulker.isEmpty();
    }

    public boolean isPowered() {
        return Math.abs(getSpeed()) > 0;
    }

    public StationRetentionMode getRetentionMode() {
        return StationRetentionMode.values()[Mth.clamp(retentionOrdinal, 0, StationRetentionMode.values().length - 1)];
    }

    public StationFullnessMode getFullnessMode() {
        return StationFullnessMode.values()[Mth.clamp(fullnessOrdinal, 0, StationFullnessMode.values().length - 1)];
    }

    public int getThreshold() {
        return Mth.clamp(threshold, 1, MAX_THRESHOLD);
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
        onInventoryChanged();
    }

    @Override
    public void clearHeldShulkerAfterEject() {
        heldShulker = ItemStack.EMPTY;
        contentsHandler = EMPTY_HANDLER;
        clearHeldHandlers();
        updateHandlerLimits();
        setChanged();
        sendData();
    }

    public IItemHandler getItemHandler(Direction side) {
        if (!hasHeldShulker())
            return holdingHandler;

        if (isPowered() && canExposeShulkerForEject(side))
            return ejectHandlers.get(side);

        return getHeldItemHandler();
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
            contentsHandler = EMPTY_HANDLER;
            clearHeldHandlers();
            updateHandlerLimits();
            return;
        }

        Runnable notify = this::onInventoryChanged;
        if (ShulkerInventoryAccess.isVanillaShulker(heldShulker)) {
            contentsHandler = new ComponentItemHandler(heldShulker, DataComponents.CONTAINER, 27) {
                @Override
                protected void onContentsChanged(int slot, ItemStack oldStack, ItemStack newStack) {
                    super.onContentsChanged(slot, oldStack, newStack);
                    notify.run();
                }
            };
        } else {
            contentsHandler = new NotifyingItemHandler(ShulkerInventoryAccess.wrap(heldShulker), notify);
        }
        bindContentsHandler(contentsHandler);
        updateHandlerLimits();
    }

    protected void onInventoryChanged() {
        setChanged();
        if (level != null && !level.isClientSide)
            sendData();
    }

    protected abstract IItemHandler getHeldItemHandler();

    protected abstract void bindContentsHandler(IItemHandler contentsHandler);

    protected abstract void clearHeldHandlers();

    protected abstract void updateHandlerLimits();

    protected abstract boolean meetsEjectCondition();

    public boolean meetsEjectConditionPublic() {
        return meetsEjectCondition();
    }

    @Override
    public void tick() {
        super.tick();
        if (level != null && !level.isClientSide && heldShulker.isEmpty()) {
            contentsHandler = EMPTY_HANDLER;
            clearHeldHandlers();
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

    /** Accepts shulker boxes into the station when no box is held yet. */
    private static final class HoldingHandler implements IItemHandler {
        private final ShulkerStation station;

        private HoldingHandler(ShulkerStation station) {
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
