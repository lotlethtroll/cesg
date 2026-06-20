package com.cesg.datagen;

import java.util.concurrent.CompletableFuture;

import com.cesg.CESG;
import com.cesg.init.CESGFluids;
import com.cesg.init.CESGRegistration;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;

public class CESGRecipeProvider extends RecipeProvider {
    public CESGRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CESGRegistration.SHULKER_LOADER.get())
                .pattern(" C ")
                .pattern("ISI")
                .pattern(" A ")
                .define('C', AllBlocks.COGWHEEL.get())
                .define('I', AllItems.IRON_SHEET.get())
                .define('S', Items.SHULKER_SHELL)
                .define('A', AllBlocks.ANDESITE_CASING.get())
                .unlockedBy("has_shulker_shell", has(Items.SHULKER_SHELL))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CESGRegistration.SHULKER_UNLOADER.get())
                .pattern(" C ")
                .pattern("ISI")
                .pattern(" A ")
                .define('C', AllBlocks.COGWHEEL.get())
                .define('I', AllItems.IRON_SHEET.get())
                .define('S', Items.SHULKER_SHELL)
                .define('A', AllBlocks.COPPER_CASING.get())
                .unlockedBy("has_shulker_shell", has(Items.SHULKER_SHELL))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CESGRegistration.SHULKER_BELT_LOADER.get())
                .pattern(" F ")
                .pattern("CBC")
                .pattern(" B ")
                .define('F', AllBlocks.ANDESITE_FUNNEL.get())
                .define('C', AllBlocks.COGWHEEL.get())
                .define('B', AllBlocks.BRASS_CASING.get())
                .unlockedBy("has_brass", has(AllBlocks.BRASS_CASING.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CESGRegistration.SHULKER_BELT_UNLOADER.get())
                .pattern(" F ")
                .pattern("CBC")
                .pattern(" B ")
                .define('F', AllBlocks.BRASS_FUNNEL.get())
                .define('C', AllBlocks.COGWHEEL.get())
                .define('B', AllBlocks.BRASS_CASING.get())
                .unlockedBy("has_brass", has(AllBlocks.BRASS_CASING.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CESGRegistration.ENHANCED_SHULKER_T2.get())
                .pattern("SSS")
                .pattern("SBS")
                .pattern("SSS")
                .define('S', CESGRegistration.SHULKER_SHELL.get())
                .define('B', Items.SHULKER_BOX)
                .unlockedBy("has_shell", has(CESGRegistration.SHULKER_SHELL.get()))
                .save(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, CESGRegistration.SHULKER_SHELL.get(), 2)
                .requires(Items.SHULKER_SHELL)
                .requires(AllItems.IRON_SHEET.get())
                .unlockedBy("has_shulker_shell", has(Items.SHULKER_SHELL))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CESGRegistration.END_GATEWAY.get())
                .pattern("CEC")
                .pattern("EGE")
                .pattern("CEC")
                .define('C', Items.CHORUS_FRUIT)
                .define('E', Blocks.END_STONE)
                .define('G', Items.ENDER_PEARL)
                .unlockedBy("has_chorus", has(Items.CHORUS_FRUIT))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CESGRegistration.CROSS_DIMENSIONAL_GATEWAY_CORE.get())
                .pattern("OEO")
                .pattern("BCB")
                .pattern("OEO")
                .define('O', Items.OBSIDIAN)
                .define('E', Items.ENDER_EYE)
                .define('B', AllBlocks.BRASS_CASING.get())
                .define('C', AllBlocks.COGWHEEL.get())
                .unlockedBy("has_ender_eye", has(Items.ENDER_EYE))
                .save(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, CESGRegistration.TELEPORT_ESSENCE_BUCKET.get())
                .requires(Items.BUCKET)
                .requires(Items.CHORUS_FRUIT)
                .requires(Items.ENDER_PEARL)
                .unlockedBy("has_pearl", has(Items.ENDER_PEARL))
                .save(output, CESG.MOD_ID + ":teleport_essence_from_chorus_pearl");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, CESGRegistration.LIQUID_EYE_OF_ENDER_BUCKET.get())
                .requires(CESGRegistration.TELEPORT_ESSENCE_BUCKET.get())
                .requires(Items.BLAZE_POWDER)
                .unlockedBy("has_blaze", has(Items.BLAZE_POWDER))
                .save(output, CESG.MOD_ID + ":liquid_eye_of_ender_mixing");
    }
}
