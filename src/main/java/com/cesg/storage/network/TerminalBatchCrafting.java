package com.cesg.storage.network;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.event.EventHooks;

import org.jetbrains.annotations.Nullable;

/** Shift-click batch crafting for the storage terminal (grid + network refill). */
final class TerminalBatchCrafting {
    private static final int MAX_PASSES = 1024;

    private TerminalBatchCrafting() {}

    static ItemStack shiftCraftAll(StorageTerminalMenu menu, Player player) {
        Slot resultSlot = menu.slots.get(StorageTerminalMenu.RESULT_SLOT);
        if (!resultSlot.hasItem())
            return ItemStack.EMPTY;

        Level level = player.level();
        if (level.isClientSide)
            return ItemStack.EMPTY;

        Container craftSlots = menu.craftSlots();
        ResultContainer resultSlots = menu.resultSlots();
        ItemStack[] slotTemplates = snapshotTemplates(craftSlots);

        // Stock full stacks before crafting so one shift-click drains as much as possible.
        stockToFullStacks(menu, craftSlots, slotTemplates);

        ItemStack outputCopy = ItemStack.EMPTY;
        RecipeHolder<CraftingRecipe> recipe = getCraftingRecipe(resultSlots);
        CraftingInput input = ((net.minecraft.world.inventory.CraftingContainer) craftSlots).asCraftInput();

        if (recipe == null)
            recipe = findRecipe(level, input);

        for (int pass = 0; pass < MAX_PASSES && recipe != null && recipe.value().matches(input, level); pass++) {
            stockToFullStacks(menu, craftSlots, slotTemplates);
            input = ((net.minecraft.world.inventory.CraftingContainer) craftSlots).asCraftInput();
            if (!recipe.value().matches(input, level))
                break;

            menu.updateCraftingResultServer();
            if (!resultSlot.hasItem())
                break;

            ItemStack recipeOutput = recipe.value().assemble(input, level.registryAccess());
            if (recipeOutput.isEmpty())
                break;

            outputCopy = recipeOutput.copy();
            recipeOutput.onCraftedBy(level, player, 1);
            EventHooks.firePlayerCraftingEvent(player, recipeOutput, craftSlots);

            ItemStack toMove = recipeOutput.copy();
            if (!menu.moveCraftResultToPlayer(toMove) || !toMove.isEmpty())
                break;

            if (!(resultSlot instanceof ResultSlot craftResult))
                break;
            craftResult.onTake(player, recipeOutput);
            resetStackedContents(input);

            input = ((net.minecraft.world.inventory.CraftingContainer) craftSlots).asCraftInput();
            recipe = getCraftingRecipe(resultSlots);
            if (recipe == null || !recipe.value().matches(input, level))
                recipe = findRecipe(level, input);
        }

        // Leave the grid topped up for the next shift-craft.
        stockToFullStacks(menu, craftSlots, slotTemplates);
        menu.updateCraftingResultServer();
        menu.requestRefresh();
        return outputCopy;
    }

    private static ItemStack[] snapshotTemplates(Container craftSlots) {
        ItemStack[] templates = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            ItemStack stack = craftSlots.getItem(i);
            if (!stack.isEmpty())
                templates[i] = stack.copyWithCount(1);
        }
        return templates;
    }

    /** Pulls from the network until each patterned slot holds a full stack (or runs dry). */
    private static void stockToFullStacks(StorageTerminalMenu menu, Container craftSlots,
            ItemStack[] templates) {
        Level level = menu.player().level();
        for (int i = 0; i < 9; i++) {
            ItemStack template = templates[i];
            if (template == null || template.isEmpty())
                continue;

            ItemStack current = craftSlots.getItem(i);
            if (current.isEmpty() || !ItemStack.isSameItemSameComponents(current, template)) {
                ItemStack pulled = StorageNetwork.extract(level, menu.getTerminalPos(), template,
                        template.getMaxStackSize());
                if (!pulled.isEmpty())
                    craftSlots.setItem(i, pulled);
                continue;
            }

            int need = current.getMaxStackSize() - current.getCount();
            if (need <= 0)
                continue;
            ItemStack pulled = StorageNetwork.extract(level, menu.getTerminalPos(), current, need);
            if (!pulled.isEmpty())
                current.grow(pulled.getCount());
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static RecipeHolder<CraftingRecipe> getCraftingRecipe(ResultContainer resultSlots) {
        RecipeHolder<?> used = resultSlots.getRecipeUsed();
        if (used != null && used.value() instanceof CraftingRecipe)
            return (RecipeHolder<CraftingRecipe>) used;
        return null;
    }

    @Nullable
    private static RecipeHolder<CraftingRecipe> findRecipe(Level level, CraftingInput input) {
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).orElse(null);
    }

    private static void resetStackedContents(CraftingInput input) {
        StackedContents contents = input.stackedContents();
        contents.clear();
        for (ItemStack stack : input.items()) {
            if (!stack.isEmpty())
                contents.accountStack(stack, 1);
        }
    }
}
