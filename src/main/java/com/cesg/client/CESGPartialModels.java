package com.cesg.client;

import com.cesg.CESG;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;

public final class CESGPartialModels {
    /** Wide brass nozzle head (item-sized mouth) capping the loader/unloader spout. */
    public static final PartialModel EXTRACTOR_NOZZLE = PartialModel.of(CESG.id("block/belt_extractor_nozzle"));
    /** Teeth-only cog for the gateway core interior (no hub — shaft passes through center). */
    public static final PartialModel GATEWAY_CORE_COG = PartialModel.of(CESG.id("block/gateway_core_cog"));

    private CESGPartialModels() {}

    public static void init() {
        // Eager-load so Flywheel's partial registry includes these models before baking.
        EXTRACTOR_NOZZLE.modelLocation();
        GATEWAY_CORE_COG.modelLocation();
    }
}
