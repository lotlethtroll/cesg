package com.cesg.storage.station;

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.gui.AllIcons;

public enum StationRetentionMode implements INamedIconOptions {
    HOLD(AllIcons.I_PASSIVE, "cesg.station.retention.hold"),
    AUTO_EJECT(AllIcons.I_MOVE_PLACE, "cesg.station.retention.auto_eject");

    private final AllIcons icon;
    private final String translationKey;

    StationRetentionMode(AllIcons icon, String translationKey) {
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
