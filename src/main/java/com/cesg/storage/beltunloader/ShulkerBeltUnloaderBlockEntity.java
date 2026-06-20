package com.cesg.storage.beltunloader;

import java.util.List;

import com.cesg.init.CESGBlockEntities;
import com.cesg.storage.ShulkerInventoryAccess;
import com.cesg.storage.util.BeltShulkerProcessing;
import com.cesg.storage.util.SideInventoryAccess;
import com.cesg.util.CESGLang;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public class ShulkerBeltUnloaderBlockEntity extends KineticBlockEntity {
    public static final int EXTEND_CYCLE = 8;
    public static final int RETRACT_CYCLE = 8;
    /**
     * Distance the spout drops from the block bottom at full extension. The unloader always sits
     * exactly two blocks above the belt (Create's belt-processing rule), so a shulker travelling
     * below tops out ~15px down; reaching ~13px leaves the nozzle mouth just above it.
     */
    public static final float MAX_TUBE_REACH = 13f / 16f;

    private enum TubePhase { RETRACTED, EXTENDING, EXTENDED, RETRACTING }

    private int processingTicks;
    private TubePhase tubePhase = TubePhase.RETRACTED;
    private int tubeAnimTicks;
    private int prevTubeAnimTicks;

    public ShulkerBeltUnloaderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public ShulkerBeltUnloaderBlockEntity(BlockPos pos, BlockState state) {
        this(CESGBlockEntities.SHULKER_BELT_UNLOADER.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        behaviours.add(BeltShulkerProcessing.create(this, this::processShulker));
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide)
            return;
        tickTubeAnimation();
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
            sendData();
        } else if (tubePhase == TubePhase.RETRACTING) {
            prevTubeAnimTicks = tubeAnimTicks;
            tubeAnimTicks++;
            if (tubeAnimTicks >= RETRACT_CYCLE) {
                tubePhase = TubePhase.RETRACTED;
                tubeAnimTicks = 0;
                prevTubeAnimTicks = 0;
            }
            sendData();
        }
    }

    private BeltProcessingBehaviour.ProcessingResult processShulker(ItemStack shulker, boolean continuing) {
        if (getSpeed() == 0 || !ShulkerInventoryAccess.isShulkerBox(shulker))
            return passAndRetract();

        if (ShulkerInventoryAccess.isEmpty(shulker))
            return passAndRetract();

        if (!continuing) {
            processingTicks = 0;
            engageTube();
            return BeltProcessingBehaviour.ProcessingResult.HOLD;
        }

        processingTicks++;
        if (processingTicks < EXTEND_CYCLE)
            return BeltProcessingBehaviour.ProcessingResult.HOLD;

        processingTicks = 0;
        IItemHandler sink = SideInventoryAccess.getAttachedInventory(level, worldPosition, getOutputFacing());
        if (sink == null)
            return passAndRetract();

        IItemHandler shulkerHandler = ShulkerInventoryAccess.wrap(shulker);
        boolean moved = false;
        for (int slot = 0; slot < shulkerHandler.getSlots(); slot++) {
            ItemStack extracted = shulkerHandler.extractItem(slot, 64, false);
            if (extracted.isEmpty())
                continue;
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(sink, extracted, false);
            if (!remainder.isEmpty())
                shulkerHandler.insertItem(slot, remainder, false);
            moved = true;
            break;
        }

        if (!moved || ShulkerInventoryAccess.isEmpty(shulker))
            return passAndRetract();

        return BeltProcessingBehaviour.ProcessingResult.HOLD;
    }

    private BeltProcessingBehaviour.ProcessingResult passAndRetract() {
        beginRetracting();
        return BeltProcessingBehaviour.ProcessingResult.PASS;
    }

    private void engageTube() {
        if (tubePhase == TubePhase.RETRACTED) {
            tubePhase = TubePhase.EXTENDING;
            prevTubeAnimTicks = tubeAnimTicks = 0;
            sendData();
        } else if (tubePhase == TubePhase.RETRACTING) {
            tubePhase = TubePhase.EXTENDED;
            prevTubeAnimTicks = tubeAnimTicks = 0;
            sendData();
        }
    }

    private void beginRetracting() {
        if (tubePhase == TubePhase.RETRACTED || tubePhase == TubePhase.RETRACTING)
            return;
        tubePhase = TubePhase.RETRACTING;
        prevTubeAnimTicks = tubeAnimTicks = 0;
        sendData();
    }

    private Direction getOutputFacing() {
        return getBlockState().getValue(DirectionalKineticBlock.FACING).getCounterClockWise();
    }

    public boolean isTubeVisible() {
        return tubePhase != TubePhase.RETRACTED;
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
        // Tube + nozzle hang up to MAX_TUBE_REACH below the block; pad a little for the cap rounding.
        float padding = 2f / 16f;
        return new AABB(worldPosition).expandTowards(0, -(MAX_TUBE_REACH + padding), 0);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("TubePhase", tubePhase.ordinal());
        tag.putInt("TubeAnimTicks", tubeAnimTicks);
        tag.putInt("PrevTubeAnimTicks", prevTubeAnimTicks);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        tubePhase = TubePhase.values()[Mth.clamp(tag.getInt("TubePhase"), 0, TubePhase.values().length - 1)];
        tubeAnimTicks = tag.getInt("TubeAnimTicks");
        prevTubeAnimTicks = tag.contains("PrevTubeAnimTicks") ? tag.getInt("PrevTubeAnimTicks") : tubeAnimTicks;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        CESGLang.forGoggles(tooltip, "cesg.goggles.belt_unloader", ChatFormatting.AQUA);
        return true;
    }
}
