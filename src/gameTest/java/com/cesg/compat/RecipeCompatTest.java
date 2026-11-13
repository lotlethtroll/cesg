package com.cesg.compat;

import java.util.ArrayList;
import java.util.List;

import com.cesg.init.CESGDataComponents;
import com.cesg.recipe.EnhancedAwareShulkerBoxColoring;
import com.cesg.upgrades.EnhancedShulkerBoxes;
import com.cesg.upgrades.EnhancedShulkerContents;
import com.mojang.logging.LogUtils;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Cross-mod recipe collisions. RecipeManager returns the first matching recipe from a list ordered by
 * recipe-ID hash buckets, and the bucket count depends on how many recipes the whole pack loads. Two
 * recipes accepting the same input therefore give a pack-dependent winner. Drop other mods' jars into
 * run/gametest/mods to audit a pack against these tests.
 */
@GameTestHolder("cesg")
@PrefixGameTestTemplate(false)
public class RecipeCompatTest {
    private static final ResourceLocation CESG_END_STONE =
            ResourceLocation.fromNamespaceAndPath("cesg", "ender_pearl_dust_from_end_stone_washing");

    /**
     * End stone crushing and milling are claimed by other addons (Aeronautics, Create Sifting, Create
     * Ultimate Factory), so CESG extracts pearl dust by washing instead and leaves both to them.
     */
    @GameTest(template = "empty")
    public void endStonePearlDustUsesWashingOnly(GameTestHelper helper) {
        Level level = helper.getLevel();
        SingleRecipeInput input = new SingleRecipeInput(new ItemStack(Items.END_STONE));
        List<ResourceLocation> crushing = matches(level, AllRecipeTypes.CRUSHING.getType(), input);
        List<ResourceLocation> milling = matches(level, AllRecipeTypes.MILLING.getType(), input);
        ResourceLocation washing = AllRecipeTypes.SPLASHING.find(input, level).map(RecipeHolder::id).orElse(null);
        LogUtils.getLogger().info("[cesg-compat] end_stone crushing={} milling={} washing->{}", crushing, milling,
                washing);

        helper.assertTrue(crushing.stream().noneMatch(id -> id.getNamespace().equals("cesg")),
                "CESG must not register end stone crushing: " + crushing);
        helper.assertTrue(milling.stream().noneMatch(id -> id.getNamespace().equals("cesg")),
                "CESG must not register end stone milling: " + milling);
        helper.assertTrue(CESG_END_STONE.equals(washing), "Washing end stone must give CESG pearl dust, got " + washing);
        helper.succeed();
    }

    /**
     * Dyeing an enhanced box must keep every component and stay an enhanced box, whichever color it
     * started as; vanilla boxes must dye exactly as in vanilla. Exactly one crafting recipe may match.
     */
    @GameTest(template = "empty")
    public void dyeingEnhancedShulkerKeepsContents(GameTestHelper helper) {
        Level level = helper.getLevel();
        var base = EnhancedShulkerContents.forTier(3);
        var contents = new EnhancedShulkerContents(3, base.slotCount(), base.upgradeSlotCount(),
                List.of(new EnhancedShulkerContents.StoredSlot(0, new ItemStack(Items.DIAMOND, 64))), List.of(),
                ItemStack.EMPTY);
        for (DyeColor from : new DyeColor[] {null, DyeColor.BLUE, DyeColor.RED}) {
            ItemStack box = new ItemStack(EnhancedShulkerBoxes.byColor(from).get());
            box.set(CESGDataComponents.ENHANCED_SHULKER.get(), contents);
            ItemStack out = craftSingle(helper, level, box, new ItemStack(Items.RED_DYE));
            helper.assertTrue(out.is(EnhancedShulkerBoxes.byColor(DyeColor.RED).get().asItem()),
                    "Dyed " + from + " enhanced box became " + out.getItem());
            var kept = out.get(CESGDataComponents.ENHANCED_SHULKER.get());
            helper.assertTrue(kept != null && kept.tier() == 3 && kept.mainSlots().size() == 1
                    && kept.mainSlots().get(0).item().is(Items.DIAMOND) && kept.mainSlots().get(0).item().getCount() == 64,
                    "Dyeing " + from + " enhanced box lost its tier or contents: " + kept);
        }
        ItemStack vanilla = craftSingle(helper, level, new ItemStack(Items.SHULKER_BOX), new ItemStack(Items.LIME_DYE));
        helper.assertTrue(vanilla.is(Items.LIME_SHULKER_BOX), "Vanilla shulker dyeing changed: " + vanilla.getItem());
        helper.succeed();
    }

