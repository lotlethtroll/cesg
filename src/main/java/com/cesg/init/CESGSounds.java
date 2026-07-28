package com.cesg.init;

import com.cesg.CESG;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * CESG's small shared sound vocabulary. The resource definitions currently layer and pitch-shift
 * familiar Minecraft sounds, so bespoke recordings can be substituted later without touching code.
 */
public final class CESGSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, CESG.MOD_ID);

    public static final Holder<SoundEvent> PORTAL_OPEN = register("portal_open");
    public static final Holder<SoundEvent> PORTAL_CLOSE = register("portal_close");
    public static final Holder<SoundEvent> TELEPORT = register("teleport");
    public static final Holder<SoundEvent> MACHINE_PROCESS = register("machine_process");
    public static final Holder<SoundEvent> LINK_LIVE = register("link_live");
    public static final Holder<SoundEvent> LINK_FAULT = register("link_fault");
    public static final Holder<SoundEvent> TRANSFER = register("transfer");

    private CESGSounds() {}

    private static Holder<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(CESG.id(name)));
    }

    public static void register(IEventBus modEventBus) {
        SOUNDS.register(modEventBus);
    }
}
