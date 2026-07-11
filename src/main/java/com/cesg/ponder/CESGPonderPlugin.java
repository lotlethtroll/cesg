package com.cesg.ponder;

import com.cesg.CESG;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

/** Registers CESG's Ponder scenes. Added to the index from client setup via {@code PonderIndex.addPlugin}. */
public class CESGPonderPlugin implements PonderPlugin {

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
    }
}
