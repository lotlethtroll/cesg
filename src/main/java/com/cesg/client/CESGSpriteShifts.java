package com.cesg.client;

import com.cesg.CESG;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;

/**
 * Connected-texture sprite shifts for CESG blocks that need Create RECTANGLE CT sheets
 * (base 16×16 + {@code *_connected} atlas).
 */
public final class CESGSpriteShifts {
    private CESGSpriteShifts() {}

    public static final CTSpriteShiftEntry GATEWAY_FLUX_BATTERY = rect("gateway_flux_battery");
    public static final CTSpriteShiftEntry GATEWAY_FLUX_BATTERY_TOP = rect("gateway_flux_battery_top");
    public static final CTSpriteShiftEntry GATEWAY_FLUX_BATTERY_INNER = rect("gateway_flux_battery_inner");

    private static CTSpriteShiftEntry rect(String name) {
        return CTSpriteShifter.getCT(AllCTTypes.RECTANGLE,
                CESG.id("block/" + name),
                CESG.id("block/" + name + "_connected"));
    }
}