    private static ItemStack craftSingle(GameTestHelper helper, Level level, ItemStack box, ItemStack dye) {
        List<ItemStack> grid = new ArrayList<>(List.of(box, dye));
        while (grid.size() < 9)
            grid.add(ItemStack.EMPTY);
        CraftingInput input = CraftingInput.of(3, 3, grid);
        List<RecipeHolder<CraftingRecipe>> found = level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)
                .stream().filter(h -> h.value().matches(input, level)).toList();
        helper.assertTrue(found.size() == 1 && found.get(0).value() instanceof EnhancedAwareShulkerBoxColoring,
                "Expected only CESG's shulker coloring to match, got " + found.stream().map(RecipeHolder::id).toList());
        return found.get(0).value().assemble(input, level.registryAccess());
    }

    /**
     * Every CESG recipe with a buildable input must be the only recipe of its type that accepts that
     * input; otherwise RecipeManager's hash order decides which one players get. Stonecutting is exempt
     * (the stonecutter lists every match). Run with other mods in run/gametest/mods to audit a pack.
     */
    @GameTest(template = "empty")
    public void cesgRecipesHaveNoSameInputTwin(GameTestHelper helper) {
        Level level = helper.getLevel();
        var recipes = level.getRecipeManager().getRecipes();
        List<String> conflicts = new ArrayList<>();
        int checked = 0;
        for (RecipeHolder<?> holder : recipes) {
            if (!holder.id().getNamespace().equals("cesg") || holder.value().getType() == RecipeType.STONECUTTING)
                continue;
            RecipeInput input = representativeInput(holder.value());
            if (input == null)
                continue;
            checked++;
            for (RecipeHolder<?> other : recipes)
                if (other != holder && other.value().getType() == holder.value().getType()
                        && accepts(other.value(), input, level))
                    conflicts.add(holder.id() + " <-> " + other.id());
        }
        LogUtils.getLogger().info("[cesg-compat] checked {} cesg recipes against {} loaded; conflicts={}", checked,
                recipes.size(), conflicts);
        helper.assertTrue(conflicts.isEmpty(), "Same-input recipe conflicts: " + conflicts);
        helper.succeed();
    }

    /** One concrete input per recipe (first stack of each ingredient), or null when not single-slot/grid. */
    private static RecipeInput representativeInput(Recipe<?> recipe) {
        List<ItemStack> stacks = recipe.getIngredients().stream()
                .map(i -> i.isEmpty() || i.getItems().length == 0 ? ItemStack.EMPTY : i.getItems()[0].copy()).toList();
        if (recipe instanceof ShapedRecipe shaped)
            return CraftingInput.of(shaped.getWidth(), shaped.getHeight(), stacks);
        if (recipe instanceof ShapelessRecipe) {
            List<ItemStack> grid = new ArrayList<>(stacks);
            while (grid.size() < 9)
                grid.add(ItemStack.EMPTY);
            return CraftingInput.of(3, 3, grid);
        }
        if (recipe instanceof CraftingRecipe || stacks.size() != 1 || stacks.get(0).isEmpty())
            return null; // custom crafting, mixing/compacting/filling (CESG items or fluids): not comparable
        if (recipe instanceof ProcessingRecipe<?, ?> p && !p.getFluidIngredients().isEmpty())
            return null;
        return new SingleRecipeInput(stacks.get(0));
    }

    @SuppressWarnings("unchecked")
    private static boolean accepts(Recipe<?> recipe, RecipeInput input, Level level) {
        try {
            return ((Recipe<RecipeInput>) recipe).matches(input, level);
        } catch (ClassCastException | IndexOutOfBoundsException e) {
            return false; // recipe expects a different input shape, so it cannot compete for this one
        }
    }

    @SuppressWarnings("unchecked")
    private static List<ResourceLocation> matches(Level level, RecipeType<?> type, SingleRecipeInput input) {
        return level.getRecipeManager()
                .getAllRecipesFor((RecipeType<Recipe<SingleRecipeInput>>) (RecipeType<?>) type).stream()
                .filter(h -> h.value().matches(input, level)).map(RecipeHolder::id).toList();
    }
}
