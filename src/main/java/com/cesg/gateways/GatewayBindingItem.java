package com.cesg.gateways;

import com.cesg.gateways.teleport.GatewayPartner;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class GatewayBindingItem extends Item {
    public GatewayBindingItem(Properties properties) {
        super(properties);
    }

    public static void bind(ServerLevel level, BlockPos pos, ItemStack stack, CrossDimensionalGatewayCoreBlockEntity gateway) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof CrossDimensionalGatewayCoreBlockEntity source))
            return;

        if (!stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA))
            return;

        var data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (data == null)
            return;

        GatewayPartner partner = GatewayPartner.load(data.copyTag());
        source.setPartner(partner);
        gateway.setPartner(new GatewayPartner(level.dimension(), pos, true));
    }

    public static ItemStack createBinding(GatewayPartner partner) {
        ItemStack stack = new ItemStack(com.cesg.init.CESGRegistration.GATEWAY_BINDING_ITEM.get());
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(partner.save()));
        return stack;
    }
}
