package com.cesg.recipe;

import com.cesg.init.CESGRecipes;
import com.cesg.upgrades.EnhancedShulkerBoxBlock;
import com.cesg.upgrades.EnhancedShulkerBoxes;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShulkerBoxColoring;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;

/**
 * Replaces {@code minecraft:shulker_box_coloring} (see data/minecraft/recipe/shulker_box_coloring.json).
 * Enhanced shulker boxes extend {@link ShulkerBoxBlock}, so vanilla's dye recipe accepts them and turns
 * them into vanilla boxes, stranding their contents. A separate CESG dye recipe cannot fix that: two
 * recipes matching the same grid are resolved by RecipeManager hash order. Replacing the vanilla recipe
 * under its own ID is deterministic because mod data always loads above vanilla data.
 *
 * <p>Vanilla boxes behave exactly as in vanilla. Enhanced boxes, dyed or not, keep all their components
 * (tier, contents, upgrades, filter, name) and become the enhanced box of the chosen color.
 */
public class EnhancedAwareShulkerBoxColoring extends ShulkerBoxColoring {
    public EnhancedAwareShulkerBoxColoring(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack vanillaResult = super.assemble(input, registries);
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (Block.byItem(stack.getItem()) instanceof EnhancedShulkerBoxBlock)
                return stack.transmuteCopy(
                        EnhancedShulkerBoxes.byColor(ShulkerBoxBlock.getColorFromItem(vanillaResult.getItem())).get(), 1);
        }
        return vanillaResult;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return CESGRecipes.SHULKER_BOX_COLORING.get();
    }
}
