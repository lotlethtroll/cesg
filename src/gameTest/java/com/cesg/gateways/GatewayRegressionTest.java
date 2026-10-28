package com.cesg.gateways;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.cesg.CESGConfig;
import com.cesg.gateways.teleport.GatewayPartner;
import com.cesg.init.CESGDataComponents;
import com.cesg.init.CESGRegistration;
import com.cesg.storage.network.StorageNetwork;
import com.cesg.upgrades.EnhancedShulkerBoxes;
import com.cesg.upgrades.EnhancedShulkerBoxBlockEntity;
import com.cesg.upgrades.EnhancedShulkerContents;
import com.cesg.upgrades.EnhancedShulkerContents.StoredSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("cesg")
@PrefixGameTestTemplate(false)
public class GatewayRegressionTest {
    @GameTest(template = "empty")
    public void shulkerCompactLayoutPreservesSlots(GameTestHelper helper) {
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());
        for (int tier = 1; tier <= 4; tier++) {
            ItemStack item = new ItemStack(EnhancedShulkerBoxes.DEFAULT.get());
            item.set(CESGDataComponents.ENHANCED_SHULKER.get(), EnhancedShulkerContents.forTier(tier));
            var menu = new com.cesg.upgrades.EnhancedShulkerMenu(1, player.getInventory(), item);
            var original = List.copyOf(menu.slots);
            int hotbarStart = original.size() - 9;
            for (int offset : new int[] {-4, 0, -4, 0}) {
                menu.positionHotbar(offset);
                for (int i = 0; i < original.size(); i++) {
                    var slot = menu.getSlot(i);
                    var before = original.get(i);
                    helper.assertTrue(slot.index == i && slot.container == before.container
                            && slot.getContainerSlot() == before.getContainerSlot(), "Shulker slot mapping changed");
                    helper.assertTrue(slot.x == before.x && slot.y == before.y + (i >= hotbarStart ? offset : 0),
                            "Shulker layout moved storage/upgrades or accumulated offsets");
                    if (i < hotbarStart) helper.assertTrue(slot == before, "Shulker layout replaced a storage/upgrade slot");
                }
            }
            menu.removed(player);
        }
        helper.succeed();
    }

    @net.minecraft.gametest.framework.GameTest(template = "empty")
    public void bridgeCompactLayoutPreservesSlots(net.minecraft.gametest.framework.GameTestHelper helper) {
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());
        var menu = new StorageBridgeMenu(1, player.getInventory(), helper.absolutePos(new BlockPos(1, 1, 1)));
        var original = java.util.stream.IntStream.range(0, 54).mapToObj(menu::getSlot).toList();
        for (int offset : new int[] {-4, 0, -4, 0}) {
            menu.positionHotbar(offset);
            for (int i = 0; i < 54; i++) {
                var slot = menu.getSlot(i);
                var before = original.get(i);
                helper.assertTrue(slot.index == i && slot.container == before.container
                        && slot.getContainerSlot() == before.getContainerSlot(), "Bridge layout changed slot mapping");
                helper.assertTrue(slot.x == before.x && slot.y == before.y + (i >= 45 ? offset : 0),
                        "Bridge layout moved filters/inventory or accumulated offsets");
                if (i < 45) helper.assertTrue(slot == before, "Bridge layout replaced a filter or inventory slot");
            }
        }
        helper.succeed();
    }

    private BlockPos local;
    private BlockPos remote;

    private void prepare(GameTestHelper helper) {
        local = helper.absolutePos(new BlockPos(1, 2, 1));
        remote = helper.absolutePos(new BlockPos(7, 2, 1));
    }

    @GameTest(template = "empty")
    public void insufficientFuelNeverExtractsFromAFilteredSource(GameTestHelper helper) {
        prepare(helper);
        ServerLevel level = helper.getLevel();
        StorageBridgeBlockEntity bridge = linkedBridges(level);
        box(level, remote.east(3), List.of(new StoredSlot(0, new ItemStack(Items.DIAMOND, 20))), true);
        CrossDimensionalGatewayCoreBlockEntity core = coreAt(level, local);
        core.fillEssence(300, false);
        withCost("bridge", "transferCostMb", 400, () -> {
            assertTrue(bridge.terminalWithdrawRemote(new ItemStack(Items.DIAMOND), 20, null).isEmpty());
            assertEquals(20, StorageNetwork.aggregate(StorageNetwork.scan(level, remote.east(3)))
                    .values().intStream().sum());
            assertEquals(300, core.getEssenceMb());
            core.fillEssence(3700, false);
            assertEquals(20, bridge.terminalWithdrawRemote(new ItemStack(Items.DIAMOND), 20, null).getCount());
            assertEquals(3600, core.getEssenceMb());
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void stagingOnlyExtractsWhatTheBufferCanKeep(GameTestHelper helper) {
        prepare(helper);
        ServerLevel level = helper.getLevel();
        linkedBridges(level);
        box(level, remote.east(3), List.of(new StoredSlot(0, new ItemStack(Items.DIAMOND, 20))), true);
        ItemStackHandler buffer = new ItemStackHandler(9);
        for (int i = 0; i < 9; i++) buffer.setStackInSlot(i, new ItemStack(Items.GOLD_INGOT, 64));
        buffer.setStackInSlot(0, new ItemStack(Items.DIAMOND, 63));
        int moved = invokeStatic("fillBufferFromNetwork",
                new Class<?>[]{Level.class, BlockPos.class, ItemStackHandler.class, int.class, Predicate.class},
                level, remote.east(), buffer, 4096, (Predicate<ItemStack>) sample -> sample.is(Items.DIAMOND));
        assertEquals(1, moved);
        assertEquals(64, buffer.getStackInSlot(0).getCount());
        assertEquals(19, StorageNetwork.aggregate(StorageNetwork.scan(level, remote.east(3)))
                .values().intStream().sum());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void idleBridgeAndFullTerminalDepositDoNotSpendFuel(GameTestHelper helper) {
        prepare(helper);
        ServerLevel level = helper.getLevel();
        StorageBridgeBlockEntity bridge = linkedBridges(level);
        List<StoredSlot> full = new ArrayList<>();
        for (int i = 0; i < 54; i++) full.add(new StoredSlot(i, new ItemStack(Items.GOLD_INGOT, 64)));
        box(level, remote.east(3), full, false);
        CrossDimensionalGatewayCoreBlockEntity core = coreAt(level, local);
        core.fillEssence(4000, false);
        bridge.setPushEnabled(true); // empty whitelist: no matching work
        withCost("bridge", "transferCostMb", 100, () -> {
            invoke(bridge, "flushPassive");
            assertEquals(4000, core.getEssenceMb());
            ItemStack remainder = bridge.terminalDepositRemote(new ItemStack(Items.DIAMOND, 20), null);
            assertEquals(20, remainder.getCount());
            assertEquals(4000, core.getEssenceMb());
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void routedPortUsesDestinationFuelEvenWhenTheActiveChannelIsDry(GameTestHelper helper) {
        prepare(helper);
        ServerLevel level = helper.getLevel();
        ServerLevel nether = level.getServer().getLevel(Level.NETHER);
        CrossDimensionalGatewayCoreBlockEntity core = core(level, local);
        core(level, remote);
        BlockPos netherDestination = new BlockPos(remote.getX(), 80, remote.getZ());
        core(nether, netherDestination);
        GatewayPortBlockEntity port = port(level, local.east());
        GatewayPortBlockEntity localTarget = port(level, remote.east());
        GatewayPortBlockEntity remoteTarget = port(nether, netherDestination.east());
        core.setBinding(0, new GatewayPartner(Level.OVERWORLD, remote, true));
        core.setBinding(1, new GatewayPartner(Level.NETHER, netherDestination, true));
        core.getOrCreateChannelFilter(0).items().setStackInSlot(0, new ItemStack(Items.GOLD_INGOT));
        core.getOrCreateChannelFilter(1).items().setStackInSlot(0, new ItemStack(Items.DIAMOND));
        core.setRouteMode(true);
        core.fillEye(4000, false);
        port.sendItems.setStackInSlot(0, new ItemStack(Items.DIAMOND, 20));
        port.sendItems.setStackInSlot(1, new ItemStack(Items.GOLD_INGOT, 20));
        withCost("gateway", "portTransferCostMb", 100, () -> {
            invoke(port, "flush");
            assertEquals(3900, core.getEyeMb());
            assertEquals(0, core.getEssenceMb());
            assertEquals(20, remoteTarget.receiveItems.getStackInSlot(0).getCount());
            assertTrue(localTarget.receiveItems.getStackInSlot(0).isEmpty());
            assertTrue(port.sendItems.getStackInSlot(0).isEmpty());
            assertEquals(20, port.sendItems.getStackInSlot(1).getCount());
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void aFullPortDoesNotSpendFuelAndBuffersSurviveSerialization(GameTestHelper helper) {
        prepare(helper);
        ServerLevel level = helper.getLevel();
        CrossDimensionalGatewayCoreBlockEntity core = core(level, local);
        core(level, remote);
        core.setPartner(new GatewayPartner(Level.OVERWORLD, remote, true));
        GatewayPortBlockEntity source = port(level, local.east());
        GatewayPortBlockEntity target = port(level, remote.east());
        for (int i = 0; i < 9; i++) target.receiveItems.setStackInSlot(i, new ItemStack(Items.GOLD_INGOT, 64));
        source.sendItems.setStackInSlot(0, new ItemStack(Items.DIAMOND, 20));
        source.sendTank.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        target.receiveTank.fill(new FluidStack(Fluids.LAVA, 4000), IFluidHandler.FluidAction.EXECUTE);
        core.fillEssence(4000, false);
        withCost("gateway", "portTransferCostMb", 100, () -> {
            invoke(source, "flush");
            assertEquals(4000, core.getEssenceMb());
            GatewayPortBlockEntity restored = new GatewayPortBlockEntity(local.east(), source.getBlockState());
            restored.loadAdditional(source.getUpdateTag(level.registryAccess()), level.registryAccess());
            assertEquals(20, restored.sendItems.getStackInSlot(0).getCount());
            assertEquals(1000, restored.sendTank.getFluidAmount());
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void routedBridgeUsesDestinationFuel(GameTestHelper helper) {
        prepare(helper);
        ServerLevel level = helper.getLevel();
        ServerLevel nether = level.getServer().getLevel(Level.NETHER);
        StorageBridgeBlockEntity bridge = linkedBridges(level);
        box(level, remote.east(3), List.of(), false);
        BlockPos destination = new BlockPos(remote.getX(), 80, remote.getZ());
        core(nether, destination);
        place(nether, destination.east(), CESGRegistration.STORAGE_BRIDGE.get());
        place(nether, destination.east(2), CESGRegistration.STORAGE_NETWORK_CONTROLLER.get());
        box(nether, destination.east(3), List.of(), false);
        CrossDimensionalGatewayCoreBlockEntity core = coreAt(level, local);
        core.setBinding(1, new GatewayPartner(Level.NETHER, destination, true));
        core.getOrCreateChannelFilter(0).items().setStackInSlot(0, new ItemStack(Items.GOLD_INGOT));
        core.getOrCreateChannelFilter(1).items().setStackInSlot(0, new ItemStack(Items.DIAMOND));
        core.setRouteMode(true);
        core.fillEye(4000, false);
        bridge.outBuffer.setStackInSlot(0, new ItemStack(Items.DIAMOND, 20));
        bridge.outBuffer.setStackInSlot(1, new ItemStack(Items.GOLD_INGOT, 20));
        withCost("bridge", "transferCostMb", 100, () -> {
            invoke(bridge, "flushPassive");
            assertEquals(3900, core.getEyeMb());
            assertEquals(0, core.getEssenceMb());
            assertTrue(bridge.outBuffer.getStackInSlot(0).isEmpty());
            assertEquals(20, bridge.outBuffer.getStackInSlot(1).getCount());
            assertEquals(20, StorageNetwork.aggregate(StorageNetwork.scan(nether, destination.east(3)))
                    .values().intStream().sum());
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void zeroCostPortPreservesTheDefaultFreeTransferBehavior(GameTestHelper helper) {
        prepare(helper);
        ServerLevel level = helper.getLevel();
        CrossDimensionalGatewayCoreBlockEntity core = core(level, local);
        core(level, remote);
        core.setPartner(new GatewayPartner(Level.OVERWORLD, remote, true));
        GatewayPortBlockEntity source = port(level, local.east());
        GatewayPortBlockEntity target = port(level, remote.east());
        place(level, local.north(), CESGRegistration.GATEWAY_FLUX_BATTERY.get());
        core.fillEssence(1000, false);
        source.sendItems.setStackInSlot(0, new ItemStack(Items.DIAMOND, 20));
        withCost("gateway", "portTransferCostMb", 0, () -> {
            invoke(source, "flush");
            assertEquals(1000, core.getEssenceMb());
            assertEquals(20, target.receiveItems.getStackInSlot(0).getCount());
            assertTrue(source.sendItems.getStackInSlot(0).isEmpty());
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void unloadedPartnerHoldsCargoWithoutSpendingFuel(GameTestHelper helper) {
        prepare(helper);
        ServerLevel level = helper.getLevel();
        CrossDimensionalGatewayCoreBlockEntity core = core(level, local);
        BlockPos unloaded = remote.offset(4096, 0, 4096);
        core.setPartner(new GatewayPartner(Level.OVERWORLD, unloaded, true));
        GatewayPortBlockEntity port = port(level, local.east());
        port.sendItems.setStackInSlot(0, new ItemStack(Items.DIAMOND, 20));
        core.fillEssence(4000, false);
        withCost("gateway", "portTransferCostMb", 100, () -> {
            invoke(port, "flush");
            assertEquals(4000, core.getEssenceMb());
            assertEquals(20, port.sendItems.getStackInSlot(0).getCount());
            assertTrue(!level.isLoaded(unloaded));
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void reserveBlocksAutomationAndBufferedBridgeItemsPersist(GameTestHelper helper) {
        prepare(helper);
        ServerLevel level = helper.getLevel();
        StorageBridgeBlockEntity bridge = linkedBridges(level);
        CrossDimensionalGatewayCoreBlockEntity core = coreAt(level, local);
        place(level, local.north(), CESGRegistration.GATEWAY_FLUX_BATTERY.get());
        core.fillEssence(1000, false);
        AutomationFuelBudget fuel = core.automationFuelBudget(100);
        assertEquals(0, fuel.transfer(false, () -> { throw new AssertionError("Travel reserve was spent"); }));
        assertEquals(1000, core.getEssenceMb());
        bridge.outBuffer.setStackInSlot(0, new ItemStack(Items.DIAMOND, 16));
        bridge.inBuffer.setStackInSlot(0, new ItemStack(Items.EMERALD, 3));
        StorageBridgeBlockEntity restored = new StorageBridgeBlockEntity(local.east(), bridge.getBlockState());
        restored.loadAdditional(bridge.getUpdateTag(level.registryAccess()), level.registryAccess());
        assertEquals(19, restored.bufferedContents().stream().mapToInt(ItemStack::getCount).sum());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void partnerWithoutControllerStaysFaulted(GameTestHelper helper) {
        prepare(helper);
        ServerLevel level = helper.getLevel();
        StorageBridgeBlockEntity bridge = linkedBridges(level);
        level.removeBlock(remote.east(2), false);
        bridge.remoteSnapshot();
        assertTrue(bridge.remoteStatus() == StorageBridgeBlockEntity.RemoteStatus.FAULT);
        StorageBridgeBlockEntity.serverTick(level, local.east(), bridge.getBlockState(), bridge);
        assertTrue(bridge.getBlockState().getValue(StorageBridgeBlock.STATUS)
                == StorageBridgeBlockEntity.RemoteStatus.FAULT);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void branchingNetworkVisitsTheFullScanLimit(GameTestHelper helper) {
        prepare(helper);
        ServerLevel level = helper.getLevel();
        for (int x = 0; x < 9; x++)
            for (int y = 0; y < 4; y++)
                for (int z = 0; z < 8; z++)
                    place(level, local.offset(x, y, z), CESGRegistration.STORAGE_NETWORK_CONTROLLER.get());
        assertEquals(256, StorageNetwork.scan(level, local).memberCount());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void removedControllerImmediatelyDisablesCachedNetwork(GameTestHelper helper) {
        prepare(helper);
        ServerLevel level = helper.getLevel();
        place(level, local, CESGRegistration.STORAGE_NETWORK_CONTROLLER.get());
        box(level, local.east(), List.of(new StoredSlot(0, new ItemStack(Items.DIAMOND, 20))), false);
        assertTrue(StorageNetwork.scan(level, local.east()).operational());
        level.removeBlock(local, false);
        assertTrue(!StorageNetwork.scan(level, local.east()).operational());
        assertTrue(StorageNetwork.extract(level, local.east(), new ItemStack(Items.DIAMOND), 20).isEmpty());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void networkScanDoesNotLoadAnUnloadedAnchor(GameTestHelper helper) {
        prepare(helper);
        ServerLevel level = helper.getLevel();
        BlockPos unloaded = local.offset(4096, 0, 4096);
        assertTrue(!level.isLoaded(unloaded));
        assertEquals(0, StorageNetwork.scan(level, unloaded).memberCount());
        assertTrue(!level.isLoaded(unloaded));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void diagnosticsExplainFailuresAndClearOnRecovery(GameTestHelper helper) {
        prepare(helper);
        ServerLevel level = helper.getLevel();
        StorageBridgeBlockEntity bridge = linkedBridges(level);
        var core = coreAt(level, local);
        box(level, local.east(3), List.of(new StoredSlot(0, new ItemStack(Items.DIAMOND, 20))), false);
        List<StoredSlot> full = new ArrayList<>();
        for (int i = 0; i < 54; i++) full.add(new StoredSlot(i, new ItemStack(Items.GOLD_INGOT, 64)));
        box(level, remote.east(3), full, false);
        assertDiagnostic(bridge, true, TransferDiagnostic.DISABLED);
        bridge.setPushEnabled(true);
        core.fillEssence(4000, false);
        assertDiagnostic(bridge, true, TransferDiagnostic.IDLE);
        bridge.getSendFilter().setStackInSlot(0, new ItemStack(Items.DIAMOND));
        withCost("bridge", "transferCostMb", 100, () -> {
            assertDiagnostic(bridge, true, TransferDiagnostic.DESTINATION_FULL);
            box(level, remote.east(3), List.of(), false);
            assertDiagnostic(bridge, true, TransferDiagnostic.READY);
            core.setRouteMode(true);
            assertDiagnostic(bridge, true, TransferDiagnostic.NO_ROUTE);
            core.getOrCreateChannelFilter(0).items().setStackInSlot(0, new ItemStack(Items.DIAMOND));
            assertDiagnostic(bridge, true, TransferDiagnostic.READY);
            core.setBinding(0, new GatewayPartner(level.dimension(), local.offset(4096, 0, 4096), true));
            assertDiagnostic(bridge, true, TransferDiagnostic.PARTNER_OFFLINE);
            assertTrue(!level.isLoaded(local.offset(4096, 0, 4096)));
            core.setBinding(0, new GatewayPartner(level.dimension(), remote, true));
            assertDiagnostic(bridge, true, TransferDiagnostic.READY);
            core.setSpeed(0);
            assertDiagnostic(bridge, true, TransferDiagnostic.GATEWAY_INACTIVE);
            core.setSpeed(16);
            assertDiagnostic(bridge, true, TransferDiagnostic.READY);
            assertEquals(4000, core.getEssenceMb());
            assertEquals(20, StorageNetwork.aggregate(StorageNetwork.scan(level, local.east())).values().intStream().sum());
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void pullDiagnosticsRespectLockReserveAndFuel(GameTestHelper helper) {
        prepare(helper);
        ServerLevel level = helper.getLevel();
        var bridge = linkedBridges(level);
        var core = coreAt(level, local);
        box(level, remote.east(3), List.of(new StoredSlot(0, new ItemStack(Items.DIAMOND, 20))), false);
        box(level, local.east(3), List.of(), false);
        bridge.setPullEnabled(true);
        bridge.getPullFilter().setStackInSlot(0, new ItemStack(Items.DIAMOND));
        var partner = (StorageBridgeBlockEntity) level.getBlockEntity(remote.east());
        partner.toggleLocked();
        assertDiagnostic(bridge, false, TransferDiagnostic.PARTNER_LOCKED);
        partner.toggleLocked();
        assertDiagnostic(bridge, false, TransferDiagnostic.INSUFFICIENT_FUEL);
        core.fillEssence(4000, false);
        assertDiagnostic(bridge, false, TransferDiagnostic.READY);
        place(level, local.north(), CESGRegistration.GATEWAY_FLUX_BATTERY.get());
        withCost("bridge", "transferCostMb", 100, () -> {
            withCost("battery", "reserveFloorMb", 4000, () -> {
                assertDiagnostic(bridge, false, TransferDiagnostic.PROTECTED_RESERVE);
                assertTrue(!core.tryConsumeAutomationFuel(100));
                assertEquals(4000, core.getEssenceMb());
            });
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void scanDiagnosticDistinguishesExactLimitFromTruncation(GameTestHelper helper) {
        BlockPos start = helper.absolutePos(new BlockPos(1000, 3, 1000));
        ServerLevel level = helper.getLevel();
        place(level, start.east(StorageNetwork.SCAN_LIMIT), net.minecraft.world.level.block.Blocks.AIR);
        for (int i = 0; i < StorageNetwork.SCAN_LIMIT; i++)
            place(level, start.east(i), CESGRegistration.STORAGE_NETWORK_CONTROLLER.get());
        assertTrue(!StorageNetwork.scan(level, start).limited());
        place(level, start.east(StorageNetwork.SCAN_LIMIT), CESGRegistration.STORAGE_NETWORK_CONTROLLER.get());
        // Use uncached anchors to inspect each topology immediately, before distant fixture chunks unload.
        assertTrue(StorageNetwork.scan(level, start.east()).limited());
        level.removeBlock(start.east(StorageNetwork.SCAN_LIMIT), false);
        assertTrue(!StorageNetwork.scan(level, start.east(2)).limited());
        helper.succeed();
    }

    private static void assertDiagnostic(StorageBridgeBlockEntity bridge, boolean push, TransferDiagnostic expected) {
        TransferDiagnostic actual = bridge.transferDiagnostic(push);
        if (actual != expected) throw new AssertionError("Expected " + expected + " but got " + actual);
    }

    @GameTest(template = "empty")
    public void fluidRoutingIsOptInOrderedAndSurvivesReload(GameTestHelper helper) {
        prepare(helper);
        ServerLevel level = helper.getLevel();
        var core = core(level, local);
        core.setBinding(0, new GatewayPartner(level.dimension(), remote, true));
        core.setBinding(1, new GatewayPartner(level.dimension(), remote.north(3), true));
        core.setActiveChannel(1);
        core.setRouteMode(true);
        var water = new FluidStack(Fluids.WATER, 250);
        core.getOrCreateChannelFilter(0).fluidItems().setStackInSlot(0, new ItemStack(Items.WATER_BUCKET));
        assertEquals(1, core.routeFluidChannel(water)); // old item routing must not reroute fluids
        core.setFluidRouteMode(true);
        assertEquals(0, core.routeFluidChannel(water));
        assertEquals(-1, core.routeFluidChannel(new FluidStack(Fluids.LAVA, 1000)));
        core.getOrCreateChannelFilter(1).setFluidBlacklist(true);
        assertEquals(1, core.routeFluidChannel(new FluidStack(Fluids.LAVA, 1000)));
        assertEquals(0, core.routeFluidChannel(water)); // catch-all later must not steal a specific route
        var saved = core.getUpdateTag(level.registryAccess());
        var restored = new CrossDimensionalGatewayCoreBlockEntity(local, core.getBlockState());
        restored.loadWithComponents(saved, level.registryAccess());
        assertTrue(restored.isFluidRouteMode());
        assertEquals(0, restored.routeFluidChannel(water));
        assertEquals(1, restored.routeFluidChannel(new FluidStack(Fluids.LAVA, 1)));
        saved.remove("FluidRouteMode");
        for (var raw : saved.getList("ChannelFilters", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            ((net.minecraft.nbt.CompoundTag) raw).remove("FluidItems");
            ((net.minecraft.nbt.CompoundTag) raw).remove("FluidBlacklist");
        }
        restored.loadWithComponents(saved, level.registryAccess());
        assertTrue(!restored.isFluidRouteMode());
        assertEquals(1, restored.routeFluidChannel(water));
        assertEquals(9, restored.getOrCreateChannelFilter(0).fluidItems().getSlots());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void fluidFilterMenuKeepsGhostSelectionsSeparateAndDoesNotConsumeContainers(GameTestHelper helper) {
        prepare(helper);
        ServerLevel level = helper.getLevel();
        var core = core(level, local);
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        var menu = new GatewayFilterMenu(1, player.getInventory(), local, 0);
        menu.setCarried(new ItemStack(Items.DIAMOND, 4));
        menu.clicked(0, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
        menu.clickMenuButton(player, GatewayFilterMenu.BTN_FLUIDS);
        menu.setCarried(new ItemStack(Items.WATER_BUCKET));
        menu.clicked(9, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
        assertTrue(menu.getCarried().is(Items.WATER_BUCKET));
        assertEquals(1, menu.getCarried().getCount());
        var filter = core.getOrCreateChannelFilter(0);
        assertTrue(filter.items().getStackInSlot(0).is(Items.DIAMOND));
        assertTrue(filter.acceptsFluid(new FluidStack(Fluids.WATER, 1)));
        assertTrue(!filter.acceptsFluid(new FluidStack(Fluids.LAVA, 1000)));
        var componentWater = new FluidStack(Fluids.WATER, 1000);
        var fluidData = new net.minecraft.nbt.CompoundTag();
        fluidData.putString("variant", "test");
        componentWater.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(fluidData));
        assertTrue(!filter.acceptsFluid(componentWater));
        menu.setCarried(new ItemStack(Items.BUCKET));
        menu.clicked(9, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
        assertTrue(filter.fluidItems().getStackInSlot(0).is(Items.WATER_BUCKET));
        menu.clickMenuButton(player, GatewayFilterMenu.BTN_BLACKLIST);
        assertTrue(filter.isFluidBlacklist() && !filter.isBlacklist());
        assertTrue(!filter.acceptsFluid(new FluidStack(Fluids.WATER, 1000)));
        menu.setCarried(ItemStack.EMPTY);
        menu.clicked(9, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
        assertTrue(filter.acceptsFluid(new FluidStack(Fluids.WATER, 1000)));
        menu.clickMenuButton(player, GatewayFilterMenu.BTN_FLUIDS);
        assertTrue(!menu.isBlacklist());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void routedFluidAndItemsShareOneDestinationFuelCharge(GameTestHelper helper) {
        prepare(helper);
        ServerLevel level = helper.getLevel();
        ServerLevel nether = level.getServer().getLevel(Level.NETHER);
        var core = core(level, local);
        core(level, remote);
        BlockPos other = new BlockPos(remote.getX(), 80, remote.getZ());
        core(nether, other);
        var source = port(level, local.east());
        var activeTarget = port(level, remote.east());
        var fluidTarget = port(nether, other.east());
        core.setBinding(0, new GatewayPartner(level.dimension(), remote, true));
        core.setBinding(1, new GatewayPartner(Level.NETHER, other, true));
        core.setFluidRouteMode(true);
        core.setRouteMode(true);
        core.getOrCreateChannelFilter(0).items().setStackInSlot(0, new ItemStack(Items.GOLD_INGOT));
        core.getOrCreateChannelFilter(1).items().setStackInSlot(0, new ItemStack(Items.DIAMOND));
        core.getOrCreateChannelFilter(1).fluidItems().setStackInSlot(0, new ItemStack(Items.WATER_BUCKET));
        source.sendItems.setStackInSlot(0, new ItemStack(Items.DIAMOND, 20));
        source.sendItems.setStackInSlot(1, new ItemStack(Items.GOLD_INGOT, 20));
        source.sendTank.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        core.fillEye(4000, false);
        core.fillEssence(4000, false);
        withCost("gateway", "portTransferCostMb", 100, () -> {
            invoke(source, "flush");
            assertEquals(3900, core.getEyeMb());
            assertEquals(3900, core.getEssenceMb());
            assertEquals(1000, fluidTarget.receiveTank.getFluidAmount());
            assertEquals(20, fluidTarget.receiveItems.getStackInSlot(0).getCount());
            assertEquals(20, activeTarget.receiveItems.getStackInSlot(0).getCount());
            assertEquals(0, activeTarget.receiveTank.getFluidAmount());
            assertTrue(source.sendTank.isEmpty());
            // Item routing alone preserves the legacy active-channel fluid path.
            core.setFluidRouteMode(false);
            source.sendTank.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
            invoke(source, "flush");
            assertEquals(1000, activeTarget.receiveTank.getFluidAmount());
            assertEquals(3800, core.getEssenceMb());
            assertEquals(3900, core.getEyeMb());
            // A dry battery protects the destination's fuel type without changing buffered cargo.
            core.setFluidRouteMode(true);
            source.sendTank.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
            place(level, local.north(), CESGRegistration.GATEWAY_FLUX_BATTERY.get());
            withCost("battery", "reserveFloorMb", 4000, () -> {
                invoke(source, "flush");
                assertEquals(1000, source.sendTank.getFluidAmount());
                assertEquals(1000, fluidTarget.receiveTank.getFluidAmount());
                assertEquals(3900, core.getEyeMb());
            });
            withCost("battery", "reserveFloorMb", 0, () -> {
                invoke(source, "flush");
                assertTrue(source.sendTank.isEmpty());
                assertEquals(2000, fluidTarget.receiveTank.getFluidAmount());
                assertEquals(3800, core.getEyeMb());
            });
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void blockedRoutedFluidsStayBufferedWithoutFuelLossOrFallback(GameTestHelper helper) {
        prepare(helper);
        ServerLevel level = helper.getLevel();
        var core = core(level, local);
        core(level, remote);
        core(level, remote.north(3));
        var source = port(level, local.east());
        var target = port(level, remote.east());
        var fallback = port(level, remote.north(3).east());
        core.setBinding(0, new GatewayPartner(level.dimension(), remote, true));
        core.setBinding(1, new GatewayPartner(level.dimension(), remote.north(3), true));
        core.setFluidRouteMode(true);
        core.getOrCreateChannelFilter(0).fluidItems().setStackInSlot(0, new ItemStack(Items.WATER_BUCKET));
        core.getOrCreateChannelFilter(1).setFluidBlacklist(true);
        core.fillEssence(4000, false);
        source.sendTank.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        target.receiveTank.fill(new FluidStack(Fluids.LAVA, 4000), IFluidHandler.FluidAction.EXECUTE);
        withCost("gateway", "portTransferCostMb", 100, () -> {
            invoke(source, "flush");
            assertEquals(1000, source.sendTank.getFluidAmount());
            assertEquals(0, fallback.receiveTank.getFluidAmount());
            assertEquals(4000, core.getEssenceMb());
            target.receiveTank.drain(4000, IFluidHandler.FluidAction.EXECUTE);
            target.receiveTank.fill(new FluidStack(Fluids.WATER, 4000), IFluidHandler.FluidAction.EXECUTE);
            invoke(source, "flush");
            assertEquals(1000, source.sendTank.getFluidAmount());
            assertEquals(4000, core.getEssenceMb());
            BlockPos unloaded = local.offset(4096, 0, 4096);
            core.setBinding(0, new GatewayPartner(level.dimension(), unloaded, true));
            invoke(source, "flush");
            assertTrue(!level.isLoaded(unloaded));
            assertEquals(1000, source.sendTank.getFluidAmount());
            assertEquals(0, fallback.receiveTank.getFluidAmount());
            assertEquals(4000, core.getEssenceMb());
            var saved = source.getUpdateTag(level.registryAccess());
            var restored = new GatewayPortBlockEntity(local.east(), source.getBlockState());
            restored.loadAdditional(saved, level.registryAccess());
            assertEquals(1000, restored.sendTank.getFluidAmount());
            core.setBinding(0, new GatewayPartner(level.dimension(), remote, true));
            target.receiveTank.drain(4000, IFluidHandler.FluidAction.EXECUTE);
            invoke(source, "flush");
            assertTrue(source.sendTank.isEmpty());
            assertEquals(1000, target.receiveTank.getFluidAmount());
            assertEquals(3900, core.getEssenceMb());
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void singletonBatteryPickupTransfersFuelExactlyOnceInSurvivalAndCreative(GameTestHelper helper) {
        prepare(helper);
        ServerLevel level = helper.getLevel();
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        var batteryBlock = CESGRegistration.GATEWAY_FLUX_BATTERY.get();
        var batteryItem = (GatewayFluxBatteryItem) batteryBlock.asItem();
        for (boolean creative : new boolean[] {false, true}) {
            player.setGameMode(creative ? net.minecraft.world.level.GameType.CREATIVE : net.minecraft.world.level.GameType.SURVIVAL);
            for (net.minecraft.world.level.material.Fluid fluid : List.<net.minecraft.world.level.material.Fluid>of(com.cesg.init.CESGFluids.TELEPORT_ESSENCE.getSource(),
                    com.cesg.init.CESGFluids.LIQUID_EYE_OF_ENDER.getSource())) {
                for (int amount : new int[] {0, 1234, GatewayFluxBatteryBlockEntity.capacityPerBlock()}) {
                    player.getInventory().clearContent();
                    place(level, local, batteryBlock);
                    var battery = (GatewayFluxBatteryBlockEntity) level.getBlockEntity(local);
                    battery.getTankInventory().fill(new FluidStack(fluid, amount), IFluidHandler.FluidAction.EXECUTE);
                    for (int cycle = 0; cycle < 3; cycle++) {
                        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(local),
                                net.minecraft.core.Direction.UP, local, false);
                        var context = new net.minecraft.world.item.context.UseOnContext(player,
                                net.minecraft.world.InteractionHand.MAIN_HAND, hit);
                        batteryBlock.onSneakWrenched(level.getBlockState(local), context);
                        assertTrue(level.getBlockEntity(local) == null);
                        int totalFuel = 0;
                        ItemStack pickup = ItemStack.EMPTY;
                        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                            ItemStack held = player.getInventory().getItem(slot);
                            totalFuel += GatewayFluxBatteryItem.storedFuel(held, level.registryAccess()).getAmount() * held.getCount();
                            if (held.is(batteryItem)) pickup = held;
                        }
                        assertEquals(amount, totalFuel);
                        assertEquals(1, pickup.getCount());
                        pickup = ItemStack.parse(level.registryAccess(), pickup.save(level.registryAccess())).orElseThrow();
                        assertEquals(amount, GatewayFluxBatteryItem.storedFuel(pickup, level.registryAccess()).getAmount());
                        // Exercise the actual item placement path into air; support is not required.
                        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, pickup.copy());
                        player.getInventory().clearContent();
                        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, pickup.copy());
                        var placeContext = new net.minecraft.world.item.context.BlockPlaceContext(player,
                                net.minecraft.world.InteractionHand.MAIN_HAND, player.getMainHandItem(), hit);
                        assertTrue(batteryItem.place(placeContext).consumesAction());
                        assertEquals(amount, ((GatewayFluxBatteryBlockEntity) level.getBlockEntity(local)).storedFluid().getAmount());
                        if (amount > 0 || !creative) assertTrue(player.getMainHandItem().isEmpty());
                        // Empty creative items retain vanilla copying behavior; remove the template before pickup.
                        player.getInventory().clearContent();
                    }
                    var restored = new GatewayFluxBatteryBlockEntity(local, level.getBlockState(local));
                    restored.loadAdditional(((GatewayFluxBatteryBlockEntity) level.getBlockEntity(local))
                            .getUpdateTag(level.registryAccess()), level.registryAccess());
                    assertEquals(amount, restored.storedFluid().getAmount());
                    level.removeBlock(local, false);
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void arrayPickupAndChargedPlacementBesideBatteriesAreRefused(GameTestHelper helper) {
        prepare(helper);
        ServerLevel level = helper.getLevel();
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.getInventory().clearContent();
        var block = CESGRegistration.GATEWAY_FLUX_BATTERY.get();
        place(level, local, block);
        var battery = (GatewayFluxBatteryBlockEntity) level.getBlockEntity(local);
        battery.getTankInventory().fill(new FluidStack((net.minecraft.world.level.material.Fluid) com.cesg.init.CESGFluids.TELEPORT_ESSENCE.getSource(), 1000),
                IFluidHandler.FluidAction.EXECUTE);
        ItemStack charged = GatewayFluxBatteryItem.pickupStack(battery);
        place(level, local.above(), block);
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(local),
                net.minecraft.core.Direction.UP, local, false);
        block.onSneakWrenched(level.getBlockState(local), new net.minecraft.world.item.context.UseOnContext(player,
                net.minecraft.world.InteractionHand.MAIN_HAND, hit));
        assertTrue(level.getBlockEntity(local) != null && level.getBlockEntity(local.above()) != null);
        assertTrue(player.getInventory().isEmpty());
        BlockPos adjacent = local.east();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, charged);
        var placeHit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(adjacent),
                net.minecraft.core.Direction.UP, adjacent, false);
        var ctx = new net.minecraft.world.item.context.BlockPlaceContext(player, net.minecraft.world.InteractionHand.MAIN_HAND,
                charged, placeHit);
        assertTrue(((GatewayFluxBatteryItem) block.asItem()).place(ctx) == net.minecraft.world.InteractionResult.FAIL);
        assertTrue(level.getBlockEntity(adjacent) == null);
        assertEquals(1000, GatewayFluxBatteryItem.storedFuel(player.getMainHandItem(), level.registryAccess()).getAmount());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void fullInventoryBatteryPickupDropsOneChargedItem(GameTestHelper helper) {
        prepare(helper);
        ServerLevel level = helper.getLevel();
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(local.getX() + 0.5, local.getY(), local.getZ() + 0.5);
        player.getInventory().clearContent();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++)
            player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
        var block = CESGRegistration.GATEWAY_FLUX_BATTERY.get();
        place(level, local, block);
        var battery = (GatewayFluxBatteryBlockEntity) level.getBlockEntity(local);
        battery.getTankInventory().fill(new FluidStack((net.minecraft.world.level.material.Fluid)
                com.cesg.init.CESGFluids.LIQUID_EYE_OF_ENDER.getSource(), 2345), IFluidHandler.FluidAction.EXECUTE);
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(local),
                net.minecraft.core.Direction.UP, local, false);
        block.onSneakWrenched(level.getBlockState(local), new net.minecraft.world.item.context.UseOnContext(player,
                net.minecraft.world.InteractionHand.MAIN_HAND, hit));
        assertTrue(level.getBlockEntity(local) == null);
        var drops = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(local).inflate(3), entity -> entity.getItem().is(block.asItem()));
        assertEquals(1, drops.size());
        assertEquals(1, drops.get(0).getItem().getCount());
        assertEquals(2345, GatewayFluxBatteryItem.storedFuel(drops.get(0).getItem(), level.registryAccess()).getAmount());
        drops.get(0).discard();
        player.getInventory().clearContent();
        helper.succeed();
    }

    private StorageBridgeBlockEntity linkedBridges(ServerLevel level) {
        CrossDimensionalGatewayCoreBlockEntity core = core(level, local);
        core(level, remote);
        core.setPartner(new GatewayPartner(level.dimension(), remote, true));
        place(level, local.east(), CESGRegistration.STORAGE_BRIDGE.get());
        place(level, remote.east(), CESGRegistration.STORAGE_BRIDGE.get());
        place(level, local.east(2), CESGRegistration.STORAGE_NETWORK_CONTROLLER.get());
        place(level, remote.east(2), CESGRegistration.STORAGE_NETWORK_CONTROLLER.get());
        return (StorageBridgeBlockEntity) level.getBlockEntity(local.east());
    }

    private static void box(ServerLevel level, BlockPos pos, List<StoredSlot> contents, boolean filter) {
        place(level, pos, EnhancedShulkerBoxes.DEFAULT.get());
        ItemStack item = new ItemStack(EnhancedShulkerBoxes.DEFAULT.get());
        item.set(CESGDataComponents.ENHANCED_SHULKER.get(), new EnhancedShulkerContents(2, 54, 2, contents,
                filter ? List.of(new StoredSlot(0, new ItemStack(CESGRegistration.FILTER_UPGRADE.get()))) : List.of(),
                filter ? new ItemStack(Items.GOLD_INGOT) : ItemStack.EMPTY));
        ((EnhancedShulkerBoxBlockEntity) level.getBlockEntity(pos)).loadFromItem(item);
    }

    private static CrossDimensionalGatewayCoreBlockEntity core(ServerLevel level, BlockPos pos) {
        place(level, pos, CESGRegistration.CROSS_DIMENSIONAL_GATEWAY_CORE.get());
        CrossDimensionalGatewayCoreBlockEntity core = coreAt(level, pos);
        core.setSpeed(16);
        return core;
    }

    private static CrossDimensionalGatewayCoreBlockEntity coreAt(ServerLevel level, BlockPos pos) {
        return (CrossDimensionalGatewayCoreBlockEntity) level.getBlockEntity(pos);
    }

    private static GatewayPortBlockEntity port(ServerLevel level, BlockPos pos) {
        place(level, pos, CESGRegistration.GATEWAY_PORT.get());
        return (GatewayPortBlockEntity) level.getBlockEntity(pos);
    }

    private static void place(ServerLevel level, BlockPos pos, Block block) {
        level.getChunkAt(pos);
        level.setBlock(pos, block.defaultBlockState(), 3);
    }

    private static void assertEquals(int expected, int actual) {
        if (expected != actual) throw new AssertionError("Expected " + expected + " but got " + actual);
    }

    private static void assertTrue(boolean condition) {
        if (!condition) throw new AssertionError("Expected condition to hold");
    }

    private static void withCost(String section, String name, int cost, Runnable action) {
        ModConfigSpec.IntValue value = (ModConfigSpec.IntValue) CESGConfig.SPEC.getValues().get(List.of(section, name));
        int previous = value.get();
        try { value.set(cost); action.run(); }
        finally { value.set(previous); }
    }

    private static int invoke(Object target, String name) {
        try {
            Method method = target.getClass().getDeclaredMethod(name);
            method.setAccessible(true);
            return (int) method.invoke(target);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    private static int invokeStatic(String name, Class<?>[] types, Object... args) {
        try {
            Method method = StorageBridgeBlockEntity.class.getDeclaredMethod(name, types);
            method.setAccessible(true);
            return (int) method.invoke(null, args);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
}
