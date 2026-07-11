package com.cesg.compat.emi;

import java.util.ArrayList;
import java.util.List;

import com.cesg.recipe.EnderInfusingRecipe;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;

/** EMI display for one {@code cesg:ender_infusing} recipe: input fluid (+ catalysts) -> output fluid. */
public class EnderInfusingEmiRecipe implements EmiRecipe {
    private final EmiRecipeCategory category;
    private final ResourceLocation id;
    private final EmiIngredient inputFluid;
    private final List<EmiIngredient> catalysts;
    private final EmiStack outputFluid;
    private final List<EmiStack> outputItems;
    private final List<EmiIngredient> inputs;
    private final List<EmiStack> outputs;
    private final int time;

    private final int catalystX;
    private final int arrowX;
    private final int outputX;
    private final int width;

    public EnderInfusingEmiRecipe(EmiRecipeCategory category, ResourceLocation id, EnderInfusingRecipe recipe) {
        this.category = category;
        this.id = id;
        this.time = recipe.processingTime();

        // EMI's NeoForge port measures fluids in mB, so the recipe amounts are used directly.
        long inAmount = recipe.input().amount();
        List<EmiStack> fluidOptions = new ArrayList<>();
        for (FluidStack fs : recipe.input().getFluids())
            fluidOptions.add(EmiStack.of(fs.getFluid(), inAmount));
        this.inputFluid = EmiIngredient.of(fluidOptions);

        this.catalysts = new ArrayList<>();
        for (SizedIngredient cat : recipe.catalysts())
            catalysts.add(EmiIngredient.of(cat.ingredient(), cat.count()));

        this.outputFluid = EmiStack.of(recipe.result().getFluid(), recipe.result().getAmount());
        this.outputItems = new ArrayList<>();
        for (net.minecraft.world.item.ItemStack item : recipe.resultItems())
            outputItems.add(EmiStack.of(item));

        this.inputs = new ArrayList<>();
        inputs.add(inputFluid);
        inputs.addAll(catalysts);
        this.outputs = new ArrayList<>();
        outputs.add(outputFluid);
        outputs.addAll(outputItems);

        // Layout: [fluid] [catalyst...]  ==>  [output fluid] [byproduct items...]
        this.catalystX = 22;
        int afterCatalysts = catalystX + Math.max(1, catalysts.size()) * 18;
        this.arrowX = afterCatalysts + 4;
        this.outputX = arrowX + 28;
        this.width = outputX + 18 + outputItems.size() * 18;
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return category;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return inputs;
    }

    @Override
    public List<EmiStack> getOutputs() {
        return outputs;
    }

    @Override
    public int getDisplayWidth() {
        return width;
    }

    @Override
    public int getDisplayHeight() {
        return 28;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        int y = 5;
        widgets.addSlot(inputFluid, 0, y);
        int x = catalystX;
        for (EmiIngredient cat : catalysts) {
            widgets.addSlot(cat, x, y);
            x += 18;
        }
        widgets.addFillingArrow(arrowX, y + 1, Math.max(1, time) * 50);
        widgets.addSlot(outputFluid, outputX, y).recipeContext(this);
        int ox = outputX + 18;
        for (EmiStack item : outputItems) {
            widgets.addSlot(item, ox, y).recipeContext(this);
            ox += 18;
        }
    }
}
