package com.cesg.gateways.teleport;

import com.cesg.gateways.CrossDimensionalGatewayCoreBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public record GatewayPartner(ResourceKey<Level> dimension, BlockPos position, boolean bound, String name) {
    public static final GatewayPartner EMPTY = new GatewayPartner(Level.OVERWORLD, BlockPos.ZERO, false);

    public GatewayPartner(ResourceKey<Level> dimension, BlockPos position, boolean bound) {
        this(dimension, position, bound, "");
    }

    public boolean isBound() {
        return bound && position != BlockPos.ZERO;
    }

    public boolean hasName() {
        return !name.isBlank();
    }

    /**
     * The partner's name as it is <em>now</em>, not as it was when the binding was made. {@link #name} is
     * a copy taken at bind time, so renaming a gateway left every binding pointing at it showing the old
     * label forever. Reads through to the live Core when its chunk is loaded, and falls back to the
     * stored copy when it is not (an unloaded or cross-dimension partner still needs something to show).
     */
    public String displayName(Level level) {
        if (level != null && level.dimension().equals(dimension) && level.isLoaded(position)
                && level.getBlockEntity(position) instanceof CrossDimensionalGatewayCoreBlockEntity core) {
            String live = core.getGatewayName();
            if (!live.isBlank())
                return live;
        }
        return name;
    }

    public GatewaySideState resolve(MinecraftServer server) {
        ServerLevel partnerLevel = server.getLevel(dimension);
        if (partnerLevel == null)
            return offlineSideState();

        BlockEntity be = partnerLevel.getBlockEntity(position);
        if (be instanceof CrossDimensionalGatewayCoreBlockEntity core)
            return core.createSideState();

        return offlineSideState();
    }

    private GatewaySideState offlineSideState() {
        return new GatewaySideState(dimension, position, false, false, dimension.equals(Level.END));
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Dimension", dimension.location().toString());
        tag.putLong("Pos", position.asLong());
        tag.putBoolean("Bound", bound);
        tag.putString("Name", name);
        return tag;
    }

    public static GatewayPartner load(CompoundTag tag) {
        if (tag.isEmpty())
            return EMPTY;
        ResourceKey<Level> dim = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                net.minecraft.resources.ResourceLocation.parse(tag.getString("Dimension")));
        BlockPos pos = BlockPos.of(tag.getLong("Pos"));
        return new GatewayPartner(dim, pos, tag.getBoolean("Bound"), tag.getString("Name"));
    }
}
