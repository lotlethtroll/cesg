package com.cesg.upgrades;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import java.util.ArrayList;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * In-box processing for the Crushing/Washing modules (Phase 7D): applies Create's crushing+milling or
 * splashing recipes to the box's contents, one operation at a time, rolling chance outputs. Every
 * conversion is simulated against a scratch mirror before it commits, so a full box simply holds (no
 * loss); any rare overflow at commit is handed to {@code overflow} rather than dropped silently.
 */
public final class ShulkerProcessingUpgrades {
    private ShulkerProcessingUpgrades() {}

    private static List<RecipeType<?>> crushingTypes() {
        return List.of(AllRecipeTypes.CRUSHING.getType(), AllRecipeTypes.MILLING.getType());
    }

    private static List<RecipeType<?>> washingTypes() {
        return List.of(AllRecipeTypes.SPLASHING.getType());
    }

    public static void runCrushing(Level level, IItemHandler box, int maxOps, Consumer<ItemStack> overflow) {
        run(level, box, crushingTypes(), maxOps, overflow);
    }

    public static void runWashing(Level level, IItemHandler box, int maxOps, Consumer<ItemStack> overflow) {
        run(level, box, washingTypes(), maxOps, overflow);
    }

    private static void run(Level level, IItemHandler box, List<RecipeType<?>> types, int maxOps,
            Consumer<ItemStack> overflow) {
        for (int op = 0; op < maxOps; op++)
            if (!processOne(level, box, types, overflow))
                break; // nothing left to process this pass
    }

    private static boolean processOne(Level level, IItemHandler box, List<RecipeType<?>> types,
            Consumer<ItemStack> overflow) {
        for (int slot = 0; slot < box.getSlots(); slot++) {
            ItemStack in = box.getStackInSlot(slot);
            if (in.isEmpty())
                continue;
            ProcessingRecipe<?, ?> recipe = findRecipe(level, types, in);
            if (recipe == null)
                continue;
            List<ItemStack> outputs = new ArrayList<>();
            for (ProcessingOutput output : recipe.getRollableResults())
                outputs.add(output.rollOutput(level.getRandom())); // rolls chance per output
            if (commitIfFits(box, slot, outputs, overflow))
                return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static ProcessingRecipe<?, ?> findRecipe(Level level, List<RecipeType<?>> types, ItemStack stack) {
        SingleRecipeInput input = new SingleRecipeInput(stack);
        for (RecipeType<?> type : types) {
            Optional<RecipeHolder<Recipe<SingleRecipeInput>>> holder = level.getRecipeManager()
                    .getRecipeFor((RecipeType<Recipe<SingleRecipeInput>>) (RecipeType<?>) type, input, level);
            if (holder.isPresent() && holder.get().value() instanceof ProcessingRecipe<?, ?> recipe)
                return recipe;
        }
        return null;
    }

    /**
     * Simulates consuming one input and inserting all outputs against a scratch mirror; commits only if
     * everything fits. The mirror borrows the box's slot limits/validity so stack-depth capacity counts.
     */
    private static boolean commitIfFits(IItemHandler box, int slot, List<ItemStack> outputs,
            Consumer<ItemStack> overflow) {
        ItemStackHandler scratch = mirror(box);
        scratch.extractItem(slot, 1, false);
        for (ItemStack out : outputs) {
            if (out.isEmpty())
                continue;
            if (!ItemHandlerHelper.insertItemStacked(scratch, out.copy(), false).isEmpty())
                return false; // would not fit: leave the input untouched and hold
        }
        box.extractItem(slot, 1, false);
        for (ItemStack out : outputs) {
            if (out.isEmpty())
                continue;
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(box, out.copy(), false);
            if (!remainder.isEmpty())
                overflow.accept(remainder); // belt-and-suspenders; the scratch check makes this ~never
        }
        return true;
    }

    private static ItemStackHandler mirror(IItemHandler box) {
        ItemStackHandler scratch = new ItemStackHandler(box.getSlots()) {
            @Override
            public int getSlotLimit(int slot) {
                return box.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return box.isItemValid(slot, stack);
            }
        };
        for (int i = 0; i < box.getSlots(); i++)
            scratch.setStackInSlot(i, box.getStackInSlot(i).copy());
        return scratch;
    }
}
