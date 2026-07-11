package com.cesg.recipe;

import java.util.UUID;

import com.cesg.init.CESGDataComponents;
import com.cesg.init.CESGRecipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

/**
 * Shaped recipe whose crafted result (a stack of two Ender Barrels) gets a FRESH pair id per craft —
 * that is what makes the two barrels twins of each other and of nothing else. A static component on
 * a normal recipe result could not do this: every craft world-wide would share one inventory.
 */
public class EnderBarrelPairingRecipe extends ShapedRecipe {
    // ShapedRecipe keeps these private; the serializer needs them back for (de)serialization.
    final ShapedRecipePattern patternCopy;
    final ItemStack resultCopy;

    public EnderBarrelPairingRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern,
            ItemStack result) {
        super(group, category, pattern, result, true);
        this.patternCopy = pattern;
        this.resultCopy = result;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack result = super.assemble(input, registries);
        result.set(CESGDataComponents.ENDER_BARREL_PAIR.get(), UUID.randomUUID());
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return CESGRecipes.ENDER_BARREL_PAIRING.get();
    }
}
