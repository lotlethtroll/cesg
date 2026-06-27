package com.cesg.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class EnhancedShulkerTierUpgradeRecipeSerializer
        implements RecipeSerializer<EnhancedShulkerTierUpgradeRecipe> {
    public static final MapCodec<EnhancedShulkerTierUpgradeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(Codec.INT.fieldOf("target_tier").forGetter(EnhancedShulkerTierUpgradeRecipe::targetTier))
                    .apply(instance, EnhancedShulkerTierUpgradeRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, EnhancedShulkerTierUpgradeRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, EnhancedShulkerTierUpgradeRecipe::targetTier,
                    EnhancedShulkerTierUpgradeRecipe::new);

    @Override
    public MapCodec<EnhancedShulkerTierUpgradeRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, EnhancedShulkerTierUpgradeRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
