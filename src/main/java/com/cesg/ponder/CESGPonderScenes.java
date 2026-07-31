package com.cesg.ponder;

import com.cesg.gateways.CrossDimensionalGatewayCoreBlockEntity;
import com.cesg.gateways.GatewayFluxBatteryBlock;
import com.cesg.gateways.GatewayFluxBatteryBlockEntity;
import com.cesg.gateways.GatewayFrameBlock;
import com.cesg.gateways.GatewayPortalBlock;
import com.cesg.gateways.StorageBridgeBlock;
import com.cesg.gateways.StorageBridgeBlockEntity;
import com.cesg.init.CESGFluids;
import com.cesg.init.CESGRegistration;
import com.cesg.storage.beltloader.ShulkerBeltLoaderBlockEntity;
import com.cesg.storage.beltunloader.ShulkerBeltUnloaderBlockEntity;
import com.cesg.storage.network.StorageTerminalBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import com.simibubi.create.foundation.ponder.element.BeltItemElement;

import net.createmod.ponder.api.element.ElementLink;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Ponder storyboards for the automated components. Every schematic has a Create-style base plate at y=0
 * (checkerboard of white concrete / snow) with the structure starting at y=1, so all scenes reveal with
 * {@code showBasePlate()} then {@code showSection(layersFrom(1))}.
 *
 * <p>Every storyboard wraps the incoming {@link SceneBuilder} in Create's {@link CreateSceneBuilder}. Ponder
 * does not run the kinetic network, so without an explicit {@code setKineticSpeed} every belt, shaft, cogwheel
 * and pump renders frozen. Wrapping also unlocks the belt-aware item instructions
 * ({@code createItemOnBelt} / {@code createItemOnBeltLike}), which hand items to the belt's own transport
 * behaviour instead of faking the motion with thrown entities.
 *
 * <p>Belt travel direction is <em>not</em> encoded in the belt blockstate — {@code getMovementFacing()} reads
 * only the belt's axis and the sign of its speed. All belts in these schematics run along Z, so a negative
 * speed carries items north (-Z) and a positive speed south (+Z).
 */
public final class CESGPonderScenes {
    private CESGPonderScenes() {}

    /** Belt/shaft speed for the station scenes. Sign selects belt travel direction; see the class note. */
    private static final float BELT_SPEED = 32f;
    /**
     * Ticks for a belt item to cross one block. {@code BeltBlockEntity.getBeltMovementSpeed()} is
     * {@code getSpeed() / 480}, so this is the inverse — 15 ticks per block at speed 32. Derived rather
     * than hardcoded so retiming a scene is just a change to {@link #BELT_SPEED}.
     */
    private static final int TICKS_PER_BLOCK = Math.round(480f / BELT_SPEED);

    /** Free-flight hop, for transfers that are not belt-backed (piped fluid, network throughput). */
    private static void itemHop(CreateSceneBuilder scene, Vec3 from, Vec3 to, ItemStack stack) {
        Vec3 motion = to.subtract(from).scale(0.16).add(0, 0.18, 0);
        scene.world().createItemEntity(from, motion, stack);
    }

    // --- Shulker Loader (shulker_loader.nbt, 8x5x8 with an 8x8 base plate) ---
    // The workshop is symmetric about the loader at (3,2,4) facing west: a barrel -> chute dock feed
    // above it, an input belt run at z=5-6 whose funnel loads the docked box, and an output belt run at
    // z=2-3 that carries the finished box away. Both runs carry north, so the whole scene flows one way.
    private static final BlockPos LOADER = new BlockPos(3, 2, 4);
    private static final BlockPos LOADER_DOCK_CHUTE = new BlockPos(3, 3, 4);
    private static final BlockPos LOADER_DOCK_BARREL = new BlockPos(3, 4, 4);
    /** Far end of the input run — items are inserted here and ride north to the loading funnel. */
    private static final BlockPos LOADER_IN_BELT = new BlockPos(3, 1, 6);
    /** Belt cell under the loading funnel; one block of travel from {@link #LOADER_IN_BELT}. */
    private static final BlockPos LOADER_IN_BELT_HEAD = new BlockPos(3, 1, 5);
    private static final BlockPos LOADER_IN_FUNNEL = new BlockPos(3, 2, 5);
    /** Belt block nearest the loader — where the finished shulker joins the output run. */
    private static final BlockPos LOADER_BELT_HEAD = new BlockPos(3, 1, 3);
    private static final BlockPos LOADER_BELT_OUT = new BlockPos(3, 1, 2);

    public static void shulkerLoader(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("shulker_loader", "Shulker Loader");
        scene.configureBasePlate(0, 0, 8);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().layersFrom(1), Direction.DOWN);
        scene.idle(20);
        // Negative: both runs carry north, so items flow toward the loader and then off the front edge.
        scene.world().setKineticSpeed(util.select().everywhere(), -BELT_SPEED);
        scene.idle(10);

