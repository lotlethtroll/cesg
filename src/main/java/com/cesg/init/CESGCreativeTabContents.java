package com.cesg.init;

import com.cesg.CESG;
import com.cesg.upgrades.EnhancedShulkerBoxes;
import com.cesg.upgrades.EnhancedShulkerContents;

import net.minecraft.world.item.DyeColor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@EventBusSubscriber(modid = CESG.MOD_ID)
public final class CESGCreativeTabContents {
    private CESGCreativeTabContents() {}

    @SubscribeEvent
    public static void appendTabEntries(BuildCreativeModeTabContentsEvent event) {
        if (!event.getTabKey().equals(CESGCreativeTabs.TAB.getKey()))
            return;
        for (int tier = 3; tier <= EnhancedShulkerContents.MAX_TIER; tier++) {
            event.accept(EnhancedShulkerBoxes.stackWithTier(tier, null));
            for (DyeColor color : DyeColor.values())
                event.accept(EnhancedShulkerBoxes.stackWithTier(tier, color));
        }
    }
}
