package com.cesg.init;

import java.util.List;

import com.cesg.CESG;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CESGPotions {
    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(Registries.POTION, CESG.MOD_ID);

    private static final int BASE = 3600;  // 3:00
    private static final int LONG = 9600;  // 8:00

    public static final Holder<Potion> TELEPORT = POTIONS.register("teleport",
            () -> new Potion("teleport", new MobEffectInstance(CESGEffects.TELEPORT, BASE)));
    public static final Holder<Potion> LONG_TELEPORT = POTIONS.register("long_teleport",
            () -> new Potion("teleport", new MobEffectInstance(CESGEffects.TELEPORT, LONG)));

    public static final Holder<Potion> TELEPORT_RESISTANCE = POTIONS.register("teleport_resistance",
            () -> new Potion("teleport_resistance", new MobEffectInstance(CESGEffects.TELEPORT_RESISTANCE, BASE)));
    public static final Holder<Potion> LONG_TELEPORT_RESISTANCE = POTIONS.register("long_teleport_resistance",
            () -> new Potion("teleport_resistance", new MobEffectInstance(CESGEffects.TELEPORT_RESISTANCE, LONG)));

    public static void register(IEventBus modEventBus) {
        POTIONS.register(modEventBus);
    }

    /** Custom potions are not auto-listed; add every bottle/arrow form to the mod's creative tab. */
    public static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (!event.getTabKey().equals(CESGCreativeTabs.TAB.getKey()))
            return;
        for (Holder<Potion> potion : List.of(TELEPORT, LONG_TELEPORT, TELEPORT_RESISTANCE, LONG_TELEPORT_RESISTANCE))
            for (var item : List.of(Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION, Items.TIPPED_ARROW))
                event.accept(PotionContents.createItemStack(item, potion),
                        CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
    }
}
