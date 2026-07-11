package com.cesg.storage.enderbarrel;

import java.util.List;
import java.util.UUID;

import com.cesg.init.CESGDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/** Block item that surfaces the pair code, so twins can be matched from a chest full of barrels. */
public class EnderBarrelBlockItem extends BlockItem {
    public EnderBarrelBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        UUID pairId = stack.get(CESGDataComponents.ENDER_BARREL_PAIR.get());
        if (pairId != null)
            tooltip.add(Component.translatable("cesg.barrel.pair", pairId.toString().substring(0, 8))
                    .withStyle(ChatFormatting.AQUA));
        else
            tooltip.add(Component.translatable("cesg.barrel.unpaired").withStyle(ChatFormatting.GRAY));
    }
}
