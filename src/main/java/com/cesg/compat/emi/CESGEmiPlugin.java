package com.cesg.compat.emi;

import com.cesg.CESG;
import com.cesg.client.StorageTerminalScreen;
import com.cesg.init.CESGRecipes;
import com.cesg.init.CESGRegistration;
import com.cesg.recipe.EnderInfusingRecipe;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiStackInteraction;

import net.minecraft.world.item.crafting.RecipeHolder;

/** Registers the Ender Infuser's recipe category, workstation, and recipes with EMI. */
@EmiEntrypoint
public class CESGEmiPlugin implements EmiPlugin {
    public static final EmiStack INFUSER = EmiStack.of(CESGRegistration.ENDER_INFUSER.get());
    public static final EmiRecipeCategory ENDER_INFUSING =
            new EmiRecipeCategory(CESG.id("ender_infusing"), INFUSER);

    @Override
    public void register(EmiRegistry registry) {
        registry.addStackProvider(StorageTerminalScreen.class, (screen, mouseX, mouseY) ->
                screen.recipeViewerItemAt(mouseX, mouseY)
                        .map(item -> new EmiStackInteraction(EmiStack.of(item.stack()), null, false))
                        .orElse(EmiStackInteraction.EMPTY));
        registry.addCategory(ENDER_INFUSING);
        registry.addWorkstation(ENDER_INFUSING, INFUSER);
        for (RecipeHolder<EnderInfusingRecipe> holder :
                registry.getRecipeManager().getAllRecipesFor(CESGRecipes.ENDER_INFUSING_TYPE.get())) {
            registry.addRecipe(new EnderInfusingEmiRecipe(ENDER_INFUSING, holder.id(), holder.value()));
        }
        // Storage Terminal "+" transfer: fill the crafting grid from the network (server-authoritative).
        registry.addRecipeHandler(com.cesg.init.CESGMenus.STORAGE_TERMINAL.get(), new TerminalEmiRecipeHandler());
    }
}
