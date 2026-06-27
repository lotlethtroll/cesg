package com.cesg.storage.loader;

import java.util.List;

import com.cesg.init.CESGBlockEntities;
import com.cesg.storage.ShulkerInventoryAccess;
import com.cesg.storage.station.AbstractShulkerStationBlockEntity;
import com.cesg.storage.station.FilterGoggleTooltip;
import com.cesg.storage.station.StationFullnessMode;
import com.cesg.storage.station.StationGoggleTooltip;
import com.cesg.storage.station.StationRetentionMode;
import com.cesg.storage.util.ShulkerContentsHandler;
import com.cesg.util.CESGLang;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

public class ShulkerLoaderBlockEntity extends AbstractShulkerStationBlockEntity {
    private final ShulkerContentsHandler fillHandler = new ShulkerContentsHandler();

    public ShulkerLoaderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        fillHandler.setTransferBudget(transferBudget());
    }

    public ShulkerLoaderBlockEntity(BlockPos pos, BlockState state) {
        this(CESGBlockEntities.SHULKER_LOADER.get(), pos, state);
    }

    @Override
    protected void syncItemFilterToHandlers() {
        fillHandler.setItemFilter(itemFilterPredicate());
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
    protected boolean meetsEjectCondition() {
        if (getHeldShulker().isEmpty())
            return false;

        return switch (getFullnessMode()) {
            case ALL_SLOTS -> ShulkerInventoryAccess.isFull(getHeldShulker());
            case SLOT_THRESHOLD -> ShulkerInventoryAccess.isSlotThresholdReached(getHeldShulker(), getThreshold());
        };
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        StationGoggleTooltip.appendHeldHeader(this, tooltip, "cesg.goggles.loader.station");
        CESGLang.forGoggles(tooltip, "cesg.goggles.loader.station.retention", ChatFormatting.WHITE,
                Component.translatable(getRetentionMode().getTranslationKey()));

        if (getRetentionMode() == StationRetentionMode.AUTO_EJECT) {
            CESGLang.forGoggles(tooltip, "cesg.goggles.loader.station.eject_when", ChatFormatting.WHITE,
                    Component.translatable(getFullnessMode().getTranslationKey() + ".load"));
            if (getFullnessMode() == StationFullnessMode.SLOT_THRESHOLD)
                CESGLang.forGoggles(tooltip, "cesg.goggles.loader.station.threshold_load", ChatFormatting.WHITE,
                        getThreshold());
            StationGoggleTooltip.appendEjectFunnelTooltip(this, tooltip, "cesg.goggles.loader.station");
        }

        appendFilterGoggleTooltip(tooltip, isPlayerSneaking);
        CESGLang.forGoggles(tooltip, "cesg.goggles.loader.station.config_hint", ChatFormatting.DARK_GRAY);
        return true;
    }

    private void appendFilterGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        FilterGoggleTooltip.appendStationFilter(this, tooltip, isPlayerSneaking, true);
    }
}
