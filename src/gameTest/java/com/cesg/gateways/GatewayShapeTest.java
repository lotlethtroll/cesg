package com.cesg.gateways;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.cesg.gateways.teleport.GatewayPartner;
import com.cesg.gateways.teleport.GatewayPortalShape;
import com.cesg.init.CESGRegistration;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Gateway ring detection. Layouts are built in an in-memory {@link BlockGetter} so hundreds of them can
 * be checked without touching the world; one test drives a real Core to show an open gateway survives
 * a touching ring being built next to it. Coordinates are (u, y) in a vertical plane: u runs along the
 * ring's width axis and the plane sits at a fixed coordinate on the other horizontal axis.
 */
@GameTestHolder("cesg")
@PrefixGameTestTemplate(false)
public class GatewayShapeTest {
    private static final String EXTRA = "cesg.goggles.gateway.frame.extra";
    private static final String GAP = "cesg.goggles.gateway.frame.gap";
    private static final String BLOCKED = "cesg.goggles.gateway.frame.blocked";
    private static final String SIZE = "cesg.goggles.gateway.frame.size";

    // ---- Layouts valid before 2.0 must detect exactly as before ----

    @GameTest(template = "empty")
    public void everyValidRingSizeKeepsItsShape(GameTestHelper helper) {
        int checked = 0;
        for (Direction.Axis axis : new Direction.Axis[] { Direction.Axis.X, Direction.Axis.Z }) {
            for (int w = GatewayPortalShape.MIN_WIDTH; w <= GatewayPortalShape.MAX_WIDTH; w++) {
                for (int h = GatewayPortalShape.MIN_HEIGHT; h <= GatewayPortalShape.MAX_HEIGHT; h++) {
                    int[][] cores = { { 1 + (w - 1) / 2, 0 }, { 0, 1 + (h - 1) / 2 }, { 0, 0 }, { w + 1, 0 },
                            { 0, h + 1 }, { w + 1, h + 1 } };
                    for (int[] c : cores) {
                        Layout layout = new Layout(axis, 0);
                        layout.ring(0, 0, w, h);
                        BlockPos core = layout.core(c[0], c[1]);
                        assertSameAsLegacy(layout, core, "size " + w + "x" + h + " axis " + axis);
                        assertRectangle(layout, core, 0, 0, w, h);
                        checked++;
                    }
                }
            }
        }
        helper.assertTrue(checked == 2 * 8 * 7 * 6, "Expected every size and Core position to be checked");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void touchingLayoutsValidTodayKeepTheirShape(GameTestHelper helper) {
        // Core-to-Core contact (A's top-right corner beside B's bottom-left corner): a foreign Core
        // already stopped the old flood-fill, and no frames touch.
        Layout coreContact = new Layout(Direction.Axis.X, 0);
        coreContact.ring(0, 0, 2, 3);
        coreContact.ring(4, 4, 2, 3);
        BlockPos a = coreContact.core(3, 4);
        BlockPos b = coreContact.core(4, 4);
        assertSameAsLegacy(coreContact, a, "core contact A");
        assertSameAsLegacy(coreContact, b, "core contact B");
        assertRectangle(coreContact, a, 0, 0, 2, 3);
        assertRectangle(coreContact, b, 4, 4, 2, 3);

        // Diagonal contact: corner frames only touch diagonally, which the flood-fill never follows.
        Layout diagonal = new Layout(Direction.Axis.X, 0);
        diagonal.ring(0, 0, 2, 3);
        diagonal.ring(4, 5, 2, 3);
        BlockPos lower = diagonal.core(1, 0);
        BlockPos upper = diagonal.core(5, 5);
        assertSameAsLegacy(diagonal, lower, "diagonal lower");
        assertSameAsLegacy(diagonal, upper, "diagonal upper");

        // Parallel rings back to back in neighbouring planes, Cores on an edge and on a corner.
        for (int[] c : new int[][] { { 1, 0 }, { 0, 0 } }) {
            Layout front = new Layout(Direction.Axis.X, 0);
            front.ring(0, 0, 2, 3);
            BlockPos frontCore = front.core(c[0], c[1]);
            Layout back = front.plane(1);
            back.ring(0, 0, 2, 3);
            BlockPos backCore = back.core(c[0], c[1]);
            assertSameAsLegacy(front, frontCore, "back-to-back front");
            assertSameAsLegacy(front, backCore, "back-to-back back");
            assertRectangle(front, frontCore, 0, 0, 2, 3);
            assertRectangle(back, backCore, 0, 0, 2, 3);
        }

        // One corner Core on both a valid X ring and a valid Z ring: X wins, as before.
        Layout both = new Layout(Direction.Axis.X, 0);
        both.ring(0, 0, 2, 3);
        Layout bothZ = both.rotated(0);
        bothZ.ring(0, 0, 3, 3);
        BlockPos shared = both.core(0, 0);
        assertSameAsLegacy(both, shared, "X and Z rings");
        assertRectangle(both, shared, 0, 0, 2, 3);

        // A corner Core whose X ring touches another ring but whose Z ring is clean: Z wins, as before,
        // even though the new fallback would accept the X ring.
        Layout messyX = new Layout(Direction.Axis.X, 0);
        messyX.ring(0, 0, 1, 2);
        messyX.ring(2, 0, 1, 2);
        Layout cleanZ = messyX.rotated(0);
        cleanZ.ring(0, 0, 2, 2);
        BlockPos corner = messyX.core(0, 0);
        assertSameAsLegacy(messyX, corner, "messy X, clean Z");
        assertRectangle(cleanZ, corner, 0, 0, 2, 2);

        // The largest ring, at the search limit, on both axes.
        for (Direction.Axis axis : new Direction.Axis[] { Direction.Axis.X, Direction.Axis.Z }) {
            Layout limit = new Layout(axis, 3);
            limit.ring(-4, 7, 8, 8);
            BlockPos core = limit.core(5, 7);
            assertSameAsLegacy(limit, core, "8x8 limit " + axis);
            assertRectangle(limit, core, -4, 7, 8, 8);
        }
        helper.succeed();
    }

    // ---- Touching layouts that failed before 2.0 now open ----

    @GameTest(template = "empty")
    public void touchingRingsNowOpen(GameTestHelper helper) {
        for (Direction.Axis axis : new Direction.Axis[] { Direction.Axis.X, Direction.Axis.Z }) {
            // Two rings sharing a column, Cores on the outer bottom corners.
            Layout shared = new Layout(axis, 0);
            shared.ring(0, 0, 1, 2);
            shared.ring(2, 0, 1, 2);
            BlockPos left = shared.core(0, 0);
            BlockPos right = shared.core(4, 0);
            assertNewlyOpen(shared, left, 0, 0, 1, 2);
            assertNewlyOpen(shared, right, 2, 0, 1, 2);

            // Side by side with adjacent frame columns.
            Layout adjacent = new Layout(axis, 0);
            adjacent.ring(0, 0, 1, 2);
            adjacent.ring(3, 0, 1, 2);
            assertNewlyOpen(adjacent, adjacent.core(1, 0), 0, 0, 1, 2);
            assertNewlyOpen(adjacent, adjacent.core(4, 0), 3, 0, 1, 2);

            // Stacked rings sharing a row.
            Layout stacked = new Layout(axis, 0);
            stacked.ring(0, 0, 2, 2);
            stacked.ring(0, 3, 2, 2);
            assertNewlyOpen(stacked, stacked.core(1, 0), 0, 0, 2, 2);
            assertNewlyOpen(stacked, stacked.core(2, 6), 0, 3, 2, 2);

            // A stray frame stuck to the outside of a ring.
            Layout stray = new Layout(axis, 0);
            stray.ring(0, 0, 2, 3);
            stray.frame(-1, 2);
            assertNewlyOpen(stray, stray.core(1, 0), 0, 0, 2, 3);

            // Three rings in a row: the middle Core sits on the bottom of its own opening.
            Layout row = new Layout(axis, 0);
            row.ring(0, 0, 1, 2);
            row.ring(2, 0, 1, 2);
            row.ring(4, 0, 1, 2);
            assertNewlyOpen(row, row.core(3, 0), 2, 0, 1, 2);
        }

        // A perpendicular (Z) ring whose edge column touches an X ring inside the X ring's plane.
        Layout xRing = new Layout(Direction.Axis.X, 0);
        xRing.ring(0, 0, 2, 3);
        BlockPos xCore = xRing.core(1, 0);
        Layout zRing = xRing.rotated(4);
        zRing.ring(0, 0, 2, 3);
        BlockPos zCore = zRing.core(1, 0);
        assertNewlyOpen(xRing, xCore, 0, 0, 2, 3);
        assertSameAsLegacy(zRing, zCore, "perpendicular Z ring");
        assertRectangle(zRing, zCore, 0, 0, 2, 3);
        helper.succeed();
    }

    // ---- Broken rings still fail, with the right diagnostic ----

    @GameTest(template = "empty")
    public void brokenRingsStillFail(GameTestHelper helper) {
        // Isolated rings: the original diagnostics.
        Layout gap = isolated();
        gap.air(1, 4);
        assertFails(gap, GAP, "gap");

        Layout port = isolated();
        port.set(0, 2, CESGRegistration.GATEWAY_PORT.get());
        assertFails(port, GAP, "port in outline");

        Layout blocked = isolated();
        blocked.set(1, 2, Blocks.STONE);
        assertFails(blocked, BLOCKED, "blocked");

        Layout foreign = isolated();
        foreign.set(2, 4, CESGRegistration.CROSS_DIMENSIONAL_GATEWAY_CORE.get());
        assertFails(foreign, GAP, "foreign core");

        Layout tooWide = new Layout(Direction.Axis.X, 0);
        tooWide.ring(0, 0, 9, 3);
        tooWide.core(1, 0);
        assertFails(tooWide, SIZE, "too wide");

        Layout tooShort = new Layout(Direction.Axis.X, 0);
        tooShort.ring(0, 0, 2, 1);
        tooShort.core(1, 0);
        assertFails(tooShort, SIZE, "too short");

        // The same faults in a ring that shares a column with a good one: the faulty side stays shut,
        // the good side opens.
        Layout touchingGap = touching();
        touchingGap.air(0, 3);
        assertFails(touchingGap, EXTRA, "touching gap");
        assertNewlyOpen(touchingGap, touchingGap.pos(4, 0), 2, 0, 1, 2);

        Layout touchingPort = touching();
        touchingPort.set(0, 1, CESGRegistration.GATEWAY_PORT.get());
        assertFails(touchingPort, EXTRA, "touching port");
        assertNewlyOpen(touchingPort, touchingPort.pos(4, 0), 2, 0, 1, 2);

        Layout touchingBlocked = touching();
        touchingBlocked.set(1, 1, Blocks.STONE);
        assertFails(touchingBlocked, EXTRA, "touching blocked");

        Layout touchingForeign = touching();
        touchingForeign.set(1, 3, CESGRegistration.CROSS_DIMENSIONAL_GATEWAY_CORE.get());
        assertFails(touchingForeign, EXTRA, "touching foreign core");
        assertNewlyOpen(touchingForeign, touchingForeign.pos(4, 0), 2, 0, 1, 2);

        // An opening one wider than the limit, sharing a column with a good ring.
        Layout touchingWide = new Layout(Direction.Axis.X, 0);
        touchingWide.ring(0, 0, 9, 2);
        touchingWide.ring(10, 0, 1, 2);
        touchingWide.core(0, 0);
        BlockPos good = touchingWide.core(12, 0);
        assertFails(touchingWide, SIZE, "touching too wide");
        assertNewlyOpen(touchingWide, good, 10, 0, 1, 2);

        // A Core that only touches an opening from outside its outline does not claim it.
        Layout outside = new Layout(Direction.Axis.X, 0);
        outside.ring(0, 0, 2, 3);
        outside.frame(-1, 0);
        BlockPos beside = outside.core(-1, 1);
        helper.assertTrue(GatewayPortalShape.detect(outside, beside).isEmpty(), "A Core off the outline opened");
        helper.succeed();
    }

    /** A 1x2 ring at u 0..2 with its Core on the bottom-left corner. */
    private static Layout isolated() {
        Layout layout = new Layout(Direction.Axis.X, 0);
        layout.ring(0, 0, 1, 3);
        layout.core(0, 0);
        return layout;
    }

    /** Two 1x2 rings sharing column u=2: Core A at (0,0) is the one under test, Core B at (4,0). */
    private static Layout touching() {
        Layout layout = new Layout(Direction.Axis.X, 0);
        layout.ring(0, 0, 1, 2);
        layout.ring(2, 0, 1, 2);
        layout.core(0, 0);
        layout.core(4, 0);
        return layout;
    }

    // ---- A live gateway is not closed by a touching ring ----

    @GameTest(template = "empty")
    public void activeGatewayStaysOpenWhenATouchingRingIsBuilt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos remote = helper.absolutePos(new BlockPos(12, 1, 12));
        for (BlockPos p : outline(origin, 0, 0, 2, 3))
            place(level, p, CESGRegistration.GATEWAY_FRAME.get());
        BlockPos corePos = origin.east();
        place(level, corePos, CESGRegistration.CROSS_DIMENSIONAL_GATEWAY_CORE.get());
        place(level, remote, CESGRegistration.CROSS_DIMENSIONAL_GATEWAY_CORE.get());
        var core = (CrossDimensionalGatewayCoreBlockEntity) level.getBlockEntity(corePos);
        core.setSpeed(16);
        core.fillEssence(4000, false);
        core.setPartner(new GatewayPartner(level.dimension(), remote, true));
        refresh(core);
        List<BlockPos> interior = interior(origin, 0, 0, 2, 3);
        for (BlockPos cell : interior)
            helper.assertTrue(level.getBlockState(cell).is(CESGRegistration.GATEWAY_PORTAL.get()),
                    "Gateway did not open before the touching ring was built");

        // Build a second ring sharing the open gateway's right-hand column, with its own Core.
        for (BlockPos p : outline(origin, 3, 0, 2, 3))
            if (!level.getBlockState(p).is(CESGRegistration.GATEWAY_FRAME.get()))
                place(level, p, CESGRegistration.GATEWAY_FRAME.get());
        place(level, origin.east(4), CESGRegistration.CROSS_DIMENSIONAL_GATEWAY_CORE.get());
        refresh(core);
        for (BlockPos cell : interior)
            helper.assertTrue(level.getBlockState(cell).is(CESGRegistration.GATEWAY_PORTAL.get()),
                    "Building a touching ring closed the active gateway");
        helper.assertTrue(GatewayPortalShape.describeFailure(level, corePos) == null,
                "Goggles report a frame fault on an open gateway");
        for (BlockPos cell : interior(origin, 3, 0, 2, 3))
            helper.assertTrue(level.getBlockState(cell).isAir(), "The unpowered neighbour opened");
        helper.succeed();
    }

