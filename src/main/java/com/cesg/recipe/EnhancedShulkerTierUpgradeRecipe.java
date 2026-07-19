package com.cesg.recipe;

import com.cesg.init.CESGRegistration;
import com.cesg.init.CESGRecipes;
import com.cesg.upgrades.EnhancedShulkerUpgrades;
import com.simibubi.create.AllBlocks;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Migrates inventory when upgrading a vanilla shulker to enhanced tier 2, or when raising an enhanced
 * shulker to tier 3 / 4. All tiers use fixed 3x3 patterns so crafting consumes the correct ingredients.
 */
public record EnhancedShulkerTierUpgradeRecipe(int targetTier) implements CraftingRecipe {
    private static final int CENTER = 4;

    /** Tier 2: shells around a vanilla shulker box. */
    private static final GridSlot[] TIER_2 = {
            shell(0), shell(1), shell(2),
            shell(3), vanillaBox(4), shell(5),
            shell(6), shell(7), shell(8)
    };

    /**
     * Tier 3: 5 shells, 2 pearls, 1 andesite casing around a tier-2 enhanced box.
     * <pre>
     * P S P
     * S B S
     * S C S
     * </pre>
     */
    private static final GridSlot[] TIER_3 = {
            pearl(0), shell(1), pearl(2),
            shell(3), enhancedBox(4, 2), shell(5),
            shell(6), andesite(7), shell(8)
    };

    /**
     * Tier 4: 5 shells, 2 ender eyes, 1 brass casing around a tier-3 enhanced box.
     * <pre>
     * E S E
     * S B S
     * S C S
     * </pre>
     */
    private static final GridSlot[] TIER_4 = {
            enderEye(0), shell(1), enderEye(2),
            shell(3), enhancedBox(4, 3), shell(5),
            shell(6), brass(7), shell(8)
    };

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.width() < 3 || input.height() < 3)
            return false;
        return switch (targetTier) {
            case 2 -> matchesPattern(input, TIER_2);
            case 3 -> matchesPattern(input, TIER_3);
            case 4 -> matchesPattern(input, TIER_4);
            default -> false;
        };
    }

    private static boolean matchesPattern(CraftingInput input, GridSlot[] pattern) {
        for (GridSlot slot : pattern) {
            if (!slot.matches(input.getItem(slot.index())))
                return false;
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack source = input.getItem(CENTER);
        if (source.isEmpty())
            return ItemStack.EMPTY;
        return EnhancedShulkerUpgrades.buildUpgradedStack(source, targetTier);
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        ItemStack preview = new ItemStack(com.cesg.upgrades.EnhancedShulkerBoxes.DEFAULT.get());
        preview.set(com.cesg.init.CESGDataComponents.ENHANCED_SHULKER.get(),
                com.cesg.upgrades.EnhancedShulkerContents.forTier(targetTier));
        return preview;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        GridSlot[] pattern = switch (targetTier) {
            case 2 -> TIER_2;
            case 3 -> TIER_3;
            case 4 -> TIER_4;
            default -> new GridSlot[0];
        };
        NonNullList<Ingredient> ingredients = NonNullList.create();
        for (GridSlot slot : pattern) {
            if (!slot.empty())
                ingredients.add(slot.ingredient());
        }
        return ingredients;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return CESGRecipes.ENHANCED_SHULKER_TIER_UPGRADE.get();
    }

    private interface GridSlot {
        int index();

        boolean empty();

        Ingredient ingredient();

        boolean matches(ItemStack stack);
    }

    private static GridSlot shell(int index) {
        return fixed(index, Ingredient.of(CESGRegistration.SHULKER_SHELL.get()),
                stack -> stack.is(CESGRegistration.SHULKER_SHELL.get()));
    }

    private static GridSlot pearl(int index) {
        return fixed(index, Ingredient.of(Items.ENDER_PEARL), stack -> stack.is(Items.ENDER_PEARL));
    }

    private static GridSlot enderEye(int index) {
        return fixed(index, Ingredient.of(Items.ENDER_EYE), stack -> stack.is(Items.ENDER_EYE));
    }

    private static GridSlot andesite(int index) {
        return fixed(index, Ingredient.of(AllBlocks.ANDESITE_CASING.asItem()),
                stack -> stack.is(AllBlocks.ANDESITE_CASING.asItem()));
    }

    private static GridSlot brass(int index) {
        return fixed(index, Ingredient.of(AllBlocks.BRASS_CASING.asItem()),
                stack -> stack.is(AllBlocks.BRASS_CASING.asItem()));
    }

    private static GridSlot vanillaBox(int index) {
        return new GridSlot() {
            @Override
            public int index() {
                return index;
            }

            @Override
            public boolean empty() {
                return false;
            }

            @Override
            public Ingredient ingredient() {
                return Ingredient.of(Items.SHULKER_BOX);
            }

            @Override
            public boolean matches(ItemStack stack) {
                return EnhancedShulkerUpgrades.isVanillaShulkerBox(stack);
            }
        };
    }

    private static GridSlot enhancedBox(int index, int requiredTier) {
        return new GridSlot() {
            @Override
            public int index() {
                return index;
            }

            @Override
            public boolean empty() {
                return false;
            }

            @Override
            public Ingredient ingredient() {
                return Ingredient.of(com.cesg.upgrades.EnhancedShulkerBoxes.DEFAULT.get());
            }

            @Override
            public boolean matches(ItemStack stack) {
                return EnhancedShulkerUpgrades.tierOf(stack) == requiredTier;
            }
        };
    }

    private static GridSlot fixed(int index, Ingredient ingredient, java.util.function.Predicate<ItemStack> test) {
        return new GridSlot() {
            @Override
            public int index() {
                return index;
            }

            @Override
            public boolean empty() {
                return false;
            }

            @Override
            public Ingredient ingredient() {
                return ingredient;
            }

            @Override
            public boolean matches(ItemStack stack) {
                return !stack.isEmpty() && test.test(stack);
            }
        };
    }
}
