package com.cesg.init;

import com.cesg.CESG;
import com.cesg.recipe.EnderInfusingRecipe;
import com.cesg.recipe.EnderInfusingRecipeSerializer;
import com.cesg.recipe.EnhancedShulkerTierUpgradeRecipe;
import com.cesg.recipe.EnhancedShulkerTierUpgradeRecipeSerializer;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CESGRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, CESG.MOD_ID);
    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, CESG.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<EnhancedShulkerTierUpgradeRecipe>> ENHANCED_SHULKER_TIER_UPGRADE =
            SERIALIZERS.register("enhanced_shulker_tier_upgrade", EnhancedShulkerTierUpgradeRecipeSerializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<EnderInfusingRecipe>> ENDER_INFUSING_TYPE =
            TYPES.register("ender_infusing", () -> new RecipeType<EnderInfusingRecipe>() {
                @Override
                public String toString() {
                    return "cesg:ender_infusing";
                }
            });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<EnderInfusingRecipe>> ENDER_INFUSING_SERIALIZER =
            SERIALIZERS.register("ender_infusing", EnderInfusingRecipeSerializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<com.cesg.recipe.EnderBarrelPairingRecipe>> ENDER_BARREL_PAIRING =
            SERIALIZERS.register("ender_barrel_pairing", com.cesg.recipe.EnderBarrelPairingRecipeSerializer::new);

    private CESGRecipes() {}

    public static void register(IEventBus modEventBus) {
        SERIALIZERS.register(modEventBus);
        TYPES.register(modEventBus);
    }
}
