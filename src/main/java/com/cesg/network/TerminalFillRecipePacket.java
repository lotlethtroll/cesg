package com.cesg.network;

import com.cesg.CESG;
import com.cesg.storage.network.StorageTerminalMenu;
import com.simibubi.create.foundation.utility.AdventureUtil;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> server: the recipe-viewer "+" transfer for the Storage Terminal (Phase 7F, folded from 7C).
 * Carries the crafting recipe id; the server places one of each ingredient into the 3×3 grid, pulling
 * from the player inventory first and the storage network second. Viewer-agnostic — JEI and EMI both
 * send this.
 */
public record TerminalFillRecipePacket(int containerId, ResourceLocation recipeId) implements CustomPacketPayload {
    public static final Type<TerminalFillRecipePacket> TYPE = new Type<>(CESG.id("terminal_fill_recipe"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalFillRecipePacket> STREAM_CODEC =
            StreamCodec.composite(
                    net.minecraft.network.codec.ByteBufCodecs.VAR_INT, TerminalFillRecipePacket::containerId,
                    ResourceLocation.STREAM_CODEC, TerminalFillRecipePacket::recipeId,
                    TerminalFillRecipePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TerminalFillRecipePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player == null || player.isSpectator() || AdventureUtil.isAdventure(player))
                return;
            if (!(player.containerMenu instanceof StorageTerminalMenu menu)
                    || menu.containerId != packet.containerId()
                    || !menu.stillValid(player))
                return;
            RecipeHolder<?> holder = player.level().getRecipeManager().byKey(packet.recipeId()).orElse(null);
            if (holder != null && holder.value() instanceof CraftingRecipe recipe)
                menu.fillFromRecipe(recipe);
        });
    }
}
