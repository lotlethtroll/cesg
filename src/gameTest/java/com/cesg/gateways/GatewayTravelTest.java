package com.cesg.gateways;

import java.util.ArrayList;
import java.util.List;

import com.cesg.gateways.teleport.GatewayPartner;
import com.cesg.init.CESGRegistration;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Travel between two bound, powered, fuelled gateways built the normal way: the ring's bottom Frame
 * sits on the ground, so a traveller enters standing on it, one block above the floor. They must land
 * on the floor in front of the partner portal and stay there. Landing inside the partner portal
 * bounced them home after every cooldown, paying the travel cost each time.
 */
@GameTestHolder("cesg")
@PrefixGameTestTemplate(false)
public class GatewayTravelTest {
    private static final int FUEL = 2000;

    @GameTest(template = "empty", timeoutTicks = 400)
    public void idleGatewaysKeepTheirFuel(GameTestHelper helper) {
        Pair pair = touchingPair(helper);
        run(helper, pair, Float.NaN, () -> {
            helper.assertTrue(pair.a.getEssenceMb() == FUEL && pair.b.getEssenceMb() == FUEL,
                    "Idle gateways lost fuel: A=" + pair.a.getEssenceMb() + " B=" + pair.b.getEssenceMb());
        });
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public void travellerCrossesTouchingGatewaysOnce(GameTestHelper helper) {
        crossesOnce(helper, touchingPair(helper), 180f); // facing north
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public void travellerCrossesSeparateGatewaysOnce(GameTestHelper helper) {
        crossesOnce(helper, separatePair(helper), 0f); // facing south
    }

    /** A traveller standing still in A's portal plane crosses once and exits B the way they face. */
    private static void crossesOnce(GameTestHelper helper, Pair pair, float yaw) {
        run(helper, pair, yaw, () -> {
            int cost = CrossDimensionalGatewayCoreBlockEntity.travelCost();
            helper.assertTrue(pair.a.getEssenceMb() == FUEL - cost && pair.b.getEssenceMb() == FUEL,
                    "Expected exactly one A->B trip, fuel A=" + pair.a.getEssenceMb() + " B=" + pair.b.getEssenceMb());
            BlockPos at = pair.stand[0].blockPosition();
            helper.assertTrue(!pair.bInterior.contains(at), "Traveller landed inside the partner portal at " + at);
            helper.assertTrue(at.distManhattan(pair.bInterior.get(0)) <= 2,
                    "Traveller did not arrive in front of the partner portal: " + at);
            helper.assertTrue(at.getY() == pair.floorY + 1, "Traveller is not standing on the floor: " + at);
            int plane = pair.bInterior.get(0).getZ();
            helper.assertTrue(yaw == 180f ? at.getZ() < plane : at.getZ() > plane,
                    "Traveller did not exit the way they were facing: " + at);
        });
    }

    /**
     * Ticks the pair for 300 ticks (several cooldowns). Unless {@code yaw} is NaN, an armour stand facing
     * that way stands still in A's portal plane.
     */
    private static void run(GameTestHelper helper, Pair pair, float yaw, Runnable check) {
        ServerLevel level = helper.getLevel();
        pair.a.setPartner(new GatewayPartner(level.dimension(), pair.b.getBlockPos(), true));
        pair.b.setPartner(new GatewayPartner(level.dimension(), pair.a.getBlockPos(), true));
        pair.a.fillEssence(FUEL, false);
        pair.b.fillEssence(FUEL, false);
        int[] tick = {0};
        helper.onEachTick(() -> {
            pair.a.setSpeed(16);
            pair.b.setSpeed(16);
            if (tick[0]++ == 40 && !Float.isNaN(yaw)) {
                BlockPos cell = pair.aInterior.get(0);
                pair.stand[0] = new ArmorStand(level, cell.getX() + 0.5, cell.getY(), cell.getZ() + 0.5);
                pair.stand[0].setYRot(yaw);
                level.addFreshEntity(pair.stand[0]);
            }
        });
        helper.runAfterDelay(300, () -> {
            check.run();
            helper.succeed();
        });
    }

    private record Pair(CrossDimensionalGatewayCoreBlockEntity a, CrossDimensionalGatewayCoreBlockEntity b,
            List<BlockPos> aInterior, List<BlockPos> bInterior, int floorY, ArmorStand[] stand) {}

    /** Two 1x2 openings sharing the middle Frame column, Cores in the outer bottom corners. */
    private static Pair touchingPair(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos o = helper.absolutePos(new BlockPos(2, 2, 4));
        floor(level, o.offset(-1, -1, -2), 7, 5);
        for (int x = 0; x <= 4; x++)
            for (int y = 0; y <= 3; y++)
                if (!((x == 1 || x == 3) && (y == 1 || y == 2)))
                    place(level, o.offset(x, y, 0), CESGRegistration.GATEWAY_FRAME.get());
        return pair(level, o, o.offset(4, 0, 0), List.of(o.offset(1, 1, 0), o.offset(1, 2, 0)),
                List.of(o.offset(3, 1, 0), o.offset(3, 2, 0)), o.getY() - 1);
    }

    /** Two independent 1x2 rings six blocks apart. */
    private static Pair separatePair(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos o = helper.absolutePos(new BlockPos(2, 2, 4));
        BlockPos p = helper.absolutePos(new BlockPos(2, 2, 10));
        for (BlockPos base : List.of(o, p)) {
            floor(level, base.offset(-1, -1, -2), 5, 5);
            for (int x = 0; x <= 2; x++)
                for (int y = 0; y <= 3; y++)
                    if (!(x == 1 && (y == 1 || y == 2)))
                        place(level, base.offset(x, y, 0), CESGRegistration.GATEWAY_FRAME.get());
        }
        return pair(level, o, p, List.of(o.offset(1, 1, 0), o.offset(1, 2, 0)),
                List.of(p.offset(1, 1, 0), p.offset(1, 2, 0)), o.getY() - 1);
    }

    private static Pair pair(ServerLevel level, BlockPos a, BlockPos b, List<BlockPos> aInterior,
            List<BlockPos> bInterior, int floorY) {
        place(level, a, CESGRegistration.CROSS_DIMENSIONAL_GATEWAY_CORE.get());
        place(level, b, CESGRegistration.CROSS_DIMENSIONAL_GATEWAY_CORE.get());
        return new Pair((CrossDimensionalGatewayCoreBlockEntity) level.getBlockEntity(a),
                (CrossDimensionalGatewayCoreBlockEntity) level.getBlockEntity(b), new ArrayList<>(aInterior),
                new ArrayList<>(bInterior), floorY, new ArmorStand[1]);
    }

    private static void floor(ServerLevel level, BlockPos corner, int sizeX, int sizeZ) {
        for (int x = 0; x < sizeX; x++)
            for (int z = 0; z < sizeZ; z++)
                place(level, corner.offset(x, 0, z), Blocks.STONE);
    }

    private static void place(ServerLevel level, BlockPos pos, Block block) {
        level.getChunkAt(pos);
        level.setBlock(pos, block.defaultBlockState(), 3);
    }
}
