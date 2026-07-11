package com.cesg.upgrades;

import com.mojang.serialization.Codec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * Immutable wrapper so a single filter item can be stored as a data component. Vanilla
 * {@link ItemStack} is rejected by NeoForge's component validation (no suitable equals/hashCode).
 */
public record ConfiguredFilterStack(ItemStack stack) {
    public static final ConfiguredFilterStack EMPTY = new ConfiguredFilterStack(ItemStack.EMPTY);

    public static final Codec<ConfiguredFilterStack> CODEC =
            ItemStack.CODEC.xmap(ConfiguredFilterStack::of, ConfiguredFilterStack::stack);

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfiguredFilterStack> STREAM_CODEC =
            ItemStack.STREAM_CODEC.map(ConfiguredFilterStack::of, ConfiguredFilterStack::stack);

    public ConfiguredFilterStack {
        if (stack == null || stack.isEmpty())
            stack = ItemStack.EMPTY;
        else
            stack = stack.copyWithCount(1);
    }

    public static ConfiguredFilterStack of(ItemStack stack) {
        return stack.isEmpty() ? EMPTY : new ConfiguredFilterStack(stack);
    }
}
