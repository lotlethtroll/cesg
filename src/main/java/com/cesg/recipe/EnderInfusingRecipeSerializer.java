package com.cesg.recipe;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

public class EnderInfusingRecipeSerializer implements RecipeSerializer<EnderInfusingRecipe> {

    private static final MapCodec<EnderInfusingRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            SizedFluidIngredient.FLAT_CODEC.fieldOf("input").forGetter(EnderInfusingRecipe::input),
            SizedIngredient.FLAT_CODEC.listOf().optionalFieldOf("catalysts", List.of())
                    .forGetter(EnderInfusingRecipe::catalysts),
            FluidStack.CODEC.fieldOf("result").forGetter(EnderInfusingRecipe::result),
            ItemStack.CODEC.listOf().optionalFieldOf("result_items", List.of())
                    .forGetter(EnderInfusingRecipe::resultItems),
            Codec.INT.optionalFieldOf("processing_time", 8).forGetter(EnderInfusingRecipe::processingTime))
            .apply(inst, EnderInfusingRecipe::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, EnderInfusingRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    SizedFluidIngredient.STREAM_CODEC, EnderInfusingRecipe::input,
                    SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), EnderInfusingRecipe::catalysts,
                    FluidStack.STREAM_CODEC, EnderInfusingRecipe::result,
                    ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), EnderInfusingRecipe::resultItems,
                    ByteBufCodecs.INT, EnderInfusingRecipe::processingTime,
                    EnderInfusingRecipe::new);

    @Override
    public MapCodec<EnderInfusingRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, EnderInfusingRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
