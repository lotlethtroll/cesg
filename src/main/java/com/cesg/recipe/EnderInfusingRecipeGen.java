package com.cesg.recipe;

import java.util.List;

import com.cesg.CESG;
import com.cesg.init.CESGFluids;
import com.cesg.init.CESGRegistration;
import com.simibubi.create.AllFluids;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/**
 * Datagen for {@code cesg:ender_infusing} recipes: a fluid + zero or more item catalysts -> a fluid. Seeds
 * the ender fuel chain (incl. a vanilla-Water step) and a two-catalyst Create Chocolate conversion. Append
 * CESG/Create/vanilla conversions here.
 */
public final class EnderInfusingRecipeGen {
    private static final TagKey<Fluid> MILK = TagKey.create(Registries.FLUID,
            ResourceLocation.fromNamespaceAndPath("c", "milk"));

    private EnderInfusingRecipeGen() {}

    public static void generate(RecipeOutput output) {
        // Vanilla Water + Ender Pearl Dust -> Liquid Ender Pearl
        infusing(output, "liquid_ender_pearl",
                SizedFluidIngredient.of(Fluids.WATER, 250),
                List.of(catalyst(CESGRegistration.ENDER_PEARL_DUST.get())),
                CESGFluids.LIQUID_ENDER_PEARL.getSource(), 250, 8);

        // Liquid Ender Pearl + Chorus Fruit -> Teleport Essence
        infusing(output, "teleport_essence",
                SizedFluidIngredient.of(CESGFluids.LIQUID_ENDER_PEARL.getSource(), 250),
                List.of(catalyst(Items.CHORUS_FRUIT)),
                CESGFluids.TELEPORT_ESSENCE.getSource(), 250, 8);

        // Teleport Essence + Blaze Powder -> Liquid Eye of Ender
        infusing(output, "liquid_eye_of_ender",
                SizedFluidIngredient.of(CESGFluids.TELEPORT_ESSENCE.getSource(), 250),
                List.of(catalyst(Items.BLAZE_POWDER)),
                CESGFluids.LIQUID_EYE_OF_ENDER.getSource(), 250, 8);

        // Create-style Chocolate: 250mB Milk + Sugar + Cocoa Beans -> 250mB Chocolate (two catalysts).
        infusing(output, "chocolate",
                SizedFluidIngredient.of(MILK, 250),
                List.of(catalyst(Items.SUGAR), catalyst(Items.COCOA_BEANS)),
                AllFluids.CHOCOLATE.getSource(), 250, 8);

        // --- Reverse conversions: no forward catalyst, and the catalyst is handed back as a byproduct.
        //     When an input fluid matches both a forward and a reverse recipe, the block entity prefers
        //     the one needing the most catalysts: load the catalyst to go forward, leave it empty to reverse.

        // Liquid Eye of Ender -> Teleport Essence + Blaze Powder (reclaim)
        infusing(output, "reclaim_essence",
                SizedFluidIngredient.of(CESGFluids.LIQUID_EYE_OF_ENDER.getSource(), 250),
                List.of(),
                CESGFluids.TELEPORT_ESSENCE.getSource(), 250,
                List.of(new ItemStack(Items.BLAZE_POWDER)), 12);

        // Teleport Essence -> Liquid Ender Pearl + Chorus Fruit
        infusing(output, "reverse_teleport_essence",
                SizedFluidIngredient.of(CESGFluids.TELEPORT_ESSENCE.getSource(), 250),
                List.of(),
                CESGFluids.LIQUID_ENDER_PEARL.getSource(), 250,
                List.of(new ItemStack(Items.CHORUS_FRUIT)), 12);

        // Liquid Ender Pearl -> Water + Ender Pearl Dust
        infusing(output, "reverse_liquid_ender_pearl",
                SizedFluidIngredient.of(CESGFluids.LIQUID_ENDER_PEARL.getSource(), 250),
                List.of(),
                Fluids.WATER, 250,
                List.of(new ItemStack(CESGRegistration.ENDER_PEARL_DUST.get())), 12);

        // Chocolate -> Milk + Sugar + Cocoa Beans
        infusing(output, "reverse_chocolate",
                SizedFluidIngredient.of(AllFluids.CHOCOLATE.getSource(), 250),
                List.of(),
                NeoForgeMod.MILK.get(), 250,
                List.of(new ItemStack(Items.SUGAR), new ItemStack(Items.COCOA_BEANS)), 12);
    }

    private static SizedIngredient catalyst(ItemLike item) {
        return new SizedIngredient(Ingredient.of(item), 1);
    }

    private static void infusing(RecipeOutput output, String name, SizedFluidIngredient in,
            List<SizedIngredient> catalysts, Fluid resultFluid, int resultAmount, int time) {
        output.accept(CESG.id("ender_infusing/" + name),
                EnderInfusingRecipe.of(in, catalysts, resultFluid, resultAmount, time), (AdvancementHolder) null);
    }

    private static void infusing(RecipeOutput output, String name, SizedFluidIngredient in,
            List<SizedIngredient> catalysts, Fluid resultFluid, int resultAmount,
            List<ItemStack> resultItems, int time) {
        output.accept(CESG.id("ender_infusing/" + name),
                EnderInfusingRecipe.of(in, catalysts, resultFluid, resultAmount, resultItems, time),
                (AdvancementHolder) null);
    }
}
