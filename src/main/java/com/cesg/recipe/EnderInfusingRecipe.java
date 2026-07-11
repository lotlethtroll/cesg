package com.cesg.recipe;

import java.util.List;

import com.cesg.init.CESGRecipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/**
 * A single Ender Infuser conversion: an input fluid (with amount) plus zero or more item catalysts (each
 * with a count) yields an output fluid (with amount). {@code processingTime} is the tick interval at speed
 * 64; it scales with rotational speed in the block entity. Works with any fluid — CESG, Create, or vanilla.
 */
public record EnderInfusingRecipe(SizedFluidIngredient input, List<SizedIngredient> catalysts,
        FluidStack result, List<ItemStack> resultItems, int processingTime) implements Recipe<EnderInfusingInput> {

    /** Datagen factory (no item byproducts). */
    public static EnderInfusingRecipe of(SizedFluidIngredient input, List<SizedIngredient> catalysts,
            Fluid resultFluid, int resultAmount, int processingTime) {
        return of(input, catalysts, resultFluid, resultAmount, List.of(), processingTime);
    }

    /** Datagen factory with item byproducts (e.g. a reverse recipe handing the catalyst back). */
    public static EnderInfusingRecipe of(SizedFluidIngredient input, List<SizedIngredient> catalysts,
            Fluid resultFluid, int resultAmount, List<ItemStack> resultItems, int processingTime) {
        return new EnderInfusingRecipe(input, catalysts, new FluidStack(resultFluid, resultAmount),
                resultItems, processingTime);
    }

    @Override
    public boolean matches(EnderInfusingInput in, Level level) {
        if (!input.test(in.fluid()))
            return false;
        return catalystsAvailable(in.catalysts());
    }

    /** True if every required catalyst can be satisfied (by count) across the given stacks, no double-use. */
    public boolean catalystsAvailable(List<ItemStack> stacks) {
        if (catalysts.isEmpty())
            return true;
        int[] remaining = new int[stacks.size()];
        for (int i = 0; i < stacks.size(); i++)
            remaining[i] = stacks.get(i).getCount();
        for (SizedIngredient cat : catalysts) {
            int need = cat.count();
            for (int i = 0; i < stacks.size() && need > 0; i++) {
                if (remaining[i] > 0 && cat.ingredient().test(stacks.get(i))) {
                    int take = Math.min(need, remaining[i]);
                    remaining[i] -= take;
                    need -= take;
                }
            }
            if (need > 0)
                return false;
        }
        return true;
    }

    @Override
    public ItemStack assemble(EnderInfusingInput in, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return CESGRecipes.ENDER_INFUSING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return CESGRecipes.ENDER_INFUSING_TYPE.get();
    }
}
