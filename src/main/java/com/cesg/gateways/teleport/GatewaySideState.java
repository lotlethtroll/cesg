package com.cesg.gateways.teleport;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record GatewaySideState(
        ResourceKey<Level> dimension,
        BlockPos position,
        boolean powered,
        boolean fueled,
        boolean inEnd
) {}
