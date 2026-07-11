package com.cesg.ponder;

import com.cesg.gateways.GatewayPortalBlock;
import com.cesg.init.CESGRegistration;

import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * Ponder storyboards for the automated components. Station schematics are 5x2x5 (base plate at y=0,
 * station at (2,1,2) facing south, power shaft at (2,1,1), shulker box at (2,1,3), barrel at (1,1,2));
 * the gateway scene is a 5x5x5 vertical frame ring. Scenes animate the power shaft and item transfer.
 */
public final class CESGPonderScenes {
    private CESGPonderScenes() {}

    private static void itemHop(SceneBuilder scene, Vec3 from, Vec3 to, ItemStack stack) {
        Vec3 motion = to.subtract(from).scale(0.16).add(0, 0.18, 0);
        scene.world().createItemEntity(from, motion, stack);
    }

    // --- Shulker Loader: animated against the player-authored structure (assets/cesg/ponder/shulker_loader.nbt) ---
    // Loader (1,2,4) facing west, docked shulker rendered by its BER; barrel->chute feed at (1,3-4,4);
    // output belt z=2-3 (y=1) runs out toward the front-left.
    private static final BlockPos LOADER = new BlockPos(1, 2, 4);
    private static final BlockPos LOADER_FEED_TOP = new BlockPos(1, 4, 4);
    private static final BlockPos LOADER_BELT_OUT = new BlockPos(1, 1, 2);

    public static void shulkerLoader(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("shulker_loader", "Shulker Loader");
        scene.configureBasePlate(0, 0, 8);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().layersFrom(1), Direction.DOWN);
        scene.idle(20);

