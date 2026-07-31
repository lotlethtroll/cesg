package com.cesg.ponder;

import com.cesg.CESG;
import com.cesg.init.CESGRegistration;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

/** Registers CESG's Ponder scenes. Added to the index from client setup via {@code PonderIndex.addPlugin}. */
public class CESGPonderPlugin implements PonderPlugin {

    /** Index category holding every CESG scene, so they are reachable without knowing which block to W-key. */
    public static final ResourceLocation TAG_END_STORAGE = CESG.id("end_storage");

    @Override
    public String getModId() {
        return CESG.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.addStoryBoard(CESG.id("shulker_loader"), CESG.id("shulker_loader"),
                CESGPonderScenes::shulkerLoader);
        helper.addStoryBoard(CESG.id("shulker_unloader"), CESG.id("shulker_unloader"),
                CESGPonderScenes::shulkerUnloader);
        helper.addStoryBoard(CESG.id("shulker_belt_loader"), CESG.id("shulker_belt_loader"),
                CESGPonderScenes::shulkerBeltLoader);
        helper.addStoryBoard(CESG.id("shulker_belt_unloader"), CESG.id("shulker_belt_unloader"),
                CESGPonderScenes::shulkerBeltUnloader);
        helper.addStoryBoard(CESG.id("cross_dimensional_gateway_core"), CESG.id("cross_dimensional_gateway_core"),
                CESGPonderScenes::gatewayCore);
        helper.addStoryBoard(CESG.id("ender_infuser"), CESG.id("ender_infuser"),
                CESGPonderScenes::enderInfuser);
        helper.addStoryBoard(CESG.id("gateway_flux_battery"), CESG.id("cross_dimensional_gateway_core"),
                CESGPonderScenes::gatewayFluxBattery);
        // Second battery scene: the multiblock arrays, on their own wider schematic.
        helper.addStoryBoard(CESG.id("gateway_flux_battery"), CESG.id("gateway_flux_battery_array"),
                CESGPonderScenes::gatewayFluxBatteryArray);
        // The three network blocks share a staging but each has its own scene id, so each resolves its own
        // title and text keys rather than inheriting the Bridge's.
        helper.addStoryBoard(CESG.id("storage_bridge"), CESG.id("cross_dimensional_gateway_core"),
                CESGPonderScenes::storageBridge);
        helper.addStoryBoard(CESG.id("storage_network_controller"), CESG.id("cross_dimensional_gateway_core"),
                CESGPonderScenes::storageNetworkController);
        helper.addStoryBoard(CESG.id("storage_terminal"), CESG.id("cross_dimensional_gateway_core"),
                CESGPonderScenes::storageTerminal);
    }

    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        helper.registerTag(TAG_END_STORAGE)
                .addToIndex()
                .item(CESGRegistration.SHULKER_LOADER.get())
                .title("End Storage & Gateways")
                .description("Shulker automation, ender processing, and the cross-dimensional Gateway network")
                .register();

        helper.addToTag(TAG_END_STORAGE)
                .add(CESG.id("shulker_loader"))
                .add(CESG.id("shulker_unloader"))
                .add(CESG.id("shulker_belt_loader"))
                .add(CESG.id("shulker_belt_unloader"))
                .add(CESG.id("ender_infuser"))
                .add(CESG.id("cross_dimensional_gateway_core"))
                .add(CESG.id("gateway_flux_battery"))
                .add(CESG.id("storage_bridge"))
                .add(CESG.id("storage_network_controller"))
                .add(CESG.id("storage_terminal"));
    }
}
