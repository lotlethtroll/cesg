package com.cesg;

import com.cesg.client.CESGKineticVisuals;
import com.cesg.client.CESGMenuScreens;
import com.cesg.client.CESGPartialModels;
import com.cesg.client.CESGFluidRenderers;
import com.cesg.client.EnhancedShulkerBoxItemRenderer;
import com.cesg.client.EnhancedShulkerBoxRenderer;
import com.cesg.client.GatewayCoreRenderer;
import com.cesg.client.GatewayFluxBatteryRenderer;
import com.cesg.client.ShulkerBeltLoaderRenderer;
import com.cesg.client.ShulkerBeltUnloaderRenderer;
import com.cesg.client.ShulkerCageRenderer;
import com.cesg.client.ShulkerStationRenderer;
import com.cesg.init.CESGBlockEntities;

import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBufferCache;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

@Mod(value = CESG.MOD_ID, dist = Dist.CLIENT)
public class CESGClient {
    public CESGClient(IEventBus modEventBus) {
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(CESGClient::registerRenderers);
        modEventBus.addListener(CESGClient::registerAdditionalModels);
        modEventBus.addListener(CESGClient::registerClientExtensions);
    }

    private static void registerClientExtensions(
            net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            // Created lazily: this event fires before Minecraft's renderers are ready.
            private EnhancedShulkerBoxItemRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null)
                    renderer = new EnhancedShulkerBoxItemRenderer();
                return renderer;
            }
        }, com.cesg.upgrades.EnhancedShulkerBoxes.allEntries().stream()
                .map(entry -> entry.get().asItem())
                .toArray(net.minecraft.world.item.Item[]::new));
    }

    private void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            SuperByteBufferCache.getInstance().registerCompartment(CachedBuffers.PARTIAL);
            CESGMenuScreens.register();
            CESGFluidRenderers.register();
            CESGPartialModels.init();
            CESGKineticVisuals.register();
            net.createmod.ponder.foundation.PonderIndex.addPlugin(new com.cesg.ponder.CESGPonderPlugin());
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                    com.cesg.init.CESGRegistration.GATEWAY_PORTAL.get(),
                    net.minecraft.client.renderer.RenderType.translucent());
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                    com.cesg.init.CESGRegistration.END_GATEWAY.get(),
                    net.minecraft.client.renderer.RenderType.translucent());
        });
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(CESGBlockEntities.ENHANCED_SHULKER_BOX.get(), EnhancedShulkerBoxRenderer::new);
        event.registerBlockEntityRenderer(CESGBlockEntities.SHULKER_LOADER.get(), ShulkerStationRenderer::new);
        event.registerBlockEntityRenderer(CESGBlockEntities.SHULKER_UNLOADER.get(), ShulkerStationRenderer::new);
        event.registerBlockEntityRenderer(CESGBlockEntities.SHULKER_BELT_LOADER.get(), ShulkerBeltLoaderRenderer::new);
        event.registerBlockEntityRenderer(CESGBlockEntities.SHULKER_BELT_UNLOADER.get(), ShulkerBeltUnloaderRenderer::new);
        event.registerBlockEntityRenderer(CESGBlockEntities.SHULKER_CAGE.get(), ShulkerCageRenderer::new);
        event.registerBlockEntityRenderer(CESGBlockEntities.CROSS_DIMENSIONAL_GATEWAY_CORE.get(), GatewayCoreRenderer::new);
        event.registerBlockEntityRenderer(CESGBlockEntities.END_GATEWAY.get(), GatewayCoreRenderer::new);
        event.registerBlockEntityRenderer(CESGBlockEntities.ENDER_INFUSER.get(),
                com.cesg.client.EnderInfuserRenderer::new);
        event.registerBlockEntityRenderer(CESGBlockEntities.GATEWAY_FLUX_BATTERY.get(),
                GatewayFluxBatteryRenderer::new);
    }

    private static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(ModelResourceLocation.standalone(CESGPartialModels.BELT_LOADER_TUBE.modelLocation()));
        event.register(ModelResourceLocation.standalone(CESGPartialModels.BELT_UNLOADER_TUBE.modelLocation()));
        event.register(ModelResourceLocation.standalone(CESGPartialModels.EXTRACTOR_NOZZLE.modelLocation()));
        event.register(ModelResourceLocation.standalone(CESGPartialModels.GATEWAY_CORE_COG.modelLocation()));
    }
}
