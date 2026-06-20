package com.cesg;

import net.minecraft.resources.ResourceLocation;

public final class CESGIds {
    private CESGIds() {}

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CESG.MOD_ID, path);
    }
}
