package com.cesg.recipe;

import java.util.List;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

/** The live contents the Ender Infuser matches against: the input tank's fluid and the catalyst slots. */
public record EnderInfusingInput(FluidStack fluid, List<ItemStack> catalysts) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return catalysts.get(index);
    }

    @Override
    public int size() {
        return catalysts.size();
    }

    @Override
    public boolean isEmpty() {
        return fluid.isEmpty() && catalysts.stream().allMatch(ItemStack::isEmpty);
    }
}
