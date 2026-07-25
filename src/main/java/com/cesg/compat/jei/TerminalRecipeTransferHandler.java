package com.cesg.compat.jei;

import java.util.Optional;

import com.cesg.init.CESGMenus;
import com.cesg.network.TerminalFillRecipePacket;
import com.cesg.storage.network.StorageTerminalMenu;

import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * JEI "+" transfer for the Storage Terminal (Phase 7F): fills the 3×3 grid from the player inventory and
 * the storage network. The actual fill is server-authoritative — we just forward the recipe id and let
 * {@link TerminalFillRecipePacket} do the work, so JEI's default player-inventory-only mover is bypassed.
 */
public class TerminalRecipeTransferHandler
        implements IRecipeTransferHandler<StorageTerminalMenu, RecipeHolder<CraftingRecipe>> {

    @Override
    public Class<StorageTerminalMenu> getContainerClass() {
        return StorageTerminalMenu.class;
    }

    @Override
    public Optional<MenuType<StorageTerminalMenu>> getMenuType() {
        return Optional.of(CESGMenus.STORAGE_TERMINAL.get());
    }

    @Override
    public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Override
    public IRecipeTransferError transferRecipe(StorageTerminalMenu container,
            RecipeHolder<CraftingRecipe> recipe, IRecipeSlotsView recipeSlots, Player player,
            boolean maxTransfer, boolean doTransfer) {
        // Optimistic: the server pulls from the live network, so we can't cheaply pre-validate here.
        if (doTransfer)
            PacketDistributor.sendToServer(new TerminalFillRecipePacket(container.containerId, recipe.id()));
        return null;
    }
}
