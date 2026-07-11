package com.cesg.gateways;

import com.cesg.gateways.teleport.GatewayPartner;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class GatewayBindingItem extends Item {
    public GatewayBindingItem(Properties properties) {
        super(properties);
    }

    /**
     * Handles using the crystal on a gateway core. Lives on the item (not just the block) because
     * sneak + use bypasses the block's {@code useItemOn} and routes to the item's {@code useOn} instead.
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof CrossDimensionalGatewayCoreBlockEntity))
            return InteractionResult.PASS;
        if (level instanceof ServerLevel serverLevel && context.getPlayer() instanceof ServerPlayer player)
            handleUse(serverLevel, pos, context.getItemInHand(), player);
        return InteractionResult.SUCCESS;
    }

    /** Sneak imprints the core's location; otherwise binds an imprinted crystal to this core. */
    public static void handleUse(ServerLevel level, BlockPos pos, ItemStack stack, ServerPlayer player) {
        if (player.isShiftKeyDown())
            imprint(level, pos, stack, player);
        else if (isImprinted(stack))
            applyBinding(level, pos, stack, player);
        else
            player.displayClientMessage(Component.translatable("cesg.gateway.crystal_empty"), true);
    }

    /**
     * Sneak + use on a gateway core to store its coordinates — and its active channel, so the
     * reciprocal binding lands on the channel the player had selected when imprinting.
     */
    public static void imprint(ServerLevel level, BlockPos pos, ItemStack stack, ServerPlayer player) {
        String name = "";
        int channel = 0;
        if (level.getBlockEntity(pos) instanceof CrossDimensionalGatewayCoreBlockEntity core) {
            name = core.getGatewayName();
            channel = core.getActiveChannel();
        }
        GatewayPartner self = new GatewayPartner(level.dimension(), pos, true, name);
        var tag = self.save();
        tag.putInt("Channel", channel);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        player.displayClientMessage(Component.translatable("cesg.gateway.crystal_imprinted"), true);
    }

    /** Use an imprinted crystal on a gateway to bind it to the stored partner (bidirectional). */
    public static void applyBinding(ServerLevel level, BlockPos pos, ItemStack stack, ServerPlayer player) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null)
            return;

        GatewayPartner crystalPartner = GatewayPartner.load(data.copyTag());
        if (!crystalPartner.isBound()) {
            player.displayClientMessage(Component.translatable("cesg.gateway.crystal_empty"), true);
            return;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof CrossDimensionalGatewayCoreBlockEntity target))
            return;

        if (crystalPartner.dimension().equals(level.dimension()) && crystalPartner.position().equals(pos)) {
            player.displayClientMessage(Component.translatable("cesg.gateway.crystal_self"), true);
            return;
        }

        GatewayPartner self = new GatewayPartner(level.dimension(), pos, true, target.getGatewayName());
        int crystalChannel = data.copyTag().getInt("Channel");
        // This side binds on its currently selected channel; the imprinted side binds on the channel
        // that was active when the crystal was imprinted. Warn when overwriting an existing binding.
        boolean replaced = target.getBinding(target.getActiveChannel()).isBound();
        target.setBinding(target.getActiveChannel(), crystalPartner);

        ServerLevel partnerLevel = level.getServer().getLevel(crystalPartner.dimension());
        if (partnerLevel != null) {
            BlockEntity partnerBe = partnerLevel.getBlockEntity(crystalPartner.position());
            if (partnerBe instanceof CrossDimensionalGatewayCoreBlockEntity partnerGateway)
                partnerGateway.setBinding(crystalChannel, self);
        }

        player.displayClientMessage(Component.translatable(
                replaced ? "cesg.gateway.bound_success_channel_replaced" : "cesg.gateway.bound_success_channel",
                target.getActiveChannel() + 1, crystalChannel + 1), true);
    }

    public static ItemStack createBinding(GatewayPartner partner) {
        ItemStack stack = new ItemStack(com.cesg.init.CESGRegistration.GATEWAY_BINDING_ITEM.get());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(partner.save()));
        return stack;
    }

    public static boolean isImprinted(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && GatewayPartner.load(data.copyTag()).isBound();
    }

    /** Shows what the crystal is imprinted with — the gateway's name, location, and return channel. */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
            java.util.List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        var tag = data == null ? null : data.copyTag();
        GatewayPartner partner = tag == null ? GatewayPartner.EMPTY : GatewayPartner.load(tag);
        if (!partner.isBound()) {
            tooltip.add(Component.translatable("cesg.crystal.empty")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
            return;
        }
        if (partner.hasName())
            tooltip.add(Component.translatable("cesg.crystal.target_named", partner.name())
                    .withStyle(net.minecraft.ChatFormatting.AQUA));
        tooltip.add(Component.translatable("cesg.crystal.location",
                partner.dimension().location().toString(), partner.position().getX(),
                partner.position().getY(), partner.position().getZ())
                .withStyle(net.minecraft.ChatFormatting.GRAY));
        tooltip.add(Component.translatable("cesg.crystal.channel", tag.getInt("Channel") + 1)
                .withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
