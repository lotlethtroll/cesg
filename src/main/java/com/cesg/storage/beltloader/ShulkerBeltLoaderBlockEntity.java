package com.cesg.storage.beltloader;

import java.util.List;

import com.cesg.init.CESGBlockEntities;
import com.cesg.storage.ShulkerInventoryAccess;
import com.cesg.storage.station.AbstractShulkerStationBlockEntity;
import com.cesg.storage.station.FilterGoggleTooltip;
import com.cesg.storage.station.StationFullnessMode;
import com.cesg.storage.station.StationGoggleTooltip;
import com.cesg.storage.station.StationRetentionMode;
import com.cesg.storage.util.BeltItemLoadingProcessing;
import com.cesg.storage.util.ShulkerContentsHandler;
import com.cesg.util.CESGLang;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import net.neoforged.neoforge.items.ItemHandlerHelper;

public class ShulkerBeltLoaderBlockEntity extends AbstractShulkerStationBlockEntity {

    /**
     * Belt-intake ONLY: docked boxes fill exclusively from the belt below — side funnels/hoppers
     * cannot insert past the intake. Docking (empty station) and finished-box eject still work.
     */
    @Override
    protected boolean exposesContentsToSides() {
        return false;
    }

    /**
     * Distance the spout drops from the nozzle ring at full extension. The loader always sits exactly
     * two blocks above the belt (Create's belt-processing rule), so a block item travelling below tops
     * out ~15px under the loader; reaching 13px (ring bottom is already 1px down) leaves the nozzle tip
     * ~1px above that item.
     */
    public static final float MAX_TUBE_REACH = 13f / 16f;
    /** ~1 second to extend on first engagement. */
    public static final int EXTEND_CYCLE = 20;
    /** ~0.8 second to retract when idle. */
    public static final int RETRACT_CYCLE = 16;
    /** 3 seconds with no matching items before retracting. */
    public static final int IDLE_RETRACT_TICKS = 60;

    private enum TubePhase {
        RETRACTED, EXTENDING, EXTENDED, RETRACTING
    }

    private final ShulkerContentsHandler fillHandler = new ShulkerContentsHandler();

    private TubePhase tubePhase = TubePhase.RETRACTED;
    private int tubeAnimTicks;
    private int prevTubeAnimTicks;
    private long lastItemActivityTick;

    public ShulkerBeltLoaderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        fillHandler.setTransferBudget(transferBudget());
    }

    public ShulkerBeltLoaderBlockEntity(BlockPos pos, BlockState state) {
        this(CESGBlockEntities.SHULKER_BELT_LOADER.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(BeltItemLoadingProcessing.create(this, this::processBeltItem));
        super.addBehaviours(behaviours);
    }

    @Override
    protected void syncItemFilterToHandlers() {
        fillHandler.setItemFilter(itemFilterPredicate());
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide)
            return;

        tickTubeAnimation();
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

    private BeltItemLoadingProcessing.ProcessingState processBeltItem(ItemStack stack, @SuppressWarnings("unused") boolean _continuing) {
        if (!wantsFromBelt(stack))
            return BeltItemLoadingProcessing.ProcessingState.PASS;

        markItemActivity();
        engageTube();

        if (!canLoadNow(stack))
            return BeltItemLoadingProcessing.ProcessingState.HOLD;

        // Wait only for the initial extend animation; after that insert immediately each tick
        if (tubePhase == TubePhase.EXTENDING)
            return BeltItemLoadingProcessing.ProcessingState.HOLD;

        int inserted = insertIntoShulker(stack);
        if (inserted > 0) {
            stack.shrink(inserted);
            syncTubeState();
        }

        if (stack.isEmpty())
            return BeltItemLoadingProcessing.ProcessingState.REMOVE;

        return BeltItemLoadingProcessing.ProcessingState.HOLD;
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

    private boolean wantsFromBelt(ItemStack stack) {
        return !stack.isEmpty() && filtering.test(stack);
    }

    private boolean canLoadNow(ItemStack stack) {
        if (!isPowered() || !hasHeldShulker() || stack.isEmpty())
            return false;
        if (meetsEjectCondition())
            return false;
        return maxInsertable(stack) > 0;
    }

    private int maxInsertable(ItemStack stack) {
        ItemStack probe = stack.copy();
        ItemStack remainder = ItemHandlerHelper.insertItemStacked(fillHandler, probe, true);
        return stack.getCount() - remainder.getCount();
    }

    private int insertIntoShulker(ItemStack stack) {
        ItemStack toInsert = stack.copy();
        ItemStack remainder = ItemHandlerHelper.insertItemStacked(fillHandler, toInsert, false);
        return stack.getCount() - remainder.getCount();
    }

    public boolean isTubeVisible() {
        return tubePhase != TubePhase.RETRACTED;
    }

    /**
     * Ponder-only: drive the hose extension directly (0 = retracted, 1 = fully extended). The normal
     * animation advances on the server tick, which Ponder's client-side scene level never runs, so scenes
     * step this each keyframe to show the tube extend/retract.
     */
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
            case RETRACTED -> 0;
            case EXTENDED -> 1;
            case EXTENDING -> {
                float ticks = Mth.lerp(partialTicks, prevTubeAnimTicks, tubeAnimTicks);
                float t = Mth.clamp(ticks / EXTEND_CYCLE, 0, 1);
                yield t * t * (3 - 2 * t);
            }
            case RETRACTING -> {
                float ticks = Mth.lerp(partialTicks, prevTubeAnimTicks, tubeAnimTicks);
                float t = 1 - Mth.clamp(ticks / RETRACT_CYCLE, 0, 1);
                yield t * t * (3 - 2 * t);
            }
        };
    }

    @Override
    protected AABB createRenderBoundingBox() {
        // Tube hangs 1px (ring) + MAX_TUBE_REACH below the block; pad a little for the cap rounding.
        float padding = 2f / 16f;
        return new AABB(worldPosition).expandTowards(0, -(MAX_TUBE_REACH + padding), 0);
    }

    @Override
    protected IItemHandler getHeldItemHandler() {
        return fillHandler;
    }

    @Override
    protected void bindContentsHandler(IItemHandler handler) {
        fillHandler.setDelegate(handler);
    }

    @Override
    protected void clearHeldHandlers() {
        fillHandler.setDelegate(emptyHandler());
    }

    @Override
    protected void updateHandlerLimits() {
        if (getRetentionMode() == StationRetentionMode.AUTO_EJECT
                && getFullnessMode() == StationFullnessMode.SLOT_THRESHOLD
                && hasHeldShulker()) {
            int slots = ShulkerInventoryAccess.getSlotCount(getHeldShulker());
            fillHandler.setMaxInsertSlotExclusive(Math.min(getThreshold(), slots));
        } else {
            fillHandler.setMaxInsertSlotExclusive(Integer.MAX_VALUE);
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
            case ALL_SLOTS -> ShulkerInventoryAccess.isFull(getHeldShulker());
            case SLOT_THRESHOLD -> ShulkerInventoryAccess.isSlotThresholdReached(getHeldShulker(), getThreshold());
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
                "cesg.goggles.loader.station", ".load", ".threshold_load");
        if (isPlayerSneaking) {
            CESGLang.forGoggles(tooltip, "cesg.goggles.belt_loader.placement", ChatFormatting.GRAY);
            FilterGoggleTooltip.appendStationFilter(this, tooltip, true, true);
            CESGLang.forGoggles(tooltip, "cesg.goggles.loader.station.config_hint", ChatFormatting.DARK_GRAY);
        }
        return true;
    }
}
