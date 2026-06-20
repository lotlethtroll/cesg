package com.cesg.gateways.teleport;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

public final class TeleportResolver {
    private TeleportResolver() {}

    public static void teleportToCentralIsland(ServerPlayer player, ServerLevel endLevel) {
        BlockPos target = findCentralIslandTarget(endLevel);
        placeSafely(player, endLevel, target);
        player.displayClientMessage(Component.translatable("cesg.gateway.central_island"), true);
    }

    public static void teleportBound(ServerPlayer traveler, GatewaySideState thisSide, GatewaySideState partner) {
        ServerLevel destination = traveler.server.getLevel(partner.dimension());
        if (destination == null) {
            traveler.displayClientMessage(Component.translatable("cesg.gateway.invalid_partner"), true);
            return;
        }

        if (!thisSide.powered() || !thisSide.fueled()) {
            traveler.displayClientMessage(Component.translatable("cesg.gateway.unpowered"), true);
            return;
        }

        TeleportResult result = resolveTeleportDestination(traveler, thisSide, partner);
        if (result.isDenied()) {
            traveler.displayClientMessage(Component.translatable("cesg.gateway.denied"), true);
            return;
        }

        if (result.usedFallback())
            traveler.displayClientMessage(Component.translatable("cesg.gateway.fallback"), true);

        ServerLevel targetLevel = traveler.server.getLevel(result.dimension());
        if (targetLevel == null)
            return;

        if (!targetLevel.equals(traveler.level())) {
            Vec3 spawn = Vec3.atBottomCenterOf(result.position());
            traveler.changeDimension(new DimensionTransition(targetLevel, spawn, traveler.getDeltaMovement(),
                    traveler.getYRot(), traveler.getXRot(), DimensionTransition.DO_NOTHING));
        }
        placeSafely(traveler, targetLevel, result.position());
    }

    public static TeleportResult resolveTeleportDestination(Entity traveler, GatewaySideState thisSide,
            GatewaySideState partner) {
        if (!thisSide.powered() || !thisSide.fueled())
            return TeleportResult.deniedResult();

        if (partner != null && partner.powered() && partner.fueled()) {
            ServerLevel partnerLevel = traveler.getServer().getLevel(partner.dimension());
            return TeleportResult.bound(partner.dimension(), findSafePosition(partnerLevel, partner.position()));
        }

        if (thisSide.inEnd())
            return TeleportResult.fallback(Level.END, findCentralIslandTarget(traveler.getServer().getLevel(Level.END)));

        return TeleportResult.deniedResult();
    }

    private static BlockPos findCentralIslandTarget(ServerLevel endLevel) {
        if (endLevel == null)
            return BlockPos.ZERO;
        BlockPos portal = endLevel.getSharedSpawnPos();
        return findSafePosition(endLevel, portal.above());
    }

    private static BlockPos findSafePosition(ServerLevel level, BlockPos origin) {
        if (level == null)
            return BlockPos.ZERO;

        for (int radius = 0; radius < 8; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos test = origin.offset(x, 0, z);
                    if (isSafe(level, test))
                        return test;
                    if (isSafe(level, test.above(2)))
                        return test.above(2);
                }
            }
        }
        return origin;
    }

    private static boolean isSafe(ServerLevel level, BlockPos pos) {
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockState below = level.getBlockState(pos.below());
        return !below.isAir() && feet.isAir() && head.isAir() && below.blocksMotion();
    }

    private static void placeSafely(ServerPlayer player, ServerLevel level, BlockPos pos) {
        player.teleportTo(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYRot(), player.getXRot());
        player.fallDistance = 0;
    }

    public record TeleportResult(ResourceKey<Level> dimension, BlockPos position, boolean isDenied, boolean usedFallback) {
        public static TeleportResult deniedResult() {
            return new TeleportResult(Level.OVERWORLD, BlockPos.ZERO, true, false);
        }

        public static TeleportResult bound(ResourceKey<Level> dimension, BlockPos position) {
            return new TeleportResult(dimension, position, false, false);
        }

        public static TeleportResult fallback(ResourceKey<Level> dimension, BlockPos position) {
            return new TeleportResult(dimension, position, false, true);
        }
    }
}