        scene.overlay().showText(80)
                .text("An empty shulker box drops in from above and docks in the Shulker Loader")
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(LOADER_DOCK_BARREL));
        scene.world().createItemOnBeltLike(LOADER_DOCK_CHUTE, Direction.DOWN, new ItemStack(Items.SHULKER_BOX));
        scene.idle(90);

        scene.overlay().showText(90)
                .text("Items ride in on a belt and the funnel loads them into the docked box")
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().centerOf(LOADER_IN_FUNNEL));
        // One item at a time: it rides the real belt to the funnel, pauses there, then is taken in.
        for (int i = 0; i < 3; i++)
            beltItemConsumedAt(scene, LOADER_IN_BELT, Direction.SOUTH, LOADER_IN_BELT_HEAD,
                    LOADER_IN_FUNNEL, new ItemStack(Items.IRON_INGOT), 1);
        scene.idle(20);

        scene.overlay().showText(80)
                .text("When full, the loaded shulker ejects onto the output belt")
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().centerOf(LOADER_BELT_OUT));
        scene.world().createItemOnBelt(LOADER_BELT_HEAD, Direction.SOUTH, new ItemStack(Items.SHULKER_BOX));
        scene.idle(70);
    }

    /**
     * Ride one item along a real belt into a machine and have the machine take it.
     *
     * <p>Ponder cannot do the pickup for us: every transfer path in this mod and in Create is behind
     * {@code if (level.isClientSide) return;} and {@code PonderLevel} is client-side, so no funnel or
     * station will ever grab anything on its own. What is real is the travel — {@code createItemOnBelt}
     * hands a genuine {@code TransportedItemStack} to the belt's own transport behaviour, and block
     * entities in a shown section do tick. So the item moves for real and only the hand-off is scripted:
     * lock it where it arrives, flap the funnel, then clear the run.
     *
     * <p><b>{@code insertFrom} is the side the item arrives FROM, not the way it travels.</b>
     * {@code createItemOnBelt} passes it through {@code getOpposite()} into
     * {@code DirectBeltInputBehaviour.handleInsertion}, and a belt's {@code canInsertFrom} accepts only
     * when {@code getMovementFacing() != side.getOpposite()} — which reduces to "the argument must not
     * equal the belt's movement direction". Passing the travel direction silently inserts nothing, so a
     * scene renders moving belts with no cargo. On a Z-axis belt a negative speed moves items NORTH
     * (movement facing is derived from the speed sign, never from {@code facing}), so a north-running
     * belt is fed with {@code SOUTH}.
     *
     * @param blocks how many blocks of belt lie between the insertion point and {@code arriveAt}
     */
    private static void beltItemConsumedAt(CreateSceneBuilder scene, BlockPos insertAt, Direction insertFrom,
            BlockPos arriveAt, BlockPos funnel, ItemStack stack, int blocks) {
        ElementLink<BeltItemElement> item = scene.world().createItemOnBelt(insertAt, insertFrom, stack);
        scene.idle(blocks * TICKS_PER_BLOCK + 2);
        scene.world().stallBeltItem(item, true);
        if (funnel != null)
            scene.world().flapFunnel(funnel, false);
        scene.idle(10);
        // removeItemsFromBelt clears every item on this belt's handler, so only ever run one at a time.
        scene.world().removeItemsFromBelt(arriveAt);
        scene.idle(8);
    }

    // --- Shulker Unloader (shulker_unloader.nbt, 7x3x7 with a 7x7 base plate) ---
    // Unloader (2,2,3) facing west; smart chute (2,1,3) -> barrel below; belts z=1-2 and z=4-5 (y=1).
    private static final BlockPos UNLOADER = new BlockPos(2, 2, 3);
    private static final BlockPos UNLOADER_CHUTE = new BlockPos(2, 1, 3);
    private static final BlockPos UNLOADER_FUNNEL = new BlockPos(2, 2, 2);
    /** Belt beneath the output funnel; createItemOnBelt flaps the funnel above it for free. */
    private static final BlockPos UNLOADER_BELT_HEAD = new BlockPos(2, 1, 2);

    public static void shulkerUnloader(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("shulker_unloader", "Shulker Unloader");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().layersFrom(1), Direction.DOWN);
        scene.idle(20);
        scene.world().setKineticSpeed(util.select().everywhere(), -BELT_SPEED);
        scene.idle(10);

        scene.overlay().showText(80)
                .text("A full shulker box docks in the Shulker Unloader")
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(UNLOADER));
        scene.idle(90);

        scene.overlay().showText(80)
                .text("Its contents are pushed out through the funnel onto a belt")
                .placeNearTarget().pointAt(util.vector().centerOf(UNLOADER_FUNNEL));
        for (int i = 0; i < 4; i++) {
            scene.world().createItemOnBelt(UNLOADER_BELT_HEAD, Direction.SOUTH, new ItemStack(Items.COBBLESTONE));
            scene.idle(18);
        }
        scene.idle(25);

        scene.overlay().showText(80)
                .text("The empty shulker then drops down into storage below")
                .placeNearTarget().pointAt(util.vector().centerOf(UNLOADER_CHUTE));
        scene.world().createItemOnBeltLike(UNLOADER_CHUTE, Direction.DOWN, new ItemStack(Items.SHULKER_BOX));
        scene.idle(70);
    }

    // --- Shulker Belt Loader (shulker_belt_loader.nbt, 7x6x7 with a 7x7 base plate) ---
    // Loader (3,3,3) facing west, belt (3,1,1..5) below at y=1; output funnel (2,3,3) -> item drain (2,2,3).
    private static final BlockPos BELT_LOADER = new BlockPos(3, 3, 3);
    private static final BlockPos BELT_LOADER_HOSE = new BlockPos(3, 2, 3);
    /** Belt block at the input end of the run; items ride south from here toward the hose. */
    private static final BlockPos BELT_LOADER_BELT_IN = new BlockPos(3, 1, 1);
    /** Belt cell directly under the hose tip — two blocks of travel from {@link #BELT_LOADER_BELT_IN}. */
    private static final BlockPos BELT_LOADER_BELT_UNDER_HOSE = new BlockPos(3, 1, 3);
    private static final BlockPos BELT_LOADER_FUNNEL = new BlockPos(2, 3, 3);
    private static final BlockPos BELT_LOADER_DRAIN = new BlockPos(2, 2, 3);

    public static void shulkerBeltLoader(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("shulker_belt_loader", "Shulker Belt Loader");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().layersFrom(1), Direction.DOWN);
        scene.idle(20);
        // Positive: the belt carries south, feeding items from z=1 up to the hose at z=3.
        scene.world().setKineticSpeed(util.select().everywhere(), BELT_SPEED);
        scene.idle(10);

        scene.overlay().showText(80)
                .text("The Shulker Belt Loader sits two blocks above a belt")
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(BELT_LOADER));
        scene.idle(90);

        scene.overlay().showText(90)
                .text("Its hose extends down to draw items off the belt into a shulker")
                .placeNearTarget().pointAt(util.vector().centerOf(BELT_LOADER_HOSE));
        extendHose(scene, BELT_LOADER, true);
        // Items ride the belt for real from z=1, stop under the hose tip at z=3, then are drawn up.
        for (int i = 0; i < 3; i++)
            beltItemConsumedAt(scene, BELT_LOADER_BELT_IN, Direction.NORTH, BELT_LOADER_BELT_UNDER_HOSE,
                    null, new ItemStack(Items.IRON_INGOT), 2);
        scene.idle(15);
        extendHose(scene, BELT_LOADER, false);
        scene.idle(15);

        scene.overlay().showText(90)
                .text("The filled shulker then drops from the funnel onto an item drain")
                .placeNearTarget().pointAt(util.vector().centerOf(BELT_LOADER_DRAIN));
        scene.world().flapFunnel(BELT_LOADER_FUNNEL, true);
        scene.idle(10);
        scene.world().createItemOnBeltLike(BELT_LOADER_DRAIN, Direction.DOWN, new ItemStack(Items.SHULKER_BOX));
        scene.idle(70);
    }

    /**
     * Step the belt-loader hose extension over a few keyframes. Ponder never runs the server tick, so the
     * tube phase has to be driven straight onto the block entity; see
     * {@link ShulkerBeltLoaderBlockEntity#ponderSetTubeProgress}.
     */
    private static void extendHose(CreateSceneBuilder scene, BlockPos loader, boolean extend) {
        for (int s = 0; s <= 4; s++) {
            final float t = (extend ? s : 4 - s) / 4f;
            scene.world().modifyBlockEntity(loader,
                    ShulkerBeltLoaderBlockEntity.class, be -> be.ponderSetTubeProgress(t));
            scene.idle(4);
        }
    }

    // --- Shulker Belt Unloader (shulker_belt_unloader.nbt, 5x6x5 with a 5x5 base plate) ---
    // Unloader (2,3,3) facing west; belt (2,1,1..3) below; barrel->chute feed at (2,4-5,3).
    private static final BlockPos BELT_UNLOADER = new BlockPos(2, 3, 3);
    private static final BlockPos BELT_UNLOADER_HOSE = new BlockPos(2, 2, 3);
    private static final BlockPos BELT_UNLOADER_CHUTE = new BlockPos(2, 4, 3);
    /** Belt block under the hose; contents land here and ride north off the front edge. */
    private static final BlockPos BELT_UNLOADER_BELT_HEAD = new BlockPos(2, 1, 3);

    public static void shulkerBeltUnloader(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("shulker_belt_unloader", "Shulker Belt Unloader");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().layersFrom(1), Direction.DOWN);
        scene.idle(20);
        // Negative: contents dropped at z=3 ride north and off the front edge.
        scene.world().setKineticSpeed(util.select().everywhere(), -BELT_SPEED);
        scene.idle(10);

        scene.overlay().showText(80)
                .text("A full shulker box is fed into the Belt Unloader from above")
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(BELT_UNLOADER));
        scene.world().createItemOnBeltLike(BELT_UNLOADER_CHUTE, Direction.DOWN, new ItemStack(Items.SHULKER_BOX));
        scene.idle(90);

        scene.overlay().showText(80)
                .text("Its hose extends down toward the belt below")
                .placeNearTarget().pointAt(util.vector().centerOf(BELT_UNLOADER_HOSE));
        extendHoseUnloader(scene, BELT_UNLOADER, true);
        scene.idle(15);

        scene.overlay().showText(90)
                .text("It drops the shulker's contents onto the belt to be carried away")
                .placeNearTarget().pointAt(util.vector().centerOf(BELT_UNLOADER_BELT_HEAD));
        for (int i = 0; i < 4; i++) {
            scene.world().createItemOnBelt(BELT_UNLOADER_BELT_HEAD, Direction.SOUTH, new ItemStack(Items.COBBLESTONE));
            scene.idle(16);
        }
        scene.idle(20);
        extendHoseUnloader(scene, BELT_UNLOADER, false);
        scene.idle(40);
    }

    /** @see #extendHose */
    private static void extendHoseUnloader(CreateSceneBuilder scene, BlockPos unloader, boolean extend) {
        for (int s = 0; s <= 4; s++) {
            final float t = (extend ? s : 4 - s) / 4f;
            scene.world().modifyBlockEntity(unloader,
                    ShulkerBeltUnloaderBlockEntity.class, be -> be.ponderSetTubeProgress(t));
            scene.idle(4);
        }
    }

    // --- Ender Infuser: reuses the loader workshop as a compact powered-machine demonstration. ---
    private static final BlockPos INFUSER = LOADER;

    public static void enderInfuser(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("ender_infuser", "Ender Infuser");
        scene.configureBasePlate(0, 0, 8);
        scene.world().setBlocks(util.select().position(INFUSER),
                CESGRegistration.ENDER_INFUSER.get().defaultBlockState(), false);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().layersFrom(1), Direction.DOWN);
        scene.idle(20);
        scene.world().setKineticSpeed(util.select().everywhere(), BELT_SPEED);
        scene.idle(10);

        scene.overlay().showText(80)
                .text("Supply rotational force and pipe a recipe fluid into the input")
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().centerOf(INFUSER));
        itemHop(scene, util.vector().centerOf(INFUSER).add(1.4, 0.6, 0),
                util.vector().centerOf(INFUSER).add(0.3, 0.6, 0),
                new ItemStack(CESGFluids.LIQUID_ENDER_PEARL.get().getBucket()));
        scene.idle(90);

        scene.overlay().showText(80)
                .text("Insert the required catalysts into the Infuser")
                .placeNearTarget().pointAt(util.vector().topOf(INFUSER));
        scene.world().createItemOnBeltLike(LOADER_DOCK_CHUTE, Direction.DOWN, new ItemStack(Items.CHORUS_FRUIT));
        scene.idle(90);

        scene.overlay().showText(80)
                .text("Completed fluid leaves through the output connection")
                .placeNearTarget().pointAt(util.vector().centerOf(INFUSER).add(-0.4, 0.3, 0));
        itemHop(scene, util.vector().centerOf(INFUSER).add(-0.2, 0.6, 0),
                util.vector().centerOf(INFUSER).add(-1.5, 0.6, 0),
                new ItemStack(CESGFluids.TELEPORT_ESSENCE.get().getBucket()));
        scene.idle(80);
    }

    // --- Storage network: the ring plane is x=1, so the blocks that bolt onto it stack in the x=2 column,
    // resting on the base plate. The schematic parks the battery's fuel feed in those same three cells, so
    // these scenes overwrite all of them. ---
    private static final BlockPos BRIDGE = new BlockPos(2, 1, 0);
    private static final BlockPos NETWORK_CONTROLLER = new BlockPos(2, 2, 0);
    private static final BlockPos TERMINAL = new BlockPos(2, 3, 0);

    /** Which block the shared storage-network scene is being told from. */
    private enum NetworkFocus { BRIDGE, CONTROLLER, TERMINAL }

    public static void storageBridge(SceneBuilder builder, SceneBuildingUtil util) {
        storageNetwork(builder, util, NetworkFocus.BRIDGE);
    }

    public static void storageNetworkController(SceneBuilder builder, SceneBuildingUtil util) {
        storageNetwork(builder, util, NetworkFocus.CONTROLLER);
    }

    public static void storageTerminal(SceneBuilder builder, SceneBuildingUtil util) {
        storageNetwork(builder, util, NetworkFocus.TERMINAL);
    }

    /**
     * One staging of the storage network, narrated from whichever block the player pressed W on. The layout is
     * shared because all three blocks only do anything as a cluster, but each focus gets its own scene id so
     * its title and text resolve to its own lang keys.
     */
    private static void storageNetwork(SceneBuilder builder, SceneBuildingUtil util, NetworkFocus focus) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        switch (focus) {
            case BRIDGE -> scene.title("storage_bridge", "Cross-Dimensional Storage Bridge");
            case CONTROLLER -> scene.title("storage_network_controller", "Storage Network Controller");
            case TERMINAL -> scene.title("storage_terminal", "Storage Terminal");
        }
        scene.configureBasePlate(0, 0, 4);

        // The schematic ships a cold ring, so these scenes bring it up before the reveal.
        primeGateway(scene, util, CrossDimensionalGatewayCoreBlockEntity.TANK_CAPACITY);
        // Bridge starts OFFLINE — its gauge only goes green once a partner answers.
        scene.world().setBlocks(util.select().position(BRIDGE),
                CESGRegistration.STORAGE_BRIDGE.get().defaultBlockState(), false);
        scene.world().setBlocks(util.select().position(NETWORK_CONTROLLER),
                CESGRegistration.STORAGE_NETWORK_CONTROLLER.get().defaultBlockState(), false);
        // NORTH points the console face at the camera; the default placement faces away.
        scene.world().setBlocks(util.select().position(TERMINAL),
                CESGRegistration.STORAGE_TERMINAL.get().defaultBlockState()
                        .setValue(StorageTerminalBlock.FACING, Direction.NORTH), false);

        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().layersFrom(1), Direction.DOWN);
        scene.idle(20);
        scene.world().setKineticSpeed(util.select().everywhere(), BELT_SPEED);
        scene.world().propagatePipeChange(GATEWAY_PUMP);
        scene.idle(10);

        switch (focus) {
            case BRIDGE -> {
                scene.overlay().showText(80)
                        .text("Attach a Storage Bridge to a powered, bound Gateway ring")
                        .attachKeyFrame().placeNearTarget().pointAt(util.vector().centerOf(BRIDGE));
                scene.idle(90);

                scene.overlay().showText(90)
                        .text("Its gauge reads OFFLINE until a matching Bridge on the far ring answers")
                        .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(BRIDGE));
                scene.idle(100);

                scene.overlay().showText(100)
                        .text("A green LIVE gauge means the pair is up — filtered items cross the Gateway")
                        .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(BRIDGE));
                setBridgeStatus(scene, StorageBridgeBlockEntity.RemoteStatus.LIVE);
                scene.idle(20);
                for (int i = 0; i < 3; i++) {
                    itemHop(scene, util.vector().centerOf(BRIDGE).add(0, 0.7, 0),
                            util.vector().centerOf(GATEWAY_CORE).add(0, 0.4, 0), new ItemStack(Items.ENDER_PEARL));
                    scene.idle(26);
                }
                scene.idle(40);

                scene.overlay().showText(110)
                        .text("If the partner Bridge is missing or unloaded the gauge turns amber and reads FAULT")
                        .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(BRIDGE));
                setBridgeStatus(scene, StorageBridgeBlockEntity.RemoteStatus.FAULT);
                scene.idle(90);
                // Back to LIVE so the scene loops from a healthy state.
                setBridgeStatus(scene, StorageBridgeBlockEntity.RemoteStatus.LIVE);
                scene.idle(40);
            }
            case CONTROLLER -> {
                setBridgeStatus(scene, StorageBridgeBlockEntity.RemoteStatus.LIVE);
                scene.overlay().showText(80)
                        .text("The Storage Network Controller anchors a cluster of adjacent storage blocks")
                        .attachKeyFrame().placeNearTarget().pointAt(util.vector().centerOf(NETWORK_CONTROLLER));
                scene.idle(90);
                scene.overlay().showText(90)
                        .text("Place it touching the Bridge to expose that storage to the Gateway")
                        .attachKeyFrame().placeNearTarget().pointAt(util.vector().centerOf(BRIDGE));
                scene.idle(100);
                scene.overlay().showText(100)
                        .text("Right-click the Controller to read back the network's size and contents")
                        .placeNearTarget().pointAt(util.vector().topOf(NETWORK_CONTROLLER));
                itemHop(scene, util.vector().centerOf(NETWORK_CONTROLLER).add(0, 0.7, 0),
                        util.vector().centerOf(GATEWAY_CORE).add(0, 0.4, 0), new ItemStack(Items.ENDER_PEARL));
                scene.idle(90);
            }
            case TERMINAL -> {
                setBridgeStatus(scene, StorageBridgeBlockEntity.RemoteStatus.LIVE);
                scene.overlay().showText(80)
                        .text("The Storage Terminal is the console for a Controller's network")
                        .attachKeyFrame().placeNearTarget().pointAt(util.vector().centerOf(TERMINAL));
                scene.idle(90);
                scene.overlay().showText(90)
                        .text("It must touch a cluster that contains a Storage Network Controller")
                        .attachKeyFrame().placeNearTarget().pointAt(util.vector().centerOf(NETWORK_CONTROLLER));
                scene.idle(100);
                scene.overlay().showText(100)
                        .text("Open it to browse local stock and, across a Bridge, the remote network's contents")
                        .placeNearTarget().pointAt(util.vector().topOf(TERMINAL));
                itemHop(scene, util.vector().centerOf(TERMINAL).add(0, 0.7, 0),
                        util.vector().centerOf(GATEWAY_CORE).add(0, 0.4, 0), new ItemStack(Items.ENDER_PEARL));
                scene.idle(90);
            }
        }
    }

    /** Drive the Bridge's status gauge. The property is mirrored from the BE server-side; Ponder sets it directly. */
    private static void setBridgeStatus(CreateSceneBuilder scene, StorageBridgeBlockEntity.RemoteStatus status) {
        scene.world().modifyBlock(BRIDGE, state -> state.hasProperty(StorageBridgeBlock.STATUS)
                ? state.setValue(StorageBridgeBlock.STATUS, status) : state, false);
    }

    // --- Gateway Flux Battery: one reservoir bolted to the ring, fed by the tank + downward pump stacked
    // above it in the schematic. A 1-wide battery never stacks (height is capped to the base width), so a
    // single block is the smallest valid install; the array scene below covers 2x2x2 and 3x3x3. ---
    private static final BlockPos BATTERY = new BlockPos(2, 1, 0);
    private static final BlockPos BATTERY_PUMP = new BlockPos(2, 2, 0);
    private static final BlockPos BATTERY_TANK = new BlockPos(2, 3, 0);

    public static void gatewayFluxBattery(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("gateway_flux_battery", "Gateway Flux Battery");
        scene.configureBasePlate(0, 0, 4);
        // Ring lit, but the Core deliberately near-empty so the top-up at the end is visible.
        primeGateway(scene, util, CrossDimensionalGatewayCoreBlockEntity.TANK_CAPACITY / 8);
        formBatteryArray(scene, util, BATTERY, 1, 1, 0f);

        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().layersFrom(1), Direction.DOWN);
        scene.idle(20);
        scene.world().setKineticSpeed(util.select().everywhere(), BELT_SPEED);
        scene.world().propagatePipeChange(GATEWAY_PUMP);
        scene.world().propagatePipeChange(BATTERY_PUMP);
        scene.idle(10);

        scene.overlay().showText(80)
                .text("A Gateway Flux Battery stores gateway fuel beside the ring")
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().centerOf(BATTERY));
        scene.idle(90);

        scene.overlay().showText(100)
                .text("Pump Liquid Eye of Ender or Teleport Essence in — the window and gauge fill as it charges")
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().centerOf(BATTERY_PUMP));
        for (int i = 1; i <= 8; i++)
            fillBattery(scene, BATTERY, i / 8f, 10);
        scene.idle(40);

        scene.overlay().showText(100)
                .text("While it holds fuel, the battery keeps the Gateway Core topped up")
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().centerOf(GATEWAY_CORE));
        // Battery drains as the Core's Eye tank climbs — both gauges move, no thrown buckets.
        for (int i = 7; i >= 3; i--) {
            fillBattery(scene, BATTERY, i / 8f, 0);
            scene.world().modifyBlockEntity(GATEWAY_CORE, CrossDimensionalGatewayCoreBlockEntity.class,
                    be -> be.addEye(CrossDimensionalGatewayCoreBlockEntity.TANK_CAPACITY / 6));
            scene.idle(16);
        }
        scene.idle(70);
    }

    // --- Gateway Flux Battery arrays (gateway_flux_battery_array.nbt, 5x5 plate). The full 3x3x3 is baked
    // into the schematic so the scene bounds reach it; the storyboard clears it and grows
    // 1x1x1 -> 2x2x2 -> 3x3x3 from one corner. ---
    private static final BlockPos ARRAY_ORIGIN = new BlockPos(1, 1, 1);

    public static void gatewayFluxBatteryArray(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("gateway_flux_battery_array", "Flux Battery Arrays");
        scene.configureBasePlate(0, 0, 5);

        Selection prism = util.select().fromTo(ARRAY_ORIGIN, ARRAY_ORIGIN.offset(2, 2, 2));
        scene.world().setBlocks(prism, Blocks.AIR.defaultBlockState(), false);
        formBatteryArray(scene, util, ARRAY_ORIGIN, 1, 1, 1f);

        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().layersFrom(1), Direction.DOWN);
        scene.idle(20);

        scene.overlay().showText(90)
                .text("A lone Gateway Flux Battery is a 1x1x1 tank holding one block of fuel")
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(ARRAY_ORIGIN));
        scene.idle(100);

        scene.overlay().showText(100)
                .text("A 2x2 footprint stacks two high: eight blocks merged into one 2x2x2 tank")
                .attachKeyFrame().placeNearTarget()
                .pointAt(util.vector().centerOf(ARRAY_ORIGIN.offset(1, 1, 1)));
        formBatteryArray(scene, util, ARRAY_ORIGIN, 2, 2, 1f);
        scene.idle(110);

        scene.overlay().showText(110)
                .text("A 3x3 footprint stacks three high — the largest array, holding twenty-seven blocks")
                .attachKeyFrame().placeNearTarget()
                .pointAt(util.vector().centerOf(ARRAY_ORIGIN.offset(1, 2, 1)));
        formBatteryArray(scene, util, ARRAY_ORIGIN, 3, 3, 1f);
        scene.idle(120);

        scene.overlay().showText(100)
                .text("Any face of the array accepts fuel, and one face carries the charge gauge")
                .attachKeyFrame().placeNearTarget()
                .pointAt(util.vector().centerOf(ARRAY_ORIGIN.offset(1, 1, 0)));
        for (int i = 1; i <= 6; i++)
            fillBattery(scene, ARRAY_ORIGIN, i / 6f, 14);
        scene.idle(80);
    }

    /**
     * Assemble a width×width×height battery array rooted at {@code origin}. Ponder never runs the server
     * tick, so neither {@link com.simibubi.create.api.connectivity.ConnectivityHandler} nor
     * {@code setWindows}' lid rewrite happens — the per-cell SHAPE/TOP/BOTTOM values and the
     * controller/width/height/tank-size wiring are applied here by hand, mirroring
     * {@code GatewayFluxBatteryBlockEntity.setWindows}.
     *
     * <p>The trailing no-op {@code modifyBlocks} is load-bearing: the connected-texture behaviour resolves
     * neighbours through {@code ConnectivityHandler.isConnected}, which compares each block entity's
     * controller. The section mesh is baked when {@code setBlocks} runs — before the controllers are wired —
     * so without a second pass to re-queue the redraw every cell bakes its own unconnected lid.
     */
    private static void formBatteryArray(CreateSceneBuilder scene, SceneBuildingUtil util,
            BlockPos origin, int width, int height, float fill) {
        BlockState base = CESGRegistration.GATEWAY_FLUX_BATTERY.get().defaultBlockState();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < width; z++) {
                    scene.world().setBlocks(util.select().position(origin.offset(x, y, z)), base
                            .setValue(GatewayFluxBatteryBlock.SHAPE, windowShape(width, x, z))
                            .setValue(GatewayFluxBatteryBlock.BOTTOM, y == 0)
                            .setValue(GatewayFluxBatteryBlock.TOP, y == height - 1), false);
                }
            }
        }
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                for (int z = 0; z < width; z++)
                    scene.world().modifyBlockEntity(origin.offset(x, y, z),
                            GatewayFluxBatteryBlockEntity.class, be -> be.setController(origin));
        // setTankSize before setWidth/setHeight — that is the order Create's connectivity pass uses, and a
        // non-positive size is what trips the singleton guard.
        int blocks = width * width * height;
        scene.world().modifyBlockEntity(origin, GatewayFluxBatteryBlockEntity.class, be -> {
            be.setTankSize(0, blocks);
            be.setWidth(width);
            be.setHeight(height);
        });
        fillBattery(scene, origin, fill, 0);
        scene.world().modifyBlocks(util.select().fromTo(origin, origin.offset(width - 1, height - 1, width - 1)),
                state -> state, false);
    }

    /** Set the array's stored fuel to a fraction of its capacity, reading the real configured capacity. */
    private static void fillBattery(CreateSceneBuilder scene, BlockPos origin, float fraction, int idle) {
        scene.world().modifyBlockEntity(origin, GatewayFluxBatteryBlockEntity.class, be -> {
            FluidTank tank = be.getTankInventory();
            int amount = Math.round(tank.getCapacity() * Math.max(0f, Math.min(1f, fraction)));
            Fluid eye = CESGFluids.LIQUID_EYE_OF_ENDER.getSource();
            tank.setFluid(amount <= 0 ? FluidStack.EMPTY : new FluidStack(eye, amount));
        });
        if (idle > 0)
            scene.idle(idle);
    }

    /** Mirrors the SHAPE rule in {@code GatewayFluxBatteryBlockEntity.setWindows}. */
    private static GatewayFluxBatteryBlock.Shape windowShape(int width, int xOffset, int zOffset) {
        if (width == 1)
            return GatewayFluxBatteryBlock.Shape.WINDOW;
        if (width == 2)
            return xOffset == 0
                    ? zOffset == 0 ? GatewayFluxBatteryBlock.Shape.WINDOW_NW : GatewayFluxBatteryBlock.Shape.WINDOW_SW
                    : zOffset == 0 ? GatewayFluxBatteryBlock.Shape.WINDOW_NE : GatewayFluxBatteryBlock.Shape.WINDOW_SE;
        return Math.abs(xOffset - zOffset) == 1
                ? GatewayFluxBatteryBlock.Shape.WINDOW : GatewayFluxBatteryBlock.Shape.PLAIN;
    }

    // --- Gateway ring: base plate at y=0, ring plane at x=1 (y=1..5), pump/tank above at z=3.
    // The schematic ships COLD — unlit frames, fuel=none conduits, empty Core tanks, no portal — so the
    // storyboard drives the real fuel visuals instead of fighting a captured live gateway. ---
    private static final BlockPos GATEWAY_CORE = new BlockPos(1, 5, 0);
    private static final BlockPos GATEWAY_PUMP = new BlockPos(1, 6, 3);
    private static final BlockPos GATEWAY_INTERIOR_MIN = new BlockPos(1, 2, 1);
    private static final BlockPos GATEWAY_INTERIOR_MAX = new BlockPos(1, 4, 2);
    /** A point inside the portal, for overlays. */
    private static final BlockPos GATEWAY_PORTAL_MID = new BlockPos(1, 3, 1);
    /**
     * The ring as a walk: from the frame the pump injects into, the long way down the z=3 upright, across the
     * bottom and back up the z=0 upright to the Core, then the short top run. Fuel and light are animated in
     * this order so the ring visibly comes alive toward the Core.
     */
    private static final BlockPos[] GATEWAY_RING = {
            new BlockPos(1, 5, 3), new BlockPos(1, 4, 3), new BlockPos(1, 3, 3), new BlockPos(1, 2, 3),
            new BlockPos(1, 1, 3), new BlockPos(1, 1, 2), new BlockPos(1, 1, 1), new BlockPos(1, 1, 0),
            new BlockPos(1, 2, 0), new BlockPos(1, 3, 0), new BlockPos(1, 4, 0),
            new BlockPos(1, 5, 2), new BlockPos(1, 5, 1)
    };

    public static void gatewayCore(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("cross_dimensional_gateway_core", "Cross-Dimensional Gateway Core");
        scene.configureBasePlate(0, 0, 4);
        // The x=2 column holds the Flux Battery scene's fuel feed; this scene is only about the ring.
        scene.world().setBlocks(util.select().fromTo(BATTERY, BATTERY_TANK),
                Blocks.AIR.defaultBlockState(), false);

        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().layersFrom(1), Direction.DOWN);
        scene.idle(20);

        scene.overlay().showText(80)
                .text("Build a vertical Gateway Frame ring with the Core as one of its blocks")
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().centerOf(GATEWAY_CORE));
        scene.idle(90);

        scene.overlay().showText(100)
                .text("Pump Liquid Eye of Ender into any frame to fuel and light the ring")
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().centerOf(GATEWAY_PUMP));
        scene.world().setKineticSpeed(util.select().everywhere(), BELT_SPEED);
        scene.world().propagatePipeChange(GATEWAY_PUMP);
        scene.idle(15);
        // Conduit fuel and the frame's glow travel together, so the ring lights as the fluid reaches it.
        for (BlockPos frame : GATEWAY_RING) {
            scene.world().modifyBlock(frame, state -> lit(eyeFuel(state)), false);
            scene.idle(6);
        }
        scene.idle(20);

        scene.overlay().showText(90)
                .text("The Core's glass eye fills with the fuel it is holding")
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(GATEWAY_CORE));
        // addEye drives the Core's own LIT + FUEL blockstate via updateCoreFuelVisual, so filling the tank
        // in steps is what makes the eye light — the blockstate is never poked directly.
        for (int i = 0; i < 8; i++) {
            scene.world().modifyBlockEntity(GATEWAY_CORE,
                    CrossDimensionalGatewayCoreBlockEntity.class,
                    be -> be.addEye(CrossDimensionalGatewayCoreBlockEntity.TANK_CAPACITY / 8));
            scene.idle(8);
        }
        scene.idle(30);

        scene.overlay().showText(100)
                .text("Powered and bound, the interior opens into a portal")
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().centerOf(GATEWAY_PORTAL_MID));
        scene.world().setBlocks(util.select().fromTo(GATEWAY_INTERIOR_MIN, GATEWAY_INTERIOR_MAX),
                CESGRegistration.GATEWAY_PORTAL.get().defaultBlockState()
                        .setValue(GatewayPortalBlock.AXIS, Direction.Axis.Z), false);
        scene.idle(110);
    }

    /**
     * Bring the cold schematic's ring up to a live gateway before the reveal: conduits carrying Eye, frames
     * lit, Core tank filled to {@code coreEyeMb}, portal open. Used by the scenes whose subject assumes a
     * working gateway rather than teaching how to light one.
     */
    private static void primeGateway(CreateSceneBuilder scene, SceneBuildingUtil util, int coreEyeMb) {
        for (BlockPos frame : GATEWAY_RING)
            scene.world().modifyBlock(frame, state -> lit(eyeFuel(state)), false);
        scene.world().modifyBlockEntity(GATEWAY_CORE, CrossDimensionalGatewayCoreBlockEntity.class,
                be -> be.addEye(coreEyeMb));
        scene.world().setBlocks(util.select().fromTo(GATEWAY_INTERIOR_MIN, GATEWAY_INTERIOR_MAX),
                CESGRegistration.GATEWAY_PORTAL.get().defaultBlockState()
                        .setValue(GatewayPortalBlock.AXIS, Direction.Axis.Z), false);
    }

    private static BlockState lit(BlockState state) {
        return state.hasProperty(BlockStateProperties.LIT) ? state.setValue(BlockStateProperties.LIT, true) : state;
    }

    /** Shows Liquid Eye of Ender in a frame's (or the Core's) internal conduit. */
    private static BlockState eyeFuel(BlockState state) {
        return state.hasProperty(GatewayFrameBlock.FUEL)
                ? state.setValue(GatewayFrameBlock.FUEL, GatewayFrameBlock.FrameFuel.EYE) : state;
    }
}
