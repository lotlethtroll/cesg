package com.cesg.init;

import com.cesg.CESG;
import com.cesg.recipe.EnhancedShulkerTierUpgradeRecipe;
import com.cesg.recipe.EnhancedShulkerTierUpgradeRecipeSerializer;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CESGRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, CESG.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<EnhancedShulkerTierUpgradeRecipe>> ENHANCED_SHULKER_TIER_UPGRADE =
            SERIALIZERS.register("enhanced_shulker_tier_upgrade", EnhancedShulkerTierUpgradeRecipeSerializer::new);

    private CESGRecipes() {}

    public static void register(IEventBus modEventBus) {
        SERIALIZERS.register(modEventBus);
    }
}
