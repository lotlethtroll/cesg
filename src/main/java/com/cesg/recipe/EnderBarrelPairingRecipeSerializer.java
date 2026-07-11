package com.cesg.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

/** Same JSON/network shape as a vanilla shaped recipe, decoded into {@link EnderBarrelPairingRecipe}. */
public class EnderBarrelPairingRecipeSerializer implements RecipeSerializer<EnderBarrelPairingRecipe> {
    private static final MapCodec<EnderBarrelPairingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.STRING.optionalFieldOf("group", "").forGetter(ShapedRecipe::getGroup),
                    CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC)
                            .forGetter(ShapedRecipe::category),
                    ShapedRecipePattern.MAP_CODEC.forGetter(recipe -> recipe.patternCopy),
                    ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.resultCopy))
            .apply(instance, EnderBarrelPairingRecipe::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, EnderBarrelPairingRecipe> STREAM_CODEC =
            StreamCodec.of(EnderBarrelPairingRecipeSerializer::toNetwork,
                    EnderBarrelPairingRecipeSerializer::fromNetwork);

    private static void toNetwork(RegistryFriendlyByteBuf buffer, EnderBarrelPairingRecipe recipe) {
        buffer.writeUtf(recipe.getGroup());
        buffer.writeEnum(recipe.category());
        ShapedRecipePattern.STREAM_CODEC.encode(buffer, recipe.patternCopy);
        ItemStack.STREAM_CODEC.encode(buffer, recipe.resultCopy);
    }

    private static EnderBarrelPairingRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
        String group = buffer.readUtf();
        CraftingBookCategory category = buffer.readEnum(CraftingBookCategory.class);
        ShapedRecipePattern pattern = ShapedRecipePattern.STREAM_CODEC.decode(buffer);
        ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
        return new EnderBarrelPairingRecipe(group, category, pattern, result);
    }

    @Override
    public MapCodec<EnderBarrelPairingRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, EnderBarrelPairingRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
