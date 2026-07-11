package com.cesg.gateways.teleport;

import java.util.Optional;

import com.cesg.init.CESGRegistration;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

public final class TeleportResolver {
    /** Use current entity yaw when placing after teleport. */
    public static final float KEEP_YAW = Float.NaN;

    private TeleportResolver() {}

    /** Teleport Resistance blocks all gateway travel too (the gateway teleport never fires the vanilla event). */
    public static boolean hasTeleportResistance(net.minecraft.world.entity.Entity entity) {
        return entity instanceof net.minecraft.world.entity.LivingEntity living
                && living.hasEffect(com.cesg.init.CESGEffects.TELEPORT_RESISTANCE);
    }

    public static void teleportToCentralIsland(ServerPlayer player, ServerLevel endLevel) {
        if (hasTeleportResistance(player)) {
            player.displayClientMessage(Component.translatable("cesg.gateway.teleport_resistant"), true);
            return;
        }
        BlockPos target = findCentralIslandTarget(endLevel);
        placeSafely(player, endLevel, target, KEEP_YAW);
        player.displayClientMessage(Component.translatable("cesg.gateway.central_island"), true);
    }

    /**
     * Walk-through transport for any entity (players, mobs, dropped items) — mirrors nether-portal passage.
     * Players keep their richer messaging path; everything else is moved silently to the partner exit.
     */
    public static void teleportThroughPortal(Entity traveler, GatewaySideState thisSide, GatewaySideState partner) {
        if (traveler == null)
            return;
        if (hasTeleportResistance(traveler)) {
            if (traveler instanceof ServerPlayer player)
                player.displayClientMessage(Component.translatable("cesg.gateway.teleport_resistant"), true);
            return;
        }
        if (traveler instanceof ServerPlayer player) {
            teleportBound(player, thisSide, partner);
            return;
        }
        net.minecraft.server.MinecraftServer server = traveler.getServer();
        if (server == null)
            return;

        TeleportResult result = resolveTeleportDestination(traveler, thisSide, partner);
        if (result.isDenied())
            return;

        ServerLevel targetLevel = server.getLevel(result.dimension());
        if (targetLevel == null)
            return;

        float yaw = Float.isNaN(result.exitYaw()) ? traveler.getYRot() : result.exitYaw();
        Vec3 spawn = Vec3.atBottomCenterOf(result.position());
        if (!targetLevel.equals(traveler.level())) {
            traveler.changeDimension(new DimensionTransition(targetLevel, spawn, traveler.getDeltaMovement(),
                    yaw, traveler.getXRot(), DimensionTransition.DO_NOTHING));
        } else {
            traveler.moveTo(spawn.x, spawn.y, spawn.z, yaw, traveler.getXRot());
        }
        traveler.fallDistance = 0;
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

        float exitYaw = Float.isNaN(result.exitYaw()) ? traveler.getYRot() : result.exitYaw();
        boolean crossDimensional = !targetLevel.equals(traveler.level());
        if (crossDimensional) {
            Vec3 spawn = Vec3.atBottomCenterOf(result.position());
            traveler.changeDimension(new DimensionTransition(targetLevel, spawn, traveler.getDeltaMovement(),
                    exitYaw, traveler.getXRot(), DimensionTransition.DO_NOTHING));
        }
        placeSafely(traveler, targetLevel, result.position(), result.exitYaw());
        if (crossDimensional)
            awardTravelAdvancement(traveler);
    }

    /** Gateway travel bypasses vanilla triggers, so the travel advancement is awarded explicitly. */
    private static void awardTravelAdvancement(ServerPlayer traveler) {
        var advancement = traveler.server.getAdvancements().get(com.cesg.CESGIds.GATEWAY_TRAVEL_ADVANCEMENT);
        if (advancement != null)
            traveler.getAdvancements().award(advancement, com.cesg.CESGIds.GATEWAY_TRAVEL_CRITERION);
    }

    public static TeleportResult resolveTeleportDestination(Entity traveler, GatewaySideState thisSide,
            GatewaySideState partner) {
        if (!thisSide.powered() || !thisSide.fueled())
            return TeleportResult.deniedResult();

        // Entity.getServer() is null on the client; resolution is a server-side decision.
        net.minecraft.server.MinecraftServer server = traveler.getServer();
        if (server == null)
            return TeleportResult.deniedResult();

        if (partner != null && partner.powered() && partner.fueled()) {
            ServerLevel partnerLevel = server.getLevel(partner.dimension());
            ServerLevel sourceLevel = server.getLevel(thisSide.dimension());
            ExitPlan exit = computePortalExit(traveler, sourceLevel, thisSide.position(), partnerLevel,
                    partner.position());
            return TeleportResult.bound(partner.dimension(), exit.feet(), exit.yaw());
        }

        if (thisSide.inEnd())
            return TeleportResult.fallback(Level.END, findCentralIslandTarget(server.getLevel(Level.END)));

        return TeleportResult.deniedResult();
    }

