package com.cesg.init;

import com.cesg.CESG;
import com.cesg.potion.TeleportEffect;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CESGEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, CESG.MOD_ID);

    /** Periodically blinks the holder to a random nearby location. */
    public static final Holder<MobEffect> TELEPORT = EFFECTS.register("teleport",
            () -> new TeleportEffect(MobEffectCategory.NEUTRAL, 0x9B59B6));

    /** Marker effect; a handler cancels every teleport (vanilla + gateway) while it is active. */
    public static final Holder<MobEffect> TELEPORT_RESISTANCE = EFFECTS.register("teleport_resistance",
            () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x44BBAA) {});

    public static void register(IEventBus modEventBus) {
        EFFECTS.register(modEventBus);
    }
}