    private static void refresh(CrossDimensionalGatewayCoreBlockEntity core) {
        try {
            Method method = CrossDimensionalGatewayCoreBlockEntity.class.getDeclaredMethod("refreshPortalState");
            method.setAccessible(true);
            method.invoke(core);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    private static void place(ServerLevel level, BlockPos pos, Block block) {
        level.getChunkAt(pos);
        level.setBlock(pos, block.defaultBlockState(), 3);
    }

    /** World X-plane outline of the w x h opening whose bottom-left outline corner is origin + (u0, y0). */
    private static List<BlockPos> outline(BlockPos origin, int u0, int y0, int w, int h) {
        List<BlockPos> cells = new java.util.ArrayList<>();
        for (int u = u0; u <= u0 + w + 1; u++)
            for (int y = y0; y <= y0 + h + 1; y++)
                if (u == u0 || u == u0 + w + 1 || y == y0 || y == y0 + h + 1)
                    cells.add(origin.offset(u, y, 0));
        return cells;
    }

    private static List<BlockPos> interior(BlockPos origin, int u0, int y0, int w, int h) {
        List<BlockPos> cells = new java.util.ArrayList<>();
        for (int u = u0 + 1; u <= u0 + w; u++)
            for (int y = y0 + 1; y <= y0 + h; y++)
                cells.add(origin.offset(u, y, 0));
        return cells;
    }

    // ---- Assertions ----

    /** The pre-2.0 flood-fill accepts this layout and detection returns exactly its result. */
    private static void assertSameAsLegacy(Layout layout, BlockPos core, String label) {
        GatewayPortalShape legacy = legacy(layout, core);
        Optional<GatewayPortalShape> shape = GatewayPortalShape.detect(layout, core);
        check(legacy != null, label + ": layout should be valid before 2.0");
        check(shape.isPresent(), label + ": layout no longer detects");
        check(shape.get().axis == legacy.axis, label + ": axis changed");
        check(shape.get().interior.equals(legacy.interior), label + ": interior changed");
        check(shape.get().frame.equals(legacy.frame), label + ": frame changed");
        check(GatewayPortalShape.describeFailure(layout, core) == null, label + ": goggles report a fault");
    }

    /** The pre-2.0 flood-fill rejects this layout, and the fallback opens exactly the given rectangle. */
    private static void assertNewlyOpen(Layout layout, BlockPos core, int u0, int y0, int w, int h) {
        String label = "opening at " + u0 + "," + y0 + " axis " + layout.axis;
        check(legacy(layout, core) == null, label + ": expected the pre-2.0 detection to fail");
        assertRectangle(layout, core, u0, y0, w, h);
        check(GatewayPortalShape.describeFailure(layout, core) == null, label + ": goggles report a fault");
    }

    private static void assertRectangle(Layout layout, BlockPos core, int u0, int y0, int w, int h) {
        String label = "rectangle at " + u0 + "," + y0 + " (" + w + "x" + h + ") axis " + layout.axis;
        Optional<GatewayPortalShape> shape = GatewayPortalShape.detect(layout, core);
        check(shape.isPresent(), label + ": not detected");
        check(shape.get().axis == layout.axis, label + ": wrong axis " + shape.get().axis);
        Set<BlockPos> interior = new HashSet<>();
        Set<BlockPos> frame = new HashSet<>();
        for (int u = u0; u <= u0 + w + 1; u++) {
            for (int y = y0; y <= y0 + h + 1; y++) {
                BlockPos p = layout.pos(u, y);
                if (u == u0 || u == u0 + w + 1 || y == y0 || y == y0 + h + 1) {
                    if (!p.equals(core))
                        frame.add(p);
                } else {
                    interior.add(p);
                }
            }
        }
        check(new HashSet<>(shape.get().interior).equals(interior) && shape.get().interior.size() == interior.size(),
                label + ": wrong interior " + shape.get().interior);
        check(new HashSet<>(shape.get().frame).equals(frame) && shape.get().frame.size() == frame.size(),
                label + ": wrong frame " + shape.get().frame);
    }

    /** The first Core placed in the layout stays shut and the goggles report this diagnostic. */
    private static void assertFails(Layout layout, String reason, String label) {
        BlockPos core = layout.firstCore;
        check(GatewayPortalShape.detect(layout, core).isEmpty(), label + ": broken ring opened");
        String actual = GatewayPortalShape.describeFailure(layout, core);
        check(reason.equals(actual), label + ": expected " + reason + " but got " + actual);
    }

    private static GatewayPortalShape legacy(Layout layout, BlockPos core) {
        try {
            Method method = GatewayPortalShape.class.getDeclaredMethod("detectRing", BlockGetter.class, BlockPos.class);
            method.setAccessible(true);
            return (GatewayPortalShape) method.invoke(null, layout, core);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }

    /**
     * Blocks in a map; everything else is air. Views share one map: {@link #plane} moves to a parallel
     * plane on the same axis and {@link #rotated} to the perpendicular plane through u = 0 of this one.
     */
    private static final class Layout implements BlockGetter {
        private static final int BASE_Y = 100;
        final Direction.Axis axis;
        final int fixed;
        final int uOffset;
        final Map<BlockPos, BlockState> blocks;
        BlockPos firstCore;

        Layout(Direction.Axis axis, int fixed) {
            this(axis, fixed, 0, new HashMap<>());
        }

        private Layout(Direction.Axis axis, int fixed, int uOffset, Map<BlockPos, BlockState> blocks) {
            this.axis = axis;
            this.fixed = fixed;
            this.uOffset = uOffset;
            this.blocks = blocks;
        }

        Layout plane(int otherFixed) {
            return new Layout(axis, otherFixed, uOffset, blocks);
        }

        /** The perpendicular plane through this plane's column u = {@code u}; its u = 0 is this plane. */
        Layout rotated(int u) {
            Direction.Axis other = axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
            return new Layout(other, u + uOffset, fixed, blocks);
        }

        BlockPos pos(int u, int y) {
            int w = u + uOffset;
            return axis == Direction.Axis.X ? new BlockPos(w, BASE_Y + y, fixed) : new BlockPos(fixed, BASE_Y + y, w);
        }

        /** Frames around a w x h opening whose bottom-left outline corner is (u0, y0). */
        void ring(int u0, int y0, int w, int h) {
            for (int u = u0; u <= u0 + w + 1; u++)
                for (int y = y0; y <= y0 + h + 1; y++)
                    if (u == u0 || u == u0 + w + 1 || y == y0 || y == y0 + h + 1)
                        frame(u, y);
        }

        void frame(int u, int y) {
            set(u, y, CESGRegistration.GATEWAY_FRAME.get());
        }

        BlockPos core(int u, int y) {
            set(u, y, CESGRegistration.CROSS_DIMENSIONAL_GATEWAY_CORE.get());
            BlockPos p = pos(u, y);
            if (firstCore == null)
                firstCore = p;
            return p;
        }

        void air(int u, int y) {
            blocks.remove(pos(u, y));
        }

        void set(int u, int y, Block block) {
            blocks.put(pos(u, y), block.defaultBlockState());
        }

        @Override
        public BlockEntity getBlockEntity(BlockPos pos) {
            return null;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState());
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return getBlockState(pos).getFluidState();
        }

        @Override
        public int getHeight() {
            return 384;
        }

        @Override
        public int getMinBuildHeight() {
            return -64;
        }
    }
}