    /**
     * Maps the traveler's position within the source portal to the partner portal, then steps one block
     * out along the travel-through axis (not the core's arbitrary FACING, which may be sideways to the plane).
     */
    private static ExitPlan computePortalExit(Entity traveler, ServerLevel sourceLevel, BlockPos sourceCore,
            ServerLevel destLevel, BlockPos destCore) {
        if (destLevel == null)
            return new ExitPlan(BlockPos.ZERO, KEEP_YAW);

        Optional<GatewayPortalShape> destShape = GatewayPortalShape.detect(destLevel, destCore);
        if (destShape.isEmpty()) {
            Direction facing = coreFacing(destLevel, destCore);
            BlockPos feet = resolveExitFeet(destLevel, destCore.relative(facing), facing, null);
            return new ExitPlan(feet, facing.toYRot());
        }

        GatewayPortalShape destPortal = destShape.get();
        PortalBounds destBounds = PortalBounds.of(destPortal);

        BlockPos inPortal;
        Direction exitDir;
        Optional<GatewayPortalShape> sourceShape = sourceLevel != null
                ? GatewayPortalShape.detect(sourceLevel, sourceCore)
                : Optional.empty();
        if (sourceShape.isPresent()) {
            PortalBounds sourceBounds = PortalBounds.of(sourceShape.get());
            inPortal = destBounds.mapFrom(sourceBounds, traveler.blockPosition());
            exitDir = exitThroughDirection(sourceShape.get(), sourceBounds, traveler.blockPosition());
        } else {
            inPortal = destBounds.center();
            exitDir = fallbackExitDirection(destPortal, destBounds, destCore, destLevel);
        }

        BlockPos preferred = inPortal.relative(exitDir);
        BlockPos feet = resolveExitFeet(destLevel, preferred, exitDir, destPortal);
        return new ExitPlan(feet, exitDir.toYRot());
    }

    /** Direction the traveler was moving through the source portal (out the far side of the partner portal). */
    private static Direction exitThroughDirection(GatewayPortalShape source, PortalBounds sourceBounds,
            BlockPos travelerFeet) {
        Direction side = sideRelativeToPlane(source, sourceBounds, travelerFeet);
        return side.getOpposite();
    }

    /** When the source ring is missing, step out toward the core's eye side if it lies on the portal normal. */
    private static Direction fallbackExitDirection(GatewayPortalShape destPortal, PortalBounds destBounds,
            BlockPos destCore, ServerLevel destLevel) {
        Direction facing = coreFacing(destLevel, destCore);
        Direction.Axis normalAxis = portalNormalAxis(destPortal);
        if (facing.getAxis() == normalAxis)
            return facing;
        BlockPos center = destBounds.center();
        return dominantHorizontalDirection(destCore.getX() - center.getX(), destCore.getZ() - center.getZ(), normalAxis);
    }

    private static Direction coreFacing(ServerLevel level, BlockPos corePos) {
        if (level == null)
            return Direction.NORTH;
        BlockState state = level.getBlockState(corePos);
        if (state.hasProperty(DirectionalKineticBlock.FACING))
            return state.getValue(DirectionalKineticBlock.FACING);
        return Direction.NORTH;
    }

    private static Direction.Axis portalNormalAxis(GatewayPortalShape shape) {
        return shape.axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
    }

    private static Direction sideRelativeToPlane(GatewayPortalShape shape, PortalBounds bounds, BlockPos feet) {
        Direction.Axis normalAxis = portalNormalAxis(shape);
        int plane = bounds.planeCoord;
        int coord = axisCoord(feet, normalAxis);
        if (coord > plane)
            return Direction.get(Direction.AxisDirection.POSITIVE, normalAxis);
        if (coord < plane)
            return Direction.get(Direction.AxisDirection.NEGATIVE, normalAxis);
        BlockPos center = bounds.center();
        return dominantHorizontalDirection(feet.getX() - center.getX(), feet.getZ() - center.getZ(), normalAxis);
    }

    private static Direction dominantHorizontalDirection(int dx, int dz, Direction.Axis normalAxis) {
        if (normalAxis == Direction.Axis.X)
            return dz >= 0 ? Direction.SOUTH : Direction.NORTH;
        return dx >= 0 ? Direction.EAST : Direction.WEST;
    }

    private static int axisCoord(BlockPos pos, Direction.Axis axis) {
        return switch (axis) {
            case X -> pos.getX();
            case Y -> pos.getY();
            case Z -> pos.getZ();
        };
    }

