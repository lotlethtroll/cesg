package com.cesg.potion;

import com.cesg.CESG;
import com.cesg.init.CESGEffects;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;

/**
 * Teleport Resistance blocks every vanilla teleport (enderman grabs, chorus fruit, ender pearls, commands)
 * by cancelling the teleport event. Gateway travel is handled separately because it does not fire this
 * event (see {@link com.cesg.gateways.teleport.TeleportResolver#hasTeleportResistance}).
 */
@EventBusSubscriber(modid = CESG.MOD_ID)
public final class TeleportEvents {
    private TeleportEvents() {}

    @SubscribeEvent
    public static void onEntityTeleport(EntityTeleportEvent event) {
        if (event.getEntity() instanceof LivingEntity living && living.hasEffect(CESGEffects.TELEPORT_RESISTANCE))
            event.setCanceled(true);
    }
}
