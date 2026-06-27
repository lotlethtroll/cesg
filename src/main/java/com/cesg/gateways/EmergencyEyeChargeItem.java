package com.cesg.gateways;

import com.cesg.gateways.teleport.TeleportResolver;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class EmergencyEyeChargeItem extends Item {
    public EmergencyEyeChargeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide)
            return InteractionResultHolder.sidedSuccess(stack, true);

        if (!(player instanceof ServerPlayer serverPlayer))
            return InteractionResultHolder.pass(stack);

        ServerLevel end = serverPlayer.server.getLevel(Level.END);
        if (end == null)
            return InteractionResultHolder.fail(stack);

        TeleportResolver.teleportToCentralIsland(serverPlayer, end);
        if (!player.getAbilities().instabuild)
            stack.shrink(1);

        return InteractionResultHolder.success(stack);
    }
}