    private static BlockPos findCentralIslandTarget(ServerLevel endLevel) {
        if (endLevel == null)
            return BlockPos.ZERO;

        // The End's main island is centered on world origin. Probe real column surfaces via the
        // heightmap (which loads/generates the chunk) instead of trusting getSharedSpawnPos(), which
        // is unreliable in the End and previously dropped travelers above the void. Start a few blocks
        // out so we clear the 5x5 exit-portal frame at (0,0) and never land in an end_portal block.
        for (int radius = 4; radius <= 24; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius)
                        continue; // walk the ring only, expanding outward
                    BlockPos surface =
                            endLevel.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(dx, 0, dz));
                    if (surface.getY() > endLevel.getMinBuildHeight() + 1 && isSafe(endLevel, surface))
                        return surface;
                }
            }
        }

        // No island surface found near origin (shouldn't happen on a normal End) — fall back.
        return findSafePosition(endLevel, endLevel.getSharedSpawnPos().above());
    }

    /** Legacy spiral search — used only for End fallback spawns. */
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

    /**
     * Prefer the exact portal exit tile. Only nudge by one block when blocked — never scan far forward
     * (that was dropping players several blocks ahead on End void edges).
     */
    private static BlockPos resolveExitFeet(ServerLevel level, BlockPos preferred, Direction exitDir,
            GatewayPortalShape destPortal) {
        if (level == null)
            return BlockPos.ZERO;

        if (isSafe(level, preferred))
            return preferred;
        if (isSafe(level, preferred.above()))
            return preferred.above();

        // Step back into the portal plane rather than forward into void.
        BlockPos inside = preferred.relative(exitDir.getOpposite());
        if (destPortal != null && destPortal.interior.contains(inside) && isStandable(level, inside))
            return inside;

        Direction right = exitDir.getClockWise(Direction.Axis.Y);
        for (int side : new int[] { -1, 1 }) {
            BlockPos test = preferred.relative(right, side);
            if (isSafe(level, test))
                return test;
        }
        for (int dy : new int[] { -1, 1 }) {
            BlockPos test = preferred.offset(0, dy, 0);
            if (isSafe(level, test))
                return test;
        }

        return preferred;
    }

    private static boolean isStandable(ServerLevel level, BlockPos pos) {
        return isPassableForFeet(level, pos) && isPassableForFeet(level, pos.above());
    }

    private static boolean isSafe(ServerLevel level, BlockPos pos) {
        if (!isPassableForFeet(level, pos) || !isPassableForFeet(level, pos.above()))
            return false;
        BlockState below = level.getBlockState(pos.below());
        return !below.isAir() && below.blocksMotion();
    }

    private static boolean isPassableForFeet(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.is(CESGRegistration.GATEWAY_PORTAL.get());
    }

    private static void placeSafely(ServerPlayer player, ServerLevel level, BlockPos pos, float exitYaw) {
        float yaw = Float.isNaN(exitYaw) ? player.getYRot() : exitYaw;
        player.teleportTo(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, player.getXRot());
        player.fallDistance = 0;
    }

    private record ExitPlan(BlockPos feet, float yaw) {}

    private record PortalBounds(Direction.Axis widthAxis, int planeCoord, int minWidth, int maxWidth, int minY,
            int maxY) {
        static PortalBounds of(GatewayPortalShape shape) {
            int minW = Integer.MAX_VALUE, maxW = Integer.MIN_VALUE;
            int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
            int plane = 0;
            for (BlockPos p : shape.interior) {
                int w = shape.axis == Direction.Axis.X ? p.getX() : p.getZ();
                minW = Math.min(minW, w);
                maxW = Math.max(maxW, w);
                minY = Math.min(minY, p.getY());
                maxY = Math.max(maxY, p.getY());
                plane = shape.axis == Direction.Axis.X ? p.getZ() : p.getX();
            }
            return new PortalBounds(shape.axis, plane, minW, maxW, minY, maxY);
        }

        BlockPos center() {
            return blockAt(0.5, 0.5);
        }

        BlockPos mapFrom(PortalBounds source, BlockPos sourceFeet) {
            return blockAt(source.relativeWidth(sourceFeet), source.relativeHeight(sourceFeet));
        }

        private BlockPos blockAt(double relW, double relH) {
            int w = minWidth + (int) Math.round((maxWidth - minWidth) * relW);
            int y = minY + (int) Math.round((maxY - minY) * relH);
            return widthAxis == Direction.Axis.X ? new BlockPos(w, y, planeCoord) : new BlockPos(planeCoord, y, w);
        }

        private double relativeWidth(BlockPos pos) {
            int w = widthAxis == Direction.Axis.X ? pos.getX() : pos.getZ();
            if (maxWidth == minWidth)
                return 0.5;
            return (w - minWidth + 0.5) / (maxWidth - minWidth + 1.0);
        }

        private double relativeHeight(BlockPos pos) {
            if (maxY == minY)
                return 0.5;
            return (pos.getY() - minY + 0.5) / (maxY - minY + 1.0);
        }
    }

    public record TeleportResult(ResourceKey<Level> dimension, BlockPos position, float exitYaw, boolean isDenied,
            boolean usedFallback) {
        public static TeleportResult deniedResult() {
            return new TeleportResult(Level.OVERWORLD, BlockPos.ZERO, KEEP_YAW, true, false);
        }

        public static TeleportResult bound(ResourceKey<Level> dimension, BlockPos position, float exitYaw) {
            return new TeleportResult(dimension, position, exitYaw, false, false);
        }

        public static TeleportResult fallback(ResourceKey<Level> dimension, BlockPos position) {
            return new TeleportResult(dimension, position, KEEP_YAW, false, true);
        }
    }
}
