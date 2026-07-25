package com.cesg.compat.jei;

import java.util.List;

import com.cesg.CESG;
import com.cesg.init.CESGRecipes;
import com.cesg.init.CESGRegistration;
import com.cesg.recipe.EnderInfusingRecipe;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Registers the Ender Infuser's recipe category, workstation, and recipes with JEI. */
@JeiPlugin
public class CESGJeiPlugin implements IModPlugin {
    // Lazy: JEI's plugin finder class-loads this DURING MOD CONSTRUCTION, before registries bind —
    // a static createFromVanilla(...) here crashes the clinit and JEI silently skips the plugin.
    private static RecipeType<RecipeHolder<EnderInfusingRecipe>> enderInfusing;

    public static RecipeType<RecipeHolder<EnderInfusingRecipe>> enderInfusingType() {
        if (enderInfusing == null)
            enderInfusing = RecipeType.createFromVanilla(CESGRecipes.ENDER_INFUSING_TYPE.get());
        return enderInfusing;
    }

    @Override
    public ResourceLocation getPluginUid() {
        return CESG.id("jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new EnderInfusingJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null)
            return;
        List<RecipeHolder<EnderInfusingRecipe>> recipes =
                mc.level.getRecipeManager().getAllRecipesFor(CESGRecipes.ENDER_INFUSING_TYPE.get());
        registration.addRecipes(enderInfusingType(), recipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(CESGRegistration.ENDER_INFUSER.get()), enderInfusingType());
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        // Storage Terminal "+" transfer: fill the crafting grid from the network (server-authoritative).
        registration.addRecipeTransferHandler(new TerminalRecipeTransferHandler(), RecipeTypes.CRAFTING);
    }
}
