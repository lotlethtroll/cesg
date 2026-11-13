package com.cesg.recipe;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import com.cesg.CESG;
import com.cesg.upgrades.EnhancedShulkerBoxes;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.ItemLike;

/**
 * Display-only recipes for JEI/EMI. Dyeing is crafted by {@link EnhancedAwareShulkerBoxColoring}, a
 * special recipe that recipe viewers cannot enumerate, so the viewers show one entry per color instead.
 * These holders are never added to the RecipeManager.
 */
public final class EnhancedShulkerDyeDisplay {
    private EnhancedShulkerDyeDisplay() {}

    /** One color's display: any enhanced box + that dye gives the enhanced box of that color. */
    public record Entry(ResourceLocation id, List<Ingredient> inputs, ItemStack result) {}

    public static List<Entry> entries() {
        Ingredient anyEnhancedBox = Ingredient.of(Stream.concat(Stream.of((DyeColor) null), Arrays.stream(DyeColor.values()))
                .map(c -> (ItemLike) EnhancedShulkerBoxes.byColor(c).get()).toArray(ItemLike[]::new));
        return Arrays.stream(DyeColor.values()).map(color -> new Entry(
                CESG.id("/enhanced_" + color.getName() + "_shulker_box_dyeing"),
                List.of(anyEnhancedBox, Ingredient.of(DyeItem.byColor(color))),
                new ItemStack(EnhancedShulkerBoxes.byColor(color).get()))).toList();
    }

    /** The entries as shapeless crafting holders, for JEI's crafting category. */
    public static List<RecipeHolder<CraftingRecipe>> recipes() {
        return entries().stream().map(e -> new RecipeHolder<CraftingRecipe>(e.id(),
                new ShapelessRecipe("cesg:enhanced_shulker_box_dye", CraftingBookCategory.MISC, e.result(),
                        NonNullList.of(Ingredient.EMPTY, e.inputs().toArray(Ingredient[]::new))))).toList();
    }
}
