package com.cesg.potion;

import com.cesg.CESG;
import com.cesg.init.CESGPotions;
import com.cesg.init.CESGRegistration;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;

/**
 * Brewing chain for the teleport potions, all rooted in {@code ender_pearl_dust}:
 * awkward + ender pearl dust → Teleport; + redstone → long; + fermented spider eye → Teleport Resistance.
 * {@link RegisterBrewingRecipesEvent} fires on the game bus, so this subscribes there.
 */
@EventBusSubscriber(modid = CESG.MOD_ID)
public final class CESGBrewing {
    private CESGBrewing() {}

    @SubscribeEvent
    public static void register(RegisterBrewingRecipesEvent event) {
        var builder = event.getBuilder();

        builder.addMix(Potions.AWKWARD, CESGRegistration.ENDER_PEARL_DUST.get(), CESGPotions.TELEPORT);
        builder.addMix(CESGPotions.TELEPORT, Items.REDSTONE, CESGPotions.LONG_TELEPORT);

        // Fermented spider eye corrupts Teleportation into its opposite, Teleport Resistance.
        builder.addMix(CESGPotions.TELEPORT, Items.FERMENTED_SPIDER_EYE, CESGPotions.TELEPORT_RESISTANCE);
        builder.addMix(CESGPotions.LONG_TELEPORT, Items.FERMENTED_SPIDER_EYE, CESGPotions.LONG_TELEPORT_RESISTANCE);
        builder.addMix(CESGPotions.TELEPORT_RESISTANCE, Items.REDSTONE, CESGPotions.LONG_TELEPORT_RESISTANCE);
    }
}
