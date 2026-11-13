package com.cesg.upgrades;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * In-box processing for the Crushing/Washing/Haunting/Blasting modules: applies Create's crushing+milling,
 * splashing or haunting recipes, or vanilla blasting recipes, to the box's contents one operation at a
 * time, rolling chance outputs. Every conversion is simulated against a scratch mirror before it commits,
 * so a full box simply holds (no loss); any rare overflow at commit is handed to {@code overflow} rather
 * than dropped silently.
 *
 * <p>Priority: Crushing and Washing share items freely. Haunting leaves alone anything an installed
 * Crushing or Washing module can process; Blasting leaves alone anything an installed Crushing, Washing
 * or Haunting module can process, and never melts unstackable items (tools, armour).
 */
public final class ShulkerProcessingUpgrades {
    private ShulkerProcessingUpgrades() {}

    /** Which processing modules a box has installed; drives the priority skips. */
    public record Installed(boolean crushing, boolean washing, boolean haunting) {}

    private static List<RecipeType<?>> crushingTypes() {
        return List.of(AllRecipeTypes.CRUSHING.getType(), AllRecipeTypes.MILLING.getType());
    }

    private static List<RecipeType<?>> washingTypes() {
        return List.of(AllRecipeTypes.SPLASHING.getType());
    }

    private static List<RecipeType<?>> hauntingTypes() {
        return List.of(AllRecipeTypes.HAUNTING.getType());
    }

    public static void runCrushing(Level level, IItemHandler box, int maxOps, Consumer<ItemStack> overflow) {
        run(box, maxOps, overflow, in -> processingOutputs(level, crushingTypes(), in));
    }

    public static void runWashing(Level level, IItemHandler box, int maxOps, Consumer<ItemStack> overflow) {
        run(box, maxOps, overflow, in -> processingOutputs(level, washingTypes(), in));
    }

    public static void runHaunting(Level level, IItemHandler box, int maxOps, Consumer<ItemStack> overflow,
            Installed installed) {
        run(box, maxOps, overflow, in -> {
            if ((installed.crushing() && canCrush(level, in)) || (installed.washing() && canWash(level, in)))
                return null;
            return processingOutputs(level, hauntingTypes(), in);
        });
    }

    public static void runBlasting(Level level, IItemHandler box, int maxOps, Consumer<ItemStack> overflow,
            Installed installed) {
        run(box, maxOps, overflow, in -> {
            if (in.getMaxStackSize() <= 1 // tools and armour are not melted into nuggets
                    || (installed.crushing() && canCrush(level, in))
                    || (installed.washing() && canWash(level, in))
                    || (installed.haunting() && canHaunt(level, in)))
                return null;
            var recipe = level.getRecipeManager().getRecipeFor(RecipeType.BLASTING, new SingleRecipeInput(in), level);
            if (recipe.isEmpty())
                return null;
            ItemStack result = recipe.get().value().getResultItem(level.registryAccess());
            return result.isEmpty() ? null : List.of(result.copy());
        });
    }

    /** True when a Crushing module could process this item (crushing or milling). Cached per item. */
    public static boolean canCrush(Level level, ItemStack stack) {
        return canProcess(level, "crushing", crushingTypes(), stack);
    }

    static boolean canWash(Level level, ItemStack stack) {
        return canProcess(level, "washing", washingTypes(), stack);
    }

    static boolean canHaunt(Level level, ItemStack stack) {
        return canProcess(level, "haunting", hauntingTypes(), stack);
    }

    /**
     * "Can process" answers keyed by item, per recipe manager: a datapack reload builds a new manager,
     * which drops the stale answers. Server and client lookups each use their own manager's map.
     */
    private static final Map<RecipeManager, Map<String, Map<Item, Boolean>>> CAN_PROCESS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private static boolean canProcess(Level level, String module, List<RecipeType<?>> types, ItemStack stack) {
        if (stack.isEmpty())
            return false;
        Map<Item, Boolean> byItem = CAN_PROCESS
                .computeIfAbsent(level.getRecipeManager(), m -> new ConcurrentHashMap<>())
                .computeIfAbsent(module, m -> new ConcurrentHashMap<>());
        Boolean known = byItem.get(stack.getItem());
        if (known != null)
            return known;
        boolean found = findRecipe(level, types, new ItemStack(stack.getItem())) != null;
        byItem.put(stack.getItem(), found);
        return found;
    }

    private static void run(IItemHandler box, int maxOps, Consumer<ItemStack> overflow,
            Function<ItemStack, List<ItemStack>> outputsFor) {
        for (int op = 0; op < maxOps; op++)
            if (!processOne(box, overflow, outputsFor))
                break; // nothing left to process this pass
    }

    /** Rolled outputs of the first matching Create processing recipe, or null when none applies. */
    private static List<ItemStack> processingOutputs(Level level, List<RecipeType<?>> types, ItemStack in) {
        ProcessingRecipe<?, ?> recipe = findRecipe(level, types, in);
        if (recipe == null)
            return null;
        List<ItemStack> outputs = new ArrayList<>();
        for (ProcessingOutput output : recipe.getRollableResults())
            outputs.add(output.rollOutput(level.getRandom())); // rolls chance per output
        return outputs;
    }

    private static boolean processOne(IItemHandler box, Consumer<ItemStack> overflow,
            Function<ItemStack, List<ItemStack>> outputsFor) {
        for (int slot = 0; slot < box.getSlots(); slot++) {
            ItemStack in = box.getStackInSlot(slot);
            if (in.isEmpty())
                continue;
            List<ItemStack> outputs = outputsFor.apply(in);
            if (outputs == null)
                continue;
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
