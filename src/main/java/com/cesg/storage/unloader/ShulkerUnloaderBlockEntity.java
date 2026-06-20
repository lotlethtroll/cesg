package com.cesg.storage.unloader;

import java.util.List;

import com.cesg.init.CESGBlockEntities;
import com.cesg.storage.ShulkerInventoryAccess;
import com.cesg.storage.station.StationGoggleTooltip;
import com.cesg.storage.station.AbstractShulkerStationBlockEntity;
import com.cesg.storage.station.StationFullnessMode;
import com.cesg.storage.station.StationRetentionMode;
import com.cesg.storage.util.ShulkerExtractContentsHandler;
import com.cesg.util.CESGLang;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

public class ShulkerUnloaderBlockEntity extends AbstractShulkerStationBlockEntity {
    private final ShulkerExtractContentsHandler extractHandler = new ShulkerExtractContentsHandler();

    public ShulkerUnloaderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public ShulkerUnloaderBlockEntity(BlockPos pos, BlockState state) {
        this(CESGBlockEntities.SHULKER_UNLOADER.get(), pos, state);
    }

    @Override
    protected IItemHandler getDockedItemHandler() {
        return extractHandler;
    }

    @Override
    protected void bindContentsHandler(IItemHandler handler) {
        extractHandler.setDelegate(handler);
    }

    @Override
    protected void clearDockedHandlers() {
        extractHandler.setDelegate(EMPTY_HANDLER);
    }

    @Override
    protected void updateHandlerLimits() {
        if (getRetentionMode() == StationRetentionMode.AUTO_EJECT
                && getFullnessMode() == StationFullnessMode.SLOT_THRESHOLD
                && hasDockedShulker()) {
            extractHandler.setMinOccupiedSlotsForExtract(getThreshold());
        } else {
            extractHandler.setMinOccupiedSlotsForExtract(0);
        }
    }

    @Override
    protected boolean meetsEjectCondition() {
        if (heldShulker.isEmpty())
            return false;

        return switch (getFullnessMode()) {
            case ALL_SLOTS -> ShulkerInventoryAccess.isEmpty(heldShulker);
            case SLOT_THRESHOLD -> ShulkerInventoryAccess.isSlotThresholdEmptied(heldShulker, getThreshold());
        };
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        StationGoggleTooltip.appendDockedHeader(this, tooltip, "cesg.goggles.unloader.station");

        CESGLang.forGoggles(tooltip, "cesg.goggles.unloader.station.retention", ChatFormatting.WHITE,
                Component.translatable(getRetentionMode().getTranslationKey()));

        if (getRetentionMode() == StationRetentionMode.AUTO_EJECT) {
            CESGLang.forGoggles(tooltip, "cesg.goggles.unloader.station.eject_when", ChatFormatting.WHITE,
                    Component.translatable(getFullnessMode().getTranslationKey() + ".unload"));
            if (getFullnessMode() == StationFullnessMode.SLOT_THRESHOLD)
                CESGLang.forGoggles(tooltip, "cesg.goggles.unloader.station.threshold_unload", ChatFormatting.WHITE,
                        getThreshold());

            StationGoggleTooltip.appendEjectFunnelTooltip(this, tooltip, "cesg.goggles.unloader.station");
        }

        CESGLang.forGoggles(tooltip, "cesg.goggles.unloader.station.config_hint", ChatFormatting.DARK_GRAY);
        return true;
    }
}
