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
    protected final ShulkerDockingHandler dockingHandler = new ShulkerDockingHandler(this);
    protected final Map<Direction, IItemHandler> ejectHandlers = new EnumMap<>(Direction.class);

    protected int retentionOrdinal = StationRetentionMode.HOLD.ordinal();
    protected int fullnessOrdinal = StationFullnessMode.ALL_SLOTS.ordinal();
    protected int threshold = 27;

    protected AbstractShulkerStationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        for (Direction direction : Direction.values())
            ejectHandlers.put(direction, new ShulkerBoxEjectHandler(this, direction));
    }

    @Override
    public boolean hasDockedShulker() {
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

    public void applyConfig(int retention, int fullness, int threshold) {
        retentionOrdinal = Mth.clamp(retention, 0, StationRetentionMode.values().length - 1);
        fullnessOrdinal = Mth.clamp(fullness, 0, StationFullnessMode.values().length - 1);
        this.threshold = Mth.clamp(threshold, 1, MAX_THRESHOLD);
        updateHandlerLimits();
        onInventoryChanged();
    }

    @Override
    public ItemStack getHeldShulker() {
        return heldShulker;
    }

    @Override
    public void setHeldShulker(ItemStack stack) {
        this.heldShulker = stack.copy();
        rebuildContentsHandlers();
        setChanged();
        sendData();
        onInventoryChanged();
    }

    @Override
    public void clearHeldShulkerAfterEject() {
        heldShulker = ItemStack.EMPTY;
        contentsHandler = EMPTY_HANDLER;
        clearDockedHandlers();
        updateHandlerLimits();
        setChanged();
        sendData();
    }

    public IItemHandler getItemHandler(Direction side) {
        if (!hasDockedShulker())
            return dockingHandler;

        if (isPowered() && canExposeShulkerForEject(side))
            return ejectHandlers.get(side);

        return getDockedItemHandler();
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
            clearDockedHandlers();
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

    protected abstract IItemHandler getDockedItemHandler();

    protected abstract void bindContentsHandler(IItemHandler contentsHandler);

    protected abstract void clearDockedHandlers();

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
            clearDockedHandlers();
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("StationRetention", retentionOrdinal);
        tag.putInt("StationFullness", fullnessOrdinal);
        tag.putInt("StationThreshold", threshold);
        if (!heldShulker.isEmpty())
            tag.put("HeldShulker", heldShulker.save(registries));
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        retentionOrdinal = tag.contains("StationRetention") ? tag.getInt("StationRetention") : tag.getInt("DockRetention");
        fullnessOrdinal = tag.contains("StationFullness") ? tag.getInt("StationFullness") : tag.getInt("DockFullness");
        threshold = tag.contains("StationThreshold") ? tag.getInt("StationThreshold")
                : tag.contains("DockFillThreshold") ? tag.getInt("DockFillThreshold") : 27;
        applyConfig(retentionOrdinal, fullnessOrdinal, threshold);
        heldShulker = tag.contains("HeldShulker")
                ? ItemStack.parse(registries, tag.getCompound("HeldShulker")).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        rebuildContentsHandlers();
    }
}
