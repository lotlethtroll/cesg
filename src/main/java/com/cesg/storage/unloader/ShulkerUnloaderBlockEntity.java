package com.cesg.storage.unloader;

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

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

public class ShulkerUnloaderBlockEntity extends AbstractShulkerStationBlockEntity {
    private final ShulkerExtractContentsHandler extractHandler = new ShulkerExtractContentsHandler();

    public ShulkerUnloaderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        extractHandler.setTransferBudget(transferBudget());
    }

    public ShulkerUnloaderBlockEntity(BlockPos pos, BlockState state) {
        this(CESGBlockEntities.SHULKER_UNLOADER.get(), pos, state);
    }

    @Override
    protected void syncItemFilterToHandlers() {
        extractHandler.setItemFilter(itemFilterPredicate());
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
    protected boolean meetsEjectCondition() {
        if (getHeldShulker().isEmpty())
            return false;

        return switch (getFullnessMode()) {
            case ALL_SLOTS -> ShulkerInventoryAccess.isEmpty(getHeldShulker());
            case SLOT_THRESHOLD -> ShulkerInventoryAccess.isSlotThresholdEmptied(getHeldShulker(), getThreshold());
        };
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        StationGoggleTooltip.appendStationTooltip(this, tooltip, isPlayerSneaking,
                "cesg.goggles.unloader.station", ".unload", ".threshold_unload");
        if (isPlayerSneaking) {
            appendFilterGoggleTooltip(tooltip, true);
            CESGLang.forGoggles(tooltip, "cesg.goggles.unloader.station.config_hint", ChatFormatting.DARK_GRAY);
        }
        return true;
    }

    private void appendFilterGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        FilterGoggleTooltip.appendStationFilter(this, tooltip, isPlayerSneaking, false);
    }
}
