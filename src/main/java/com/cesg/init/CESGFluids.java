package com.cesg.init;

import com.cesg.CESG;

import com.tterrag.registrate.util.entry.FluidEntry;

import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/**
 * Real Create-compatible fluids. Textures live under {@code textures/block/} so the vanilla block atlas
 * stitches them; each fluid auto-creates a source/flowing/block/bucket. Gateway fuels are piped/spouted
 * and poured into a Cross-Dimensional Gateway Core.
 */
public class CESGFluids {
    public static final FluidEntry<BaseFlowingFluid.Flowing> LIQUID_ENDER_PEARL = CESG.REGISTRATE
            .fluid("liquid_ender_pearl",
                    CESG.id("block/liquid_ender_pearl_still"), CESG.id("block/liquid_ender_pearl_flow"))
            .lang("Liquid Ender Pearl")
            .register();

    public static final FluidEntry<BaseFlowingFluid.Flowing> TELEPORT_ESSENCE = CESG.REGISTRATE
            .fluid("teleport_essence",
                    CESG.id("block/teleport_essence_still"), CESG.id("block/teleport_essence_flow"))
            .lang("Teleport Essence")
            .register();

    public static final FluidEntry<BaseFlowingFluid.Flowing> LIQUID_EYE_OF_ENDER = CESG.REGISTRATE
            .fluid("liquid_eye_of_ender",
                    CESG.id("block/liquid_eye_of_ender_still"), CESG.id("block/liquid_eye_of_ender_flow"))
            .lang("Liquid Eye of Ender")
            .register();

    private CESGFluids() {}

    static void register() {
        // Touching this class triggers the static fluid registrations above.
    }
}
