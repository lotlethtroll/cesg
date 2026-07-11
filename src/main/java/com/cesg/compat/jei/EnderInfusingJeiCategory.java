package com.cesg.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.cesg.init.CESGRegistration;
import com.cesg.recipe.EnderInfusingRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * JEI display for {@code cesg:ender_infusing}: input fluid + up to 3 item catalysts -> output fluid
 * (+ optional item byproducts). Mirrors {@link com.cesg.compat.emi.EnderInfusingEmiRecipe}'s layout.
 */
public class EnderInfusingJeiCategory implements IRecipeCategory<RecipeHolder<EnderInfusingRecipe>> {
    private static final int CATALYST_X = 22;
    private static final int ARROW_X = CATALYST_X + 3 * 18 + 4;
    private static final int OUTPUT_X = ARROW_X + 28;
    private static final int SLOT_Y = 5;

    private final IDrawable icon;
    private final IDrawableAnimated arrow;

    public EnderInfusingJeiCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(CESGRegistration.ENDER_INFUSER.get()));
        this.arrow = guiHelper.createAnimatedRecipeArrow(60);
    }

    @Override
    public RecipeType<RecipeHolder<EnderInfusingRecipe>> getRecipeType() {
        return CESGJeiPlugin.enderInfusingType();
    }

    @Override
    public Component getTitle() {
        return Component.translatable("cesg.recipe.category.ender_infusing");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return OUTPUT_X + 18 + 2 * 18;
    }

    @Override
    public int getHeight() {
        return 28;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<EnderInfusingRecipe> holder,
            IFocusGroup focuses) {
        EnderInfusingRecipe recipe = holder.value();

        List<FluidStack> inputOptions = new ArrayList<>();
        for (FluidStack fs : recipe.input().getFluids())
            inputOptions.add(new FluidStack(fs.getFluid(), recipe.input().amount()));
        builder.addSlot(RecipeIngredientRole.INPUT, 1, SLOT_Y + 1)
                .addIngredients(NeoForgeTypes.FLUID_STACK, inputOptions)
                .setFluidRenderer(Math.max(1, recipe.input().amount()), false, 16, 16)
                .addRichTooltipCallback((view, tooltip) -> tooltip
                        .add(Component.translatable("cesg.recipe.ender_infusing.amount", recipe.input().amount())));

        int x = CATALYST_X;
        for (SizedIngredient cat : recipe.catalysts()) {
            List<ItemStack> stacks = new ArrayList<>();
            for (ItemStack stack : cat.ingredient().getItems())
                stacks.add(stack.copyWithCount(cat.count()));
            builder.addSlot(RecipeIngredientRole.INPUT, x + 1, SLOT_Y + 1).addItemStacks(stacks);
            x += 18;
        }

        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X + 1, SLOT_Y + 1)
                .addIngredient(NeoForgeTypes.FLUID_STACK, recipe.result())
                .setFluidRenderer(Math.max(1, recipe.result().getAmount()), false, 16, 16)
                .addRichTooltipCallback((view, tooltip) -> tooltip
                        .add(Component.translatable("cesg.recipe.ender_infusing.amount", recipe.result().getAmount())));

        int ox = OUTPUT_X + 18;
        for (ItemStack item : recipe.resultItems()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, ox + 1, SLOT_Y + 1).addItemStack(item);
            ox += 18;
        }
    }

    @Override
    public void draw(RecipeHolder<EnderInfusingRecipe> recipe, IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics, double mouseX, double mouseY) {
        arrow.draw(guiGraphics, ARROW_X, SLOT_Y + 1);
    }
}
