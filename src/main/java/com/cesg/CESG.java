package com.cesg;

import org.slf4j.Logger;

import com.cesg.init.CESGCapabilities;
import com.cesg.init.CESGCreativeTabs;
import com.cesg.init.CESGDataComponents;
import com.cesg.init.CESGRegistration;
import com.cesg.init.CESGRecipes;
import com.cesg.init.CESGData;
import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;

import net.createmod.catnip.lang.FontHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@Mod(CESG.MOD_ID)
public class CESG {
    public static final String MOD_ID = "cesg";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MOD_ID)
            .defaultCreativeTab(CESGCreativeTabs.TAB.getKey())
            .setTooltipModifierFactory(item ->
                    new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
                            .andThen(TooltipModifier.mapNull(KineticStats.create(item))));

    public CESG(IEventBus modEventBus, net.neoforged.fml.ModContainer modContainer) {
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER, CESGConfig.SPEC);
        REGISTRATE.registerEventListeners(modEventBus);
        CESGRegistration.register(modEventBus);
        CESGRecipes.register(modEventBus);
        CESGCapabilities.register(modEventBus);
        CESGDataComponents.register(modEventBus);
        com.cesg.init.CESGEffects.register(modEventBus);
        com.cesg.init.CESGSounds.register(modEventBus);
        com.cesg.init.CESGPotions.register(modEventBus);
        modEventBus.addListener(com.cesg.init.CESGPotions::addToCreativeTab);

        modEventBus.addListener(com.cesg.gateways.GatewayChunkLoader::registerControllers);
        modEventBus.addListener(this::gatherData);

        LOGGER.info("Create: End Storage & Gateways initialized");
    }

    private void gatherData(GatherDataEvent event) {
        CESGData.gather(event);
    }
}
