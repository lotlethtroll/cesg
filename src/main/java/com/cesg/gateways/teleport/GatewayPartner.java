package com.cesg.gateways.teleport;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public record GatewayPartner(ResourceKey<Level> dimension, BlockPos position, boolean bound) {
    public static final GatewayPartner EMPTY = new GatewayPartner(Level.OVERWORLD, BlockPos.ZERO, false);

    public boolean isBound() {
        return bound && position != BlockPos.ZERO;
    }

    public GatewaySideState resolve(ServerLevel level) {
        return new GatewaySideState(dimension, position, true, true, dimension.equals(Level.END));
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Dimension", dimension.location().toString());
        tag.putLong("Pos", position.asLong());
        tag.putBoolean("Bound", bound);
        return tag;
    }

    public static GatewayPartner load(CompoundTag tag) {
        if (tag.isEmpty())
            return EMPTY;
        ResourceKey<Level> dim = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                net.minecraft.resources.ResourceLocation.parse(tag.getString("Dimension")));
        BlockPos pos = BlockPos.of(tag.getLong("Pos"));
        return new GatewayPartner(dim, pos, tag.getBoolean("Bound"));
    }
}
