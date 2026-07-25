package com.cesg.datagen;

import java.util.concurrent.CompletableFuture;

import com.cesg.CESG;
import com.cesg.init.CESGRegistration;
import com.cesg.recipe.EnderInfusingRecipeGen;
import com.cesg.recipe.EnhancedShulkerTierUpgradeRecipe;
import com.cesg.upgrades.EnhancedShulkerBoxes;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.recipes.SingleItemRecipeBuilder;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import com.cesg.decoration.CESGDecoratives;
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

        output.accept(CESG.id("enhanced_shulker_tier_2"), new EnhancedShulkerTierUpgradeRecipe(2), null);
        output.accept(CESG.id("enhanced_shulker_tier_3"), new EnhancedShulkerTierUpgradeRecipe(3), null);
        output.accept(CESG.id("enhanced_shulker_tier_4"), new EnhancedShulkerTierUpgradeRecipe(4), null);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CESGRegistration.SHULKER_CAGE.get())
                .pattern(" S ")
                .pattern("EPE")
                .pattern(" S ")
                .define('S', CESGRegistration.SHULKER_SHELL.get())
                .define('E', Blocks.END_STONE)
                .define('P', Items.ENDER_PEARL)
                .unlockedBy("has_shulker_shell", has(CESGRegistration.SHULKER_SHELL.get()))
                .save(output);

        for (DyeColor color : DyeColor.values()) {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, EnhancedShulkerBoxes.byColor(color).get())
                    .requires(EnhancedShulkerBoxes.DEFAULT.get())
                    .requires(DyeItem.byColor(color))
                    .group("cesg:enhanced_shulker_box_dye")
                    .unlockedBy("has_enhanced_shulker_box", has(EnhancedShulkerBoxes.DEFAULT.get()))
                    .save(output, CESG.MOD_ID + ":enhanced_" + color.getName() + "_shulker_box_from_dye");
        }

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CESGRegistration.STACK_DEPTH_UPGRADE_T1.get())
                .pattern(" B ")
                .pattern("ISI")
                .pattern(" B ")
                .define('B', AllBlocks.BRASS_CASING.get())
                .define('I', AllItems.IRON_SHEET.get())
                .define('S', CESGRegistration.SHULKER_SHELL.get())
                .unlockedBy("has_enhanced_shulker_box", has(EnhancedShulkerBoxes.DEFAULT.get()))
                .save(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, CESGRegistration.STACK_DEPTH_UPGRADE_T2.get())
                .requires(CESGRegistration.STACK_DEPTH_UPGRADE_T1.get())
                .requires(AllBlocks.BRASS_CASING.get())
                .requires(AllItems.BRASS_INGOT.get())
                .requires(CESGRegistration.SHULKER_SHELL.get())
                .unlockedBy("has_stack_depth_t1", has(CESGRegistration.STACK_DEPTH_UPGRADE_T1.get()))
                .save(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, CESGRegistration.STACK_DEPTH_UPGRADE_T3.get())
                .requires(CESGRegistration.STACK_DEPTH_UPGRADE_T2.get())
                .requires(AllBlocks.BRASS_CASING.get())
                .requires(Items.ENDER_PEARL)
                .requires(CESGRegistration.SHULKER_SHELL.get())
                .unlockedBy("has_stack_depth_t2", has(CESGRegistration.STACK_DEPTH_UPGRADE_T2.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CESGRegistration.FILTER_UPGRADE.get())
                .pattern(" F ")
                .pattern("IRI")
                .pattern(" B ")
                .define('F', AllBlocks.BRASS_FUNNEL.get())
                .define('I', AllItems.IRON_SHEET.get())
                .define('R', Items.REDSTONE)
                .define('B', AllBlocks.BRASS_TUNNEL.get())
                .unlockedBy("has_enhanced_shulker_box", has(EnhancedShulkerBoxes.DEFAULT.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CESGRegistration.COMPACTING_UPGRADE.get())
                .pattern(" P ")
                .pattern("IBI")
                .pattern(" C ")
                .define('P', Items.PISTON)
                .define('I', AllItems.IRON_SHEET.get())
                .define('B', AllBlocks.BRASS_CASING.get())
                .define('C', AllBlocks.COGWHEEL.get())
                .unlockedBy("has_enhanced_shulker_box", has(EnhancedShulkerBoxes.DEFAULT.get()))
                .save(output);

        // Smelting module: what goes in comes out furnace-processed (blast furnace heart).
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CESGRegistration.SMELTING_UPGRADE.get())
                .pattern(" Z ")
                .pattern("IFI")
                .pattern(" S ")
                .define('Z', Items.BLAZE_POWDER)
                .define('I', AllItems.IRON_SHEET.get())
                .define('F', Items.BLAST_FURNACE)
                .define('S', CESGRegistration.SHULKER_SHELL.get())
                .unlockedBy("has_enhanced_shulker_box", has(EnhancedShulkerBoxes.DEFAULT.get()))
                .save(output);

        // Void module: overflow of stored types is destroyed (obsidian + ender dust sink).
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CESGRegistration.VOID_UPGRADE.get())
                .pattern(" O ")
                .pattern("IDI")
                .pattern(" B ")
                .define('O', Items.OBSIDIAN)
                .define('I', AllItems.IRON_SHEET.get())
                .define('D', CESGRegistration.ENDER_PEARL_DUST.get())
                .define('B', AllBlocks.BRASS_CASING.get())
                .unlockedBy("has_enhanced_shulker_box", has(EnhancedShulkerBoxes.DEFAULT.get()))
                .save(output);

        // Magnet modules: electron-tube coil; tiers chain like stack depth.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CESGRegistration.MAGNET_UPGRADE_T1.get())
                .pattern(" T ")
                .pattern("IBI")
                .pattern(" S ")
                .define('T', AllItems.ELECTRON_TUBE.get())
                .define('I', AllItems.IRON_SHEET.get())
                .define('B', AllBlocks.BRASS_CASING.get())
                .define('S', CESGRegistration.SHULKER_SHELL.get())
                .unlockedBy("has_enhanced_shulker_box", has(EnhancedShulkerBoxes.DEFAULT.get()))
                .save(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, CESGRegistration.MAGNET_UPGRADE_T2.get())
                .requires(CESGRegistration.MAGNET_UPGRADE_T1.get())
                .requires(AllItems.ELECTRON_TUBE.get())
                .requires(AllItems.BRASS_INGOT.get())
                .requires(CESGRegistration.SHULKER_SHELL.get())
                .unlockedBy("has_magnet_t1", has(CESGRegistration.MAGNET_UPGRADE_T1.get()))
                .save(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, CESGRegistration.MAGNET_UPGRADE_T3.get())
                .requires(CESGRegistration.MAGNET_UPGRADE_T2.get())
                .requires(AllItems.ELECTRON_TUBE.get())
                .requires(Items.ENDER_PEARL)
                .requires(CESGRegistration.SHULKER_SHELL.get())
                .unlockedBy("has_magnet_t2", has(CESGRegistration.MAGNET_UPGRADE_T2.get()))
                .save(output);

        // Crushing module (7D): millstone core; in-box crushing + milling. Tiers chain like magnet.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CESGRegistration.CRUSHING_UPGRADE_T1.get())
                .pattern(" I ")
                .pattern("IMI")
                .pattern(" S ")
                .define('I', AllItems.IRON_SHEET.get())
                .define('M', AllBlocks.MILLSTONE.get())
                .define('S', CESGRegistration.SHULKER_SHELL.get())
                .unlockedBy("has_enhanced_shulker_box", has(EnhancedShulkerBoxes.DEFAULT.get()))
                .save(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, CESGRegistration.CRUSHING_UPGRADE_T2.get())
                .requires(CESGRegistration.CRUSHING_UPGRADE_T1.get())
                .requires(AllItems.ELECTRON_TUBE.get())
                .requires(AllItems.BRASS_INGOT.get())
                .requires(CESGRegistration.SHULKER_SHELL.get())
                .unlockedBy("has_crushing_t1", has(CESGRegistration.CRUSHING_UPGRADE_T1.get()))
                .save(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, CESGRegistration.CRUSHING_UPGRADE_T3.get())
                .requires(CESGRegistration.CRUSHING_UPGRADE_T2.get())
                .requires(AllItems.PRECISION_MECHANISM.get())
                .requires(AllItems.BRASS_INGOT.get())
                .requires(CESGRegistration.SHULKER_SHELL.get())
                .unlockedBy("has_crushing_t2", has(CESGRegistration.CRUSHING_UPGRADE_T2.get()))
                .save(output);

        // Washing module (7D): encased-fan core over water; in-box splashing. Tiers chain like magnet.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CESGRegistration.WASHING_UPGRADE_T1.get())
                .pattern(" W ")
                .pattern("IFI")
                .pattern(" S ")
                .define('W', Items.WATER_BUCKET)
                .define('I', AllItems.IRON_SHEET.get())
                .define('F', AllBlocks.ENCASED_FAN.get())
                .define('S', CESGRegistration.SHULKER_SHELL.get())
                .unlockedBy("has_enhanced_shulker_box", has(EnhancedShulkerBoxes.DEFAULT.get()))
                .save(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, CESGRegistration.WASHING_UPGRADE_T2.get())
                .requires(CESGRegistration.WASHING_UPGRADE_T1.get())
                .requires(AllItems.ELECTRON_TUBE.get())
                .requires(AllItems.BRASS_INGOT.get())
                .requires(CESGRegistration.SHULKER_SHELL.get())
                .unlockedBy("has_washing_t1", has(CESGRegistration.WASHING_UPGRADE_T1.get()))
                .save(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, CESGRegistration.WASHING_UPGRADE_T3.get())
                .requires(CESGRegistration.WASHING_UPGRADE_T2.get())
                .requires(AllItems.PRECISION_MECHANISM.get())
                .requires(AllItems.BRASS_INGOT.get())
                .requires(CESGRegistration.SHULKER_SHELL.get())
                .unlockedBy("has_washing_t2", has(CESGRegistration.WASHING_UPGRADE_T2.get()))
                .save(output);

        // Processed Shulker Shells are now made by dousing a Shulker Shell in Liquid Ender Pearl with a
        // Create Spout (see data/cesg/recipe/processed_shulker_shell_from_filling.json), so the old
        // iron-sheet crafting recipe is retired.

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

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CESGRegistration.GATEWAY_FRAME.get(), 4)
                .pattern("EBE")
                .pattern("BPB")
                .pattern("EBE")
                .define('E', Blocks.END_STONE_BRICKS)
                .define('B', AllItems.BRASS_INGOT.get())
                .define('P', Items.ENDER_PEARL)
                .unlockedBy("has_ender_pearl", has(Items.ENDER_PEARL))
                .save(output);

        // Gateway Port (6A): brass-cased buffer that ships items/fluids through a bound gateway.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CESGRegistration.GATEWAY_PORT.get())
                .pattern("EDE")
                .pattern("BHB")
                .pattern("EBE")
                .define('E', Blocks.END_STONE_BRICKS)
                .define('D', CESGRegistration.ENDER_PEARL_DUST.get())
                .define('B', AllItems.BRASS_INGOT.get())
                .define('H', Blocks.HOPPER)
                .unlockedBy("has_gateway_frame", has(CESGRegistration.GATEWAY_FRAME.get()))
                .save(output);

        // Gateway Flux Battery (7E): a Create fluid tank ringed in brass + ender pearls; buffers gateway fuel.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CESGRegistration.GATEWAY_FLUX_BATTERY.get())
                .pattern("BPB")
                .pattern("PTP")
                .pattern("BPB")
                .define('B', AllItems.BRASS_INGOT.get())
                .define('P', Items.ENDER_PEARL)
                .define('T', AllBlocks.FLUID_TANK.get())
                .unlockedBy("has_fluid_tank", has(AllBlocks.FLUID_TANK.get()))
                .save(output);

        // Teleport Essence and Liquid Eye of Ender are now real fluids made by heated-basin mixing
        // (see data/cesg/recipe/*_from_mixing.json); their buckets are filled from a tank, not crafted.

        // End Cultivation (7G): End materials are made renewable through native Create processing,
        // authored as static JSON (not datagen'd). Haunting sandstone -> end stone (the essence-free
        // base path); crushing end stone -> sand + a chance of ender pearl dust; compacting 3 dust ->
        // 1 ender pearl; optional filling stone with Liquid Ender Pearl -> end stone (premium
        // accelerant). See data/cesg/recipe/{end_stone_from_haunting, ender_pearl_dust_from_end_stone_
        // crushing, ender_pearl_from_compacting, end_stone_from_filling}.json.

        // Amethyst (structure) + Echo Shard (location imprint/recall) + Ender Eye (dimensional sight).
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CESGRegistration.GATEWAY_BINDING_ITEM.get())
                .pattern(" A ")
                .pattern("HEH")
                .pattern(" A ")
                .define('A', Items.AMETHYST_SHARD)
                .define('H', Items.ECHO_SHARD)
                .define('E', Items.ENDER_EYE)
                .unlockedBy("has_echo_shard", has(Items.ECHO_SHARD))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CESGRegistration.EMERGENCY_EYE_CHARGE.get())
                .pattern(" B ")
                .pattern("PEP")
                .pattern(" C ")
                .define('B', Items.BLAZE_POWDER)
                .define('P', Items.ENDER_PEARL)
                .define('E', Items.ENDER_EYE)
                .define('C', Items.CHORUS_FRUIT)
                .unlockedBy("has_ender_eye", has(Items.ENDER_EYE))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CESGRegistration.ENDER_INFUSER.get())
                .pattern(" E ")
                .pattern("BCB")
                .pattern(" S ")
                .define('E', Items.ENDER_EYE)
                .define('B', AllItems.BRASS_SHEET.get())
                .define('C', AllBlocks.BRASS_CASING.get())
                .define('S', AllBlocks.SHAFT.get())
                .unlockedBy("has_brass_casing", has(AllBlocks.BRASS_CASING.get()))
                .save(output);

        // Storage network (6D): controller anchors the cluster, terminal is the access point.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CESGRegistration.STORAGE_NETWORK_CONTROLLER.get())
                .pattern("DSD")
                .pattern("SCS")
                .pattern("DSD")
                .define('D', CESGRegistration.ENDER_PEARL_DUST.get())
                .define('S', Items.SHULKER_SHELL)
                .define('C', AllBlocks.BRASS_CASING.get())
                .unlockedBy("has_shulker_shell", has(Items.SHULKER_SHELL))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CESGRegistration.STORAGE_TERMINAL.get())
                .pattern("GGG")
                .pattern("DED")
                .pattern("BCB")
                .define('G', Blocks.GLASS)
                .define('D', CESGRegistration.ENDER_PEARL_DUST.get())
                .define('E', Items.ENDER_EYE)
                .define('B', AllItems.BRASS_INGOT.get())
                .define('C', AllBlocks.BRASS_CASING.get())
                .unlockedBy("has_shulker_shell", has(Items.SHULKER_SHELL))
                .save(output);

        EnderInfusingRecipeGen.generate(output);
        decorativeRecipes(output);
    }

    /** Smooth (smelt) / polished (2x2) base variants + crafting & stonecutting for each shape family. */
    private void decorativeRecipes(RecipeOutput output) {
        for (CESGDecoratives.Family fam : CESGDecoratives.families()) {
            Block base = fam.base().get();
            Block source = fam.name().contains("purpur") ? Blocks.PURPUR_BLOCK : Blocks.END_STONE;
            String sourceName = source == Blocks.PURPUR_BLOCK ? "purpur_block" : "end_stone";
            String baseName = fam.name();
            Block stairs = fam.stairs().get();
            Block slab = fam.slab().get();
            Block wall = fam.wall().get();

            if (fam.name().startsWith("smooth_")) {
                SimpleCookingRecipeBuilder.smelting(Ingredient.of(source), RecipeCategory.BUILDING_BLOCKS,
                                base, 0.1f, 200)
                        .unlockedBy("has_material", has(source))
                        .save(output);
            } else { // polished
                ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, base, 4)
                        .pattern("##").pattern("##").define('#', source)
                        .unlockedBy("has_material", has(source))
                        .save(output);
                cut(output, source, base, 1, baseName + "_from_" + sourceName);
            }

            stairBuilder(stairs, Ingredient.of(base)).unlockedBy("has_material", has(base)).save(output);
            slab(output, RecipeCategory.BUILDING_BLOCKS, slab, base);
            wall(output, RecipeCategory.BUILDING_BLOCKS, wall, base);

            cut(output, base, stairs, 1, baseName + "_stairs_from_" + baseName);
            cut(output, base, slab, 2, baseName + "_slab_from_" + baseName);
            cut(output, base, wall, 1, baseName + "_wall_from_" + baseName);
        }
    }

    /** Stonecutting recipe under the cesg namespace (the vanilla helper defaults to minecraft:). */
    private void cut(RecipeOutput output, ItemLike from, ItemLike result, int count, String id) {
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(from), RecipeCategory.BUILDING_BLOCKS, result, count)
                .unlockedBy("has_material", has(from))
                .save(output, CESG.id(id + "_stonecutting"));
    }
}
