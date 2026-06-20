package com.cesg.storage.station;

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.gui.AllIcons;

public enum StationFullnessMode implements INamedIconOptions {
    ALL_SLOTS(AllIcons.I_3x3, "cesg.station.fullness.all_slots"),
    SLOT_THRESHOLD(AllIcons.I_TARGET, "cesg.station.fullness.slot_threshold");

    private final AllIcons icon;
    private final String translationKey;

    StationFullnessMode(AllIcons icon, String translationKey) {
        this.icon = icon;
        this.translationKey = translationKey;
    }

    @Override
    public AllIcons getIcon() {
        return icon;
    }

    @Override
    public String getTranslationKey() {
        return translationKey;
    }
}
