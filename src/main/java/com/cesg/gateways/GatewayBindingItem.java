package com.cesg.gateways;

import com.cesg.gateways.teleport.GatewayPartner;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;

public class GatewayBindingItem extends Item {
    public GatewayBindingItem(Properties properties) {
        super(properties);
    }

    /** Sneak + use on a gateway core to store its coordinates on the crystal. */
    public static void imprint(ServerLevel level, BlockPos pos, ItemStack stack, ServerPlayer player) {
        GatewayPartner self = new GatewayPartner(level.dimension(), pos, true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(self.save()));
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

        GatewayPartner self = new GatewayPartner(level.dimension(), pos, true);
        target.setPartner(crystalPartner);

        ServerLevel partnerLevel = level.getServer().getLevel(crystalPartner.dimension());
        if (partnerLevel != null) {
            BlockEntity partnerBe = partnerLevel.getBlockEntity(crystalPartner.position());
            if (partnerBe instanceof CrossDimensionalGatewayCoreBlockEntity partnerGateway)
                partnerGateway.setPartner(self);
        }

        player.displayClientMessage(Component.translatable("cesg.gateway.bound_success"), true);
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
}
