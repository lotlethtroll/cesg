package com.cesg.compat.emi;

import com.cesg.network.TerminalFillRecipePacket;
import com.cesg.storage.network.StorageTerminalMenu;

import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * EMI "+" transfer for the Storage Terminal (Phase 7F): forwards the crafting recipe id to the server,
 * which fills the grid from the player inventory + storage network (see {@link TerminalFillRecipePacket}).
 */
public class TerminalEmiRecipeHandler implements EmiRecipeHandler<StorageTerminalMenu> {

    @Override
    public EmiPlayerInventory getInventory(AbstractContainerScreen<StorageTerminalMenu> screen) {
        // Only used for display hints; canCraft() below stays optimistic so the button reflects the
        // network too, and the server-side fill pulls from both the player and the network.
        return new EmiPlayerInventory(Minecraft.getInstance().player);
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        return recipe.getCategory() == VanillaEmiRecipeCategories.CRAFTING && recipe.getId() != null;
    }

    @Override
    public boolean canCraft(EmiRecipe recipe, EmiCraftContext<StorageTerminalMenu> context) {
        return recipe.getId() != null;
    }

    @Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<StorageTerminalMenu> context) {
        ResourceLocation id = recipe.getId();
        if (id == null)
            return false;
        StorageTerminalMenu menu = context.getScreen().getMenu();
        PacketDistributor.sendToServer(new TerminalFillRecipePacket(menu.containerId, id));
        return true;
    }
}
