package com.cesg.gateways.teleport;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

import com.cesg.init.CESGRegistration;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;

/**
 * Detects a valid vertical gateway ring around a Core, like a flexible nether portal. The Core counts
 * as one of the ring blocks: we flood-fill the connected loop of Gateway Frame blocks (plus the Core)
 * in the Core's vertical plane, then accept it when that loop is exactly the perimeter of a rectangle
 * whose interior is air/portal. The Core may sit anywhere on the ring (edge or corner).
 */
public final class GatewayPortalShape {
    public static final int MIN_WIDTH = 1;
    public static final int MAX_WIDTH = 8;
    public static final int MIN_HEIGHT = 2;
    public static final int MAX_HEIGHT = 8;

    public final Direction.Axis axis;
    public final List<BlockPos> interior;
    /** Ring blocks that are Gateway Frames (the Core is excluded) — driven LIT when the gateway is active. */
    public final List<BlockPos> frame;

    private GatewayPortalShape(Direction.Axis axis, List<BlockPos> interior, List<BlockPos> frame) {
        this.axis = axis;
        this.interior = interior;
        this.frame = frame;
    }

    public static Optional<GatewayPortalShape> detect(BlockGetter level, BlockPos core) {
        for (Direction.Axis widthAxis : new Direction.Axis[] { Direction.Axis.X, Direction.Axis.Z }) {
            Optional<GatewayPortalShape> shape = tryAxis(level, core, widthAxis);
            if (shape.isPresent())
                return shape;
        }
        return Optional.empty();
    }

    private static Optional<GatewayPortalShape> tryAxis(BlockGetter level, BlockPos core, Direction.Axis widthAxis) {
        Direction.Axis otherAxis = widthAxis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        int planeFixed = coord(core, otherAxis);
        Direction widthPos = Direction.fromAxisAndDirection(widthAxis, Direction.AxisDirection.POSITIVE);
        Direction[] dirs = { Direction.UP, Direction.DOWN, widthPos, widthPos.getOpposite() };
        int maxRing = 2 * (MAX_WIDTH + 2) + 2 * (MAX_HEIGHT + 2);

        // Flood-fill the connected loop of frame/core blocks in this plane, starting from the Core.
        Set<BlockPos> ring = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(core);
        ring.add(core);
        int uMin = Integer.MAX_VALUE, uMax = Integer.MIN_VALUE, yMin = Integer.MAX_VALUE, yMax = Integer.MIN_VALUE;
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            uMin = Math.min(uMin, coord(p, widthAxis));
            uMax = Math.max(uMax, coord(p, widthAxis));
            yMin = Math.min(yMin, p.getY());
            yMax = Math.max(yMax, p.getY());
            for (Direction d : dirs) {
                BlockPos n = p.relative(d);
                if (coord(n, otherAxis) != planeFixed || ring.contains(n))
                    continue;
                if (isRingBlock(level, n, core)) {
                    if (ring.size() > maxRing)
                        return Optional.empty();
                    ring.add(n);
                    queue.add(n);
                }
            }
        }

        int outerWidth = uMax - uMin + 1;
        int outerHeight = yMax - yMin + 1;
        int interiorWidth = outerWidth - 2;
        int interiorHeight = outerHeight - 2;
        if (interiorWidth < MIN_WIDTH || interiorWidth > MAX_WIDTH
                || interiorHeight < MIN_HEIGHT || interiorHeight > MAX_HEIGHT)
            return Optional.empty();

        // The ring must be exactly the rectangle's perimeter — no gaps (missing corner/edge) and nothing extra.
        if (ring.size() != 2 * outerWidth + 2 * outerHeight - 4)
            return Optional.empty();
        for (int u = uMin; u <= uMax; u++)
            if (!ring.contains(make(widthAxis, u, yMin, planeFixed)) || !ring.contains(make(widthAxis, u, yMax, planeFixed)))
                return Optional.empty();
        for (int y = yMin; y <= yMax; y++)
            if (!ring.contains(make(widthAxis, uMin, y, planeFixed)) || !ring.contains(make(widthAxis, uMax, y, planeFixed)))
                return Optional.empty();

        // Interior must all be fillable (air or an existing portal cell).
        List<BlockPos> interior = new ArrayList<>();
        for (int u = uMin + 1; u <= uMax - 1; u++) {
            for (int y = yMin + 1; y <= yMax - 1; y++) {
                BlockPos cell = make(widthAxis, u, y, planeFixed);
                if (!isFillable(level, cell))
                    return Optional.empty();
                interior.add(cell);
            }
        }
        List<BlockPos> frame = new ArrayList<>();
        for (BlockPos ringCell : ring)
            if (!ringCell.equals(core))
                frame.add(ringCell);
        return Optional.of(new GatewayPortalShape(widthAxis, interior, frame));
    }

    private static int coord(BlockPos p, Direction.Axis axis) {
        return axis == Direction.Axis.X ? p.getX() : axis == Direction.Axis.Y ? p.getY() : p.getZ();
    }

    private static BlockPos make(Direction.Axis widthAxis, int u, int y, int fixed) {
        int x = widthAxis == Direction.Axis.X ? u : fixed;
        int z = widthAxis == Direction.Axis.Z ? u : fixed;
        return new BlockPos(x, y, z);
    }

    private static boolean isFillable(BlockGetter level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return state.isAir() || state.is(CESGRegistration.GATEWAY_PORTAL.get());
    }

    private static boolean isRingBlock(BlockGetter level, BlockPos pos, BlockPos core) {
        return pos.equals(core) || level.getBlockState(pos).is(CESGRegistration.GATEWAY_FRAME.get());
    }
}
