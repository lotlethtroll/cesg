package com.cesg.machine;

import java.util.List;

import com.cesg.init.CESGBlockEntities;
import com.cesg.init.CESGRecipes;
import com.cesg.recipe.EnderInfusingInput;
import com.cesg.recipe.EnderInfusingRecipe;
import com.cesg.util.CESGLang;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Kinetic fluid converter driven by {@code cesg:ender_infusing} recipes: an input fluid (+ optional item
 * catalyst) is converted into an output fluid. Pipe/bucket a fluid into the input tank, load the catalyst
 * slot, and spin the back shaft. Throughput scales with rotational speed. Works with any fluid the recipes
 * reference — CESG, Create, or vanilla.
 */
public class EnderInfuserBlockEntity extends KineticBlockEntity {
    public static final int TANK_CAPACITY = 4000;
    public static final int CATALYST_SLOTS = 3;

    private FluidStack input = FluidStack.EMPTY;
    private FluidStack output = FluidStack.EMPTY;
    // Multiple slots so a recipe can require several catalysts at once (e.g. Sugar + Cocoa Beans).
    private final ItemStackHandler catalyst = new ItemStackHandler(CATALYST_SLOTS) {
        // Accepts any item, but each distinct catalyst stays in a single slot (no spilling one item
        // across multiple slots); recipes decide which catalysts actually get consumed.
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            for (int i = 0; i < getSlots(); i++) {
                if (i != slot && !getStackInSlot(i).isEmpty()
                        && ItemStack.isSameItemSameComponents(getStackInSlot(i), stack))
                    return false;
            }
            return true;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            // Sync to clients so the goggle catalyst readout updates in real time.
            if (level != null && !level.isClientSide)
                sendData();
        }
    };
    private int ticksSinceStep;

    public EnderInfuserBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public EnderInfuserBlockEntity(BlockPos pos, BlockState state) {
        this(CESGBlockEntities.ENDER_INFUSER.get(), pos, state);
    }

    public ItemStackHandler getCatalyst() {
        return catalyst;
    }

    public FluidStack getInput() {
        return input;
    }

    public FluidStack getOutput() {
        return output;
    }

    /** Pipe fill target: only the input tank accepts fluid (any fluid — recipes decide what converts). */
    public int fillInput(FluidStack resource, boolean simulate) {
        if (resource.isEmpty())
            return 0;
        if (!input.isEmpty() && !FluidStack.isSameFluidSameComponents(input, resource))
            return 0;
        int space = TANK_CAPACITY - input.getAmount();
        int filled = Math.min(space, resource.getAmount());
        if (filled > 0 && !simulate) {
            if (input.isEmpty())
                input = new FluidStack(resource.getFluid(), filled);
            else
                input.grow(filled);
            notifyUpdate();
        }
        return Math.max(0, filled);
    }

    /** Bucket drain: pull from the input tank (used when the output is empty). */
    public FluidStack drainInput(int maxDrain, boolean simulate) {
        if (input.isEmpty() || maxDrain <= 0)
            return FluidStack.EMPTY;
        int drained = Math.min(maxDrain, input.getAmount());
        FluidStack result = new FluidStack(input.getFluid(), drained);
        if (!simulate) {
            input.shrink(drained);
            if (input.getAmount() <= 0)
                input = FluidStack.EMPTY;
            notifyUpdate();
        }
        return result;
    }

    /** Pipe drain source: only the output tank is drained. */
    public FluidStack drainOutput(int maxDrain, boolean simulate) {
        if (output.isEmpty() || maxDrain <= 0)
            return FluidStack.EMPTY;
        int drained = Math.min(maxDrain, output.getAmount());
        FluidStack result = new FluidStack(output.getFluid(), drained);
        if (!simulate) {
            output.shrink(drained);
            if (output.getAmount() <= 0)
                output = FluidStack.EMPTY;
            notifyUpdate();
        }
        return result;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide || getSpeed() == 0)
            return;
        EnderInfusingRecipe recipe = currentRecipe();
        if (recipe == null) {
            ticksSinceStep = 0;
            return;
        }
        // processingTime is the tick interval at speed 64; faster shafts process sooner.
        int interval = (int) Mth.clamp(recipe.processingTime() * 64f / Math.abs(getSpeed()), 2f, 200f);
        if (++ticksSinceStep < interval)
            return;
        ticksSinceStep = 0;
        applyRecipe(recipe);
    }

    /** Snapshot of every catalyst slot, in slot order. */
    private List<ItemStack> catalystStacks() {
        List<ItemStack> stacks = new java.util.ArrayList<>(catalyst.getSlots());
        for (int i = 0; i < catalyst.getSlots(); i++)
            stacks.add(catalyst.getStackInSlot(i));
        return stacks;
    }

    /**
     * The recipe currently matched by the input fluid + catalysts. When several match (e.g. a forward
     * recipe needing a catalyst and a catalyst-free reverse recipe on the same fluid), the one requiring
     * the MOST catalysts wins — so loading the catalyst runs the forward recipe and an empty catalyst
     * inventory runs the reverse.
     */
    private EnderInfusingRecipe currentRecipe() {
        if (level == null || input.isEmpty())
            return null;
        EnderInfusingInput in = new EnderInfusingInput(input, catalystStacks());
        EnderInfusingRecipe best = null;
        for (RecipeHolder<EnderInfusingRecipe> holder :
                level.getRecipeManager().getAllRecipesFor(CESGRecipes.ENDER_INFUSING_TYPE.get())) {
            EnderInfusingRecipe r = holder.value();
            if (r.matches(in, level) && (best == null || r.catalysts().size() > best.catalysts().size()))
                best = r;
        }
        return best;
    }

    /** Consume one recipe's input fluid + catalysts and produce its output fluid, if there's room. */
    private void applyRecipe(EnderInfusingRecipe recipe) {
        int needFluid = recipe.input().amount();
        if (input.getAmount() < needFluid)
            return;
        FluidStack result = recipe.result();
        if (!output.isEmpty() && !FluidStack.isSameFluidSameComponents(output, result))
            return;
        if (TANK_CAPACITY - output.getAmount() < result.getAmount())
            return;
        if (!recipe.catalystsAvailable(catalystStacks()))
            return;
        if (!byproductsFit(recipe.resultItems()))
            return;
        consumeCatalysts(recipe);
        input.shrink(needFluid);
        if (input.getAmount() <= 0)
            input = FluidStack.EMPTY;
        if (output.isEmpty())
            output = result.copy();
        else
            output.grow(result.getAmount());
        for (ItemStack item : recipe.resultItems())
            ItemHandlerHelper.insertItem(catalyst, item.copy(), false);
        notifyUpdate();
    }

    /** True if every byproduct item would fit into the catalyst inventory (which doubles as item output). */
    private boolean byproductsFit(List<ItemStack> items) {
        if (items.isEmpty())
            return true;
        ItemStackHandler scratch = new ItemStackHandler(catalyst.getSlots());
        for (int i = 0; i < catalyst.getSlots(); i++)
            scratch.setStackInSlot(i, catalyst.getStackInSlot(i).copy());
        for (ItemStack item : items)
            if (!ItemHandlerHelper.insertItem(scratch, item.copy(), false).isEmpty())
                return false;
        return true;
    }

    /** Pull each required catalyst out of the slots (already verified to be available). */
    private void consumeCatalysts(EnderInfusingRecipe recipe) {
        for (SizedIngredient cat : recipe.catalysts()) {
            int need = cat.count();
            for (int i = 0; i < catalyst.getSlots() && need > 0; i++) {
                ItemStack slot = catalyst.getStackInSlot(i);
                if (!slot.isEmpty() && cat.ingredient().test(slot))
                    need -= catalyst.extractItem(i, need, false).getCount();
            }
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Input", input.saveOptional(registries));
        tag.put("Output", output.saveOptional(registries));
        tag.put("Catalyst", catalyst.serializeNBT(registries));
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        input = FluidStack.parseOptional(registries, tag.getCompound("Input"));
        output = FluidStack.parseOptional(registries, tag.getCompound("Output"));
        catalyst.deserializeNBT(registries, tag.getCompound("Catalyst"));
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        CESGLang.forGoggles(tooltip, "cesg.goggles.ender_infuser.input", ChatFormatting.AQUA,
                fluidName(input), input.getAmount(), TANK_CAPACITY);
        CESGLang.forGoggles(tooltip, "cesg.goggles.ender_infuser.output", ChatFormatting.GREEN,
                fluidName(output), output.getAmount(), TANK_CAPACITY);
        for (int i = 0; i < catalyst.getSlots(); i++) {
            ItemStack cat = catalyst.getStackInSlot(i);
            if (!cat.isEmpty())
                CESGLang.forGoggles(tooltip, "cesg.goggles.ender_infuser.catalyst", ChatFormatting.GOLD,
                        cat.getCount(), cat.getHoverName());
        }
        if (getSpeed() == 0) {
            CESGLang.forGoggles(tooltip, "cesg.goggles.ender_infuser.unpowered", ChatFormatting.GRAY);
        } else {
            EnderInfusingRecipe recipe = currentRecipe();
            if (recipe != null)
                CESGLang.forGoggles(tooltip, "cesg.goggles.ender_infuser.producing", ChatFormatting.LIGHT_PURPLE,
                        recipe.result().getHoverName());
            else if (!input.isEmpty())
                CESGLang.forGoggles(tooltip, "cesg.goggles.ender_infuser.no_recipe", ChatFormatting.GRAY);
        }
        return true;
    }

    private static Component fluidName(FluidStack stack) {
        return stack.isEmpty() ? Component.translatable("cesg.goggles.ender_infuser.empty") : stack.getHoverName();
    }
}
