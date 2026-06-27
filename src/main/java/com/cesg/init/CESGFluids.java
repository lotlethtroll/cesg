package com.cesg.init;

import com.cesg.CESG;
import com.cesg.datagen.CESGPlaceholderModels;

import com.tterrag.registrate.util.entry.FluidEntry;
import com.tterrag.registrate.util.entry.ItemEntry;

import net.neoforged.neoforge.fluids.BaseFlowingFluid;

public class CESGFluids {
    public static final ItemEntry<net.minecraft.world.item.Item> TELEPORT_ESSENCE_BUCKET;
    public static final ItemEntry<net.minecraft.world.item.Item> LIQUID_EYE_OF_ENDER_BUCKET;

    /** Real fluid: crush ender pearls to dust, mix with water in a heated basin, then spout onto shells. */
    public static final FluidEntry<BaseFlowingFluid.Flowing> LIQUID_ENDER_PEARL = CESG.REGISTRATE
            .standardFluid("liquid_ender_pearl")
            .lang("Liquid Ender Pearl")
            .register();

    static {
        TELEPORT_ESSENCE_BUCKET = CESG.REGISTRATE.item("teleport_essence_bucket",
                        p -> new net.minecraft.world.item.Item(p.craftRemainder(net.minecraft.world.item.Items.BUCKET)))
                .lang("item.cesg.teleport_essence_bucket")
                .model(CESGPlaceholderModels::teleportEssenceBucket)
                .register();

        LIQUID_EYE_OF_ENDER_BUCKET = CESG.REGISTRATE.item("liquid_eye_of_ender_bucket",
                        p -> new net.minecraft.world.item.Item(p.craftRemainder(net.minecraft.world.item.Items.BUCKET)))
                .lang("item.cesg.liquid_eye_of_ender_bucket")
                .model(CESGPlaceholderModels::liquidEyeOfEnderBucket)
                .register();
    }

    static void register() {
        // Fluid items registered via static initializer.
    }
}
