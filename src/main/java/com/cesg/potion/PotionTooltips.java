package com.cesg.potion;

import com.cesg.CESG;
import com.cesg.init.CESGEffects;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Potions are vanilla items, so they don't get Create ItemDescription tooltips — this appends the
 * scope line for Warp Resistance (it stops teleports, not dimension-portal travel).
 */
@EventBusSubscriber(modid = CESG.MOD_ID, value = Dist.CLIENT)
public final class PotionTooltips {
    private PotionTooltips() {}

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        PotionContents contents = event.getItemStack().get(DataComponents.POTION_CONTENTS);
        if (contents == null)
            return;
        for (MobEffectInstance instance : contents.getAllEffects()) {
            if (instance.getEffect().value() == CESGEffects.TELEPORT_RESISTANCE.value()) {
                event.getToolTip().add(Component.translatable("cesg.tooltip.warp_resistance")
                        .withStyle(ChatFormatting.GRAY));
                return;
            }
        }
    }
}