        scene.overlay().showText(80)
                .text("The Shulker Loader accepts a shulker box, docking it in place")
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(LOADER));
        scene.idle(90);

        scene.overlay().showText(80)
                .text("Items arrive by belt and funnel to fill the docked shulker")
                .placeNearTarget().pointAt(util.vector().topOf(LOADER_FEED_TOP));
        Vec3 feed = util.vector().centerOf(LOADER_FEED_TOP);
        for (int i = 0; i < 3; i++) {
            scene.world().createItemEntity(feed, new Vec3(0, -0.05, 0), new ItemStack(Items.IRON_INGOT));
            scene.idle(20);
        }
        scene.idle(30);

        scene.overlay().showText(80)
                .text("When full, the loaded shulker ejects onto an output belt")
                .placeNearTarget().pointAt(util.vector().centerOf(LOADER_BELT_OUT));
        // eject toward the front-left belt and glide off the edge
        Vec3 dock = util.vector().centerOf(LOADER).add(0, 0.4, 0);
        Vec3 b3 = util.vector().centerOf(new BlockPos(1, 1, 3)).add(0, 0.5, 0);
        Vec3 b2 = util.vector().centerOf(LOADER_BELT_OUT).add(0, 0.5, 0);
        itemHop(scene, dock, b3, new ItemStack(Items.SHULKER_BOX));
        scene.idle(22);
        itemHop(scene, b3, b2, new ItemStack(Items.SHULKER_BOX));
        scene.idle(22);
        itemHop(scene, b2, b2.add(0, 0, -1.3), new ItemStack(Items.SHULKER_BOX));
        scene.idle(60);
    }

    // --- Shulker Unloader: animated against the player structure (assets/cesg/ponder/shulker_unloader.nbt) ---
    // Unloader (0,2,3) facing west; smart chute (0,1,3) -> barrel/chest (0,0,3); belts z=1-2 and z=4-5 (y=1).
    private static final BlockPos UNLOADER = new BlockPos(0, 2, 3);
    private static final BlockPos UNLOADER_CHUTE = new BlockPos(0, 1, 3);
    private static final BlockPos UNLOADER_FUNNEL = new BlockPos(0, 2, 2);

    public static void shulkerUnloader(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("shulker_unloader", "Shulker Unloader");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().layersFrom(0), Direction.DOWN);
        scene.idle(20);

        scene.overlay().showText(80)
                .text("A full shulker box docks in the Shulker Unloader")
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(UNLOADER));
        scene.idle(90);

        scene.overlay().showText(80)
                .text("Its contents are pushed out the front of the funnel onto a belt")
                .placeNearTarget().pointAt(util.vector().centerOf(UNLOADER_FUNNEL));
        // cobblestone emerges from the funnel's north (front) face and rides off the belt
        Vec3 funnelFront = util.vector().centerOf(UNLOADER_FUNNEL).add(0, -0.1, -0.45);
        for (int i = 0; i < 4; i++) {
            scene.world().createItemEntity(funnelFront, new Vec3(0, -0.01, -0.14), new ItemStack(Items.COBBLESTONE));
            scene.idle(18);
        }
        scene.idle(25);

        scene.overlay().showText(80)
                .text("The empty shulker then drops down into storage below")
                .placeNearTarget().pointAt(util.vector().centerOf(UNLOADER_CHUTE));
        // empty shulker drops straight down through the smart chute into the barrel
        scene.world().createItemEntity(util.vector().centerOf(UNLOADER).add(0, -0.1, 0),
                new Vec3(0, -0.12, 0), new ItemStack(Items.SHULKER_BOX));
        scene.idle(70);
    }

    // --- Shulker Belt Loader: hovers above a belt, hose extends down to suck items up ---
    // Loader (1,3,3) facing west, belt (1,1,1..5) below at y=1; the loader is two blocks above the belt.
    private static final BlockPos BELT_LOADER = new BlockPos(1, 3, 3);

    public static void shulkerBeltLoader(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("shulker_belt_loader", "Shulker Belt Loader");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().layersFrom(1), Direction.DOWN);
        scene.idle(20);

        scene.overlay().showText(80)
                .text("The Shulker Belt Loader sits two blocks above a belt")
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(BELT_LOADER));
        scene.idle(90);

        scene.overlay().showText(90)
                .text("Its hose extends down to draw items off the belt into a shulker")
                .placeNearTarget().pointAt(util.vector().centerOf(new BlockPos(1, 2, 3)));
        extendHose(scene, BELT_LOADER, true);
        scene.idle(40);
        extendHose(scene, BELT_LOADER, false);
        scene.idle(20);

        scene.overlay().showText(90)
                .text("The filled shulker then drops from the funnel onto an item drain")
                .placeNearTarget().pointAt(util.vector().centerOf(new BlockPos(0, 2, 3)));
        // full shulker ejects to the side funnel, then drops onto the item drain below
        Vec3 funnel = util.vector().centerOf(new BlockPos(0, 3, 3));
        itemHop(scene, util.vector().centerOf(BELT_LOADER).add(0, 0.2, 0), funnel, new ItemStack(Items.SHULKER_BOX));
        scene.idle(22);
        scene.world().createItemEntity(funnel.add(0, -0.2, 0), new Vec3(0, -0.08, 0), new ItemStack(Items.SHULKER_BOX));
        scene.idle(70);
    }

    /** Step the belt-loader hose extension over a few keyframes (Ponder doesn't run the server tick). */
    private static void extendHose(SceneBuilder scene, BlockPos loader, boolean extend) {
        for (int s = 0; s <= 4; s++) {
            final float t = (extend ? s : 4 - s) / 4f;
            scene.world().modifyBlockEntity(loader,
                    com.cesg.storage.beltloader.ShulkerBeltLoaderBlockEntity.class, be -> be.ponderSetTubeProgress(t));
            scene.idle(4);
        }
    }

    // --- Shulker Belt Unloader: holds a full shulker above a belt, hose extends down to drop items ---
    // Unloader (1,3,3) facing west; belt (1,1,1..3) below (facing north); barrel->chute feed at (1,4-5,3).
    private static final BlockPos BELT_UNLOADER = new BlockPos(1, 3, 3);

    public static void shulkerBeltUnloader(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("shulker_belt_unloader", "Shulker Belt Unloader");
        scene.configureBasePlate(0, 0, 6);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().layersFrom(1), Direction.DOWN);
        scene.idle(20);

        scene.overlay().showText(80)
                .text("A full shulker box is fed into the Belt Unloader from above")
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(BELT_UNLOADER));
        scene.world().createItemEntity(util.vector().centerOf(new BlockPos(1, 5, 3)).add(0, -0.2, 0),
                new Vec3(0, -0.08, 0), new ItemStack(Items.SHULKER_BOX));
        scene.idle(90);

        scene.overlay().showText(80)
                .text("Its hose extends down toward the belt below")
                .placeNearTarget().pointAt(util.vector().centerOf(new BlockPos(1, 2, 3)));
        extendHoseUnloader(scene, BELT_UNLOADER, true);
        scene.idle(15);

        scene.overlay().showText(90)
                .text("It drops the shulker's contents onto the belt to be carried away")
                .placeNearTarget().pointAt(util.vector().centerOf(new BlockPos(1, 1, 3)));
        Vec3 tip = util.vector().centerOf(new BlockPos(1, 2, 3)).add(0, -0.2, 0);
        Vec3 b3 = util.vector().centerOf(new BlockPos(1, 1, 3)).add(0, 0.45, 0);
        Vec3 b1 = util.vector().centerOf(new BlockPos(1, 1, 1)).add(0, 0.45, 0);
        for (int i = 0; i < 3; i++) {
            scene.world().createItemEntity(tip, new Vec3(0, -0.05, 0), new ItemStack(Items.COBBLESTONE));
            scene.idle(8);
            itemHop(scene, b3, b1, new ItemStack(Items.COBBLESTONE));
            scene.idle(8);
            itemHop(scene, b1, b1.add(0, 0, -1.2), new ItemStack(Items.COBBLESTONE));
            scene.idle(8);
        }
        extendHoseUnloader(scene, BELT_UNLOADER, false);
        scene.idle(40);
    }

    private static void extendHoseUnloader(SceneBuilder scene, BlockPos unloader, boolean extend) {
        for (int s = 0; s <= 4; s++) {
            final float t = (extend ? s : 4 - s) / 4f;
            scene.world().modifyBlockEntity(unloader,
                    com.cesg.storage.beltunloader.ShulkerBeltUnloaderBlockEntity.class, be -> be.ponderSetTubeProgress(t));
            scene.idle(4);
        }
    }

    // --- Gateway Core: vertical ring at x=0; pump+tank at (0,5-6,3); portal interior y1-3,z1-2; core (0,4,0) ---
    private static final BlockPos GATEWAY_CORE = new BlockPos(0, 4, 0);
    private static final BlockPos GATEWAY_PUMP = new BlockPos(0, 5, 3);
    private static final BlockPos[] GATEWAY_FRAMES = {
            new BlockPos(0, 4, 3), new BlockPos(0, 3, 3), new BlockPos(0, 2, 3), new BlockPos(0, 1, 3),
            new BlockPos(0, 0, 3), new BlockPos(0, 0, 2), new BlockPos(0, 0, 1), new BlockPos(0, 0, 0),
            new BlockPos(0, 1, 0), new BlockPos(0, 2, 0), new BlockPos(0, 3, 0),
            new BlockPos(0, 4, 1), new BlockPos(0, 4, 2), new BlockPos(0, 4, 0)
    };

    public static void gatewayCore(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("cross_dimensional_gateway_core", "Cross-Dimensional Gateway Core");
        scene.configureBasePlate(0, 0, 4);

        Selection interior = util.select().fromTo(new BlockPos(0, 1, 1), new BlockPos(0, 3, 2));
        Selection ring = util.select().fromTo(new BlockPos(0, 0, 0), new BlockPos(1, 4, 3));
        // start the gateway inactive: unlit frames/core, portal interior cleared
        scene.world().modifyBlocks(ring, CESGPonderScenes::unlit, false);
        scene.world().setBlocks(interior, Blocks.AIR.defaultBlockState(), false);

        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().layersFrom(0), Direction.DOWN);
        scene.idle(20);

        scene.overlay().showText(80)
                .text("Build a vertical Gateway Frame ring with the Core as one of its blocks")
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().centerOf(GATEWAY_CORE));
        scene.idle(90);

        scene.overlay().showText(90)
                .text("Pump Liquid Eye of Ender into any frame to fuel and light the ring")
                .placeNearTarget().pointAt(util.vector().centerOf(GATEWAY_PUMP));
        for (BlockPos f : GATEWAY_FRAMES) {
            scene.world().modifyBlock(f, CESGPonderScenes::lit, false);
            scene.idle(6);
        }
        scene.idle(30);

        scene.overlay().showText(90)
                .text("Powered and bound, the interior opens into a portal")
                .placeNearTarget().pointAt(util.vector().centerOf(new BlockPos(0, 2, 1)));
        BlockState portal = CESGRegistration.GATEWAY_PORTAL.get().defaultBlockState()
                .setValue(GatewayPortalBlock.AXIS, Direction.Axis.Z);
        scene.world().setBlocks(interior, portal, false);
        scene.idle(10);
        scene.world().createItemEntity(util.vector().centerOf(new BlockPos(0, 2, 1)).add(-1.2, 0, 0),
                new Vec3(0.16, 0.04, 0), new ItemStack(Items.ENDER_PEARL));
        scene.idle(100);
    }

    private static BlockState lit(BlockState state) {
        return state.hasProperty(BlockStateProperties.LIT) ? state.setValue(BlockStateProperties.LIT, true) : state;
    }

    private static BlockState unlit(BlockState state) {
        return state.hasProperty(BlockStateProperties.LIT) ? state.setValue(BlockStateProperties.LIT, false) : state;
    }
}
