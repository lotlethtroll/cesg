package com.cesg.storage.beltunloader;

import java.util.List;

import com.cesg.init.CESGBlockEntities;
import com.cesg.storage.ShulkerInventoryAccess;
import com.cesg.storage.station.AbstractShulkerStationBlockEntity;
import com.cesg.storage.station.FilterGoggleTooltip;
import com.cesg.storage.station.StationFullnessMode;
import com.cesg.storage.station.StationGoggleTooltip;
import com.cesg.storage.station.StationRetentionMode;
import com.cesg.storage.util.ShulkerExtractContentsHandler;
import com.cesg.util.CESGLang;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Mirror image of the Shulker Belt Loader: holds a shulker box, then pays its contents out onto the
 * belt two blocks below (everything, or only items matching the front filter). Auto-eject ejects the
 * emptied shulker through an output funnel exactly like the loader ejects a filled one.
 */
public class ShulkerBeltUnloaderBlockEntity extends AbstractShulkerStationBlockEntity {
    /**
     * Distance the spout drops from the block bottom at full extension. The unloader sits exactly two
     * blocks above the belt (Create's belt-processing placement rule), so the nozzle tip lands just
     * above the belt surface where it drops items.
     */
    public static final float MAX_TUBE_REACH = 13f / 16f;
    /** ~1 second to extend on first engagement. */
    public static final int EXTEND_CYCLE = 20;
    /** ~0.8 second to retract when idle. */
    public static final int RETRACT_CYCLE = 16;
    /** 3 seconds with no unloading activity before retracting. */
    public static final int IDLE_RETRACT_TICKS = 60;

    private enum TubePhase {
        RETRACTED, EXTENDING, EXTENDED, RETRACTING
    }

    private final ShulkerExtractContentsHandler extractHandler = new ShulkerExtractContentsHandler();

    private TubePhase tubePhase = TubePhase.RETRACTED;
    private int tubeAnimTicks;
    private int prevTubeAnimTicks;
    private long lastItemActivityTick;

    public ShulkerBeltUnloaderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        extractHandler.setTransferBudget(transferBudget());
    }

    public ShulkerBeltUnloaderBlockEntity(BlockPos pos, BlockState state) {
        this(CESGBlockEntities.SHULKER_BELT_UNLOADER.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
    }

    @Override
    protected void syncItemFilterToHandlers() {
        extractHandler.setItemFilter(itemFilterPredicate());
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide)
            return;

        tickTubeAnimation();
        tickUnload();
        tickIdleRetract();
    }

    private void tickTubeAnimation() {
        if (tubePhase == TubePhase.EXTENDING) {
            prevTubeAnimTicks = tubeAnimTicks;
            tubeAnimTicks++;
            if (tubeAnimTicks >= EXTEND_CYCLE) {
                tubePhase = TubePhase.EXTENDED;
                tubeAnimTicks = 0;
                prevTubeAnimTicks = 0;
            }
            syncTubeState();
        } else if (tubePhase == TubePhase.RETRACTING) {
            prevTubeAnimTicks = tubeAnimTicks;
            tubeAnimTicks++;
            if (tubeAnimTicks >= RETRACT_CYCLE) {
                tubePhase = TubePhase.RETRACTED;
                tubeAnimTicks = 0;
                prevTubeAnimTicks = 0;
            }
            syncTubeState();
        }
    }

    private void tickIdleRetract() {
        if (tubePhase != TubePhase.EXTENDED && tubePhase != TubePhase.EXTENDING)
            return;
        if (level.getGameTime() - lastItemActivityTick < IDLE_RETRACT_TICKS)
            return;
        beginRetracting();
    }

    private void tickUnload() {
        if (!hasHeldShulker())
            return;

        DirectBeltInputBehaviour belt = getTargetBelt();
        if (belt == null)
            return;

        int slot = findUnloadableSlot();
        if (slot < 0)
            return;

        // A matching item is queued: reach for the belt even before power lets us actually pay out.
        markItemActivity();
        engageTube();

        if (!isPowered())
            return;
        if (tubePhase == TubePhase.EXTENDING)
            return;

        pushOntoBelt(belt, slot);
    }

    private DirectBeltInputBehaviour getTargetBelt() {
        if (level == null)
            return null;
        return BlockEntityBehaviour.get(level, worldPosition.below(2), DirectBeltInputBehaviour.TYPE);
    }

    /** First shulker slot holding a filter-matching item that the eject threshold still lets us extract. */
    private int findUnloadableSlot() {
        for (int slot = 0; slot < extractHandler.getSlots(); slot++) {
            ItemStack inSlot = extractHandler.getStackInSlot(slot);
            if (inSlot.isEmpty() || !filtering.test(inSlot))
                continue;
            if (extractHandler.extractItem(slot, 1, true).isEmpty())
                continue;
            return slot;
        }
        return -1;
    }

    private void pushOntoBelt(DirectBeltInputBehaviour belt, int slot) {
        ItemStack inSlot = extractHandler.getStackInSlot(slot);
        if (inSlot.isEmpty() || !filtering.test(inSlot))
            return;

        int extractAmount = inSlot.getCount();
        ItemStack available = extractHandler.extractItem(slot, extractAmount, true);
        if (available.isEmpty())
            return;

        ItemStack remainder = belt.handleInsertion(available, Direction.UP, true);
        int accepted = available.getCount() - remainder.getCount();
        if (accepted <= 0)
            return; // belt segment occupied or stopped; keep the tube extended and wait

        ItemStack extracted = extractHandler.extractItem(slot, accepted, false);
        if (extracted.isEmpty())
            return;

        ItemStack leftover = belt.handleInsertion(extracted, Direction.UP, false);
        if (!leftover.isEmpty())
            insertIntoContents(slot, leftover, false); // belt refused after all; put it back

        onInventoryChanged();
        syncTubeState();
    }

    private void engageTube() {
        if (tubePhase == TubePhase.RETRACTED) {
            tubePhase = TubePhase.EXTENDING;
            prevTubeAnimTicks = tubeAnimTicks = 0;
            level.playSound(null, worldPosition, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.3f, 1.35f);
            syncTubeState();
        } else if (tubePhase == TubePhase.RETRACTING) {
            tubePhase = TubePhase.EXTENDED;
            prevTubeAnimTicks = tubeAnimTicks = 0;
            syncTubeState();
        }
    }

    private void beginRetracting() {
        if (tubePhase == TubePhase.RETRACTED || tubePhase == TubePhase.RETRACTING)
            return;
        tubePhase = TubePhase.RETRACTING;
        prevTubeAnimTicks = tubeAnimTicks = 0;
        level.playSound(null, worldPosition, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.25f, 1.45f);
        syncTubeState();
    }

    private void markItemActivity() {
        if (level != null)
            lastItemActivityTick = level.getGameTime();
    }

    private void syncTubeState() {
        if (level != null && !level.isClientSide)
            sendData();
    }

    public boolean isTubeVisible() {
        return tubePhase != TubePhase.RETRACTED;
    }

    /** Ponder-only: drive the hose extension directly (0 = retracted, 1 = extended); see belt loader BE. */
    public void ponderSetTubeProgress(float progress) {
        progress = Mth.clamp(progress, 0f, 1f);
        if (progress <= 0f) {
            tubePhase = TubePhase.RETRACTED;
        } else if (progress >= 1f) {
            tubePhase = TubePhase.EXTENDED;
        } else {
            tubePhase = TubePhase.EXTENDING;
            tubeAnimTicks = prevTubeAnimTicks = Math.round(progress * EXTEND_CYCLE);
        }
    }

    public float getTubeExtension(float partialTicks) {
        return switch (tubePhase) {
            case RETRACTED -> 0f;
            case EXTENDED -> 1f;
            case EXTENDING -> {
                float ticks = Mth.lerp(partialTicks, prevTubeAnimTicks, tubeAnimTicks);
                float t = Mth.clamp(ticks / EXTEND_CYCLE, 0f, 1f);
                yield t * t * (3f - 2f * t);
            }
            case RETRACTING -> {
                float ticks = Mth.lerp(partialTicks, prevTubeAnimTicks, tubeAnimTicks);
                float t = 1f - Mth.clamp(ticks / RETRACT_CYCLE, 0f, 1f);
                yield t * t * (3f - 2f * t);
            }
        };
    }

    @Override
    protected AABB createRenderBoundingBox() {
        float padding = 2f / 16f;
        return new AABB(worldPosition).expandTowards(0, -(MAX_TUBE_REACH + padding), 0);
    }

    /**
     * The tube IS the output: docked contents leave exclusively through the bottom onto the belt.
     * Side automation can still dock boxes and pull the finished (ejectable) box, but never drain
     * the contents sideways past the tube.
     */
    @Override
    protected boolean exposesContentsToSides() {
        return false;
    }

    @Override
    protected IItemHandler getHeldItemHandler() {
        return extractHandler;
    }

    @Override
    protected void bindContentsHandler(IItemHandler handler) {
        extractHandler.setDelegate(handler);
    }

    @Override
    protected void clearHeldHandlers() {
        extractHandler.setDelegate(emptyHandler());
    }

    @Override
    protected void updateHandlerLimits() {
        if (getRetentionMode() == StationRetentionMode.AUTO_EJECT
                && getFullnessMode() == StationFullnessMode.SLOT_THRESHOLD
                && hasHeldShulker()) {
            extractHandler.setMinOccupiedSlotsForExtract(getThreshold());
        } else {
            extractHandler.setMinOccupiedSlotsForExtract(0);
        }
    }

    @Override
    protected void onInventoryChanged() {
        super.onInventoryChanged();
        syncTubeState();
    }

    @Override
    protected boolean meetsEjectCondition() {
        if (getHeldShulker().isEmpty())
            return false;

        return switch (getFullnessMode()) {
            case ALL_SLOTS -> ShulkerInventoryAccess.isEmpty(getHeldShulker());
            case SLOT_THRESHOLD -> ShulkerInventoryAccess.isSlotThresholdEmptied(getHeldShulker(), getThreshold());
        };
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("TubePhase", tubePhase.ordinal());
        tag.putInt("TubeAnimTicks", tubeAnimTicks);
        tag.putInt("PrevTubeAnimTicks", prevTubeAnimTicks);
        tag.putLong("LastItemActivity", lastItemActivityTick);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        tubePhase = TubePhase.values()[Mth.clamp(tag.getInt("TubePhase"), 0, TubePhase.values().length - 1)];
        tubeAnimTicks = tag.getInt("TubeAnimTicks");
        prevTubeAnimTicks = tag.contains("PrevTubeAnimTicks") ? tag.getInt("PrevTubeAnimTicks") : tubeAnimTicks;
        lastItemActivityTick = tag.getLong("LastItemActivity");
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        StationGoggleTooltip.appendStationTooltip(this, tooltip, isPlayerSneaking,
                "cesg.goggles.unloader.station", ".unload", ".threshold_unload");
        if (isPlayerSneaking) {
            CESGLang.forGoggles(tooltip, "cesg.goggles.belt_unloader.placement", ChatFormatting.GRAY);
            FilterGoggleTooltip.appendStationFilter(this, tooltip, true, false);
            CESGLang.forGoggles(tooltip, "cesg.goggles.unloader.station.config_hint", ChatFormatting.DARK_GRAY);
        }
        return true;
    }
}
