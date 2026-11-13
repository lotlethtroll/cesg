package com.cesg.gateways;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.cesg.CESGConfig;
import com.cesg.init.CESGFluids;
import com.cesg.init.CESGRegistration;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 2.0 whole-array battery relocation. Every scenario checks conservation: fuel in world tanks plus
 * inventory items plus dropped items equals the original amount.
 */
@GameTestHolder("cesg")
@PrefixGameTestTemplate(false)
public class BatteryArrayRelocationTest {
    private static final int[][] SIZES = {{2, 1}, {2, 2}, {3, 3}};
    private static boolean listening;
    private static boolean cancelBreaks;
    private static boolean cancelPlaces;

    @GameTest(template = "empty")
    public void wrenchPicksUpWholeArrayIntoOneItem(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
        for (int[] size : SIZES)
            for (Fluid fluid : fuels())
                for (boolean creative : new boolean[] {false, true})
                    for (boolean farMember : new boolean[] {false, true}) {
                        int w = size[0], h = size[1], full = w * w * h * GatewayFluxBatteryBlockEntity.capacityPerBlock();
                        for (int amount : new int[] {1, full / 2 + 3, full}) {
                            FakePlayer player = player(helper, creative);
                            fill(build(level, origin, w, h), fluid, amount);
                            BlockPos click = farMember ? origin.offset(w - 1, h - 1, w - 1) : origin;
                            check(wrench(level, player, click) == InteractionResult.SUCCESS, "pickup refused " + w + "x" + h);
                            checkSpaceEmpty(level, origin, w, h);
                            ItemStack item = onlyBattery(player);
                            FluidStack fuel = GatewayFluxBatteryItem.rawFuel(item, level.registryAccess());
                            check(fuel.getFluid().isSame(fluid) && fuel.getAmount() == amount, "item fuel " + fuel.getAmount());
                            int[] packed = GatewayFluxBatteryItem.arraySize(item);
                            check(packed != null && packed[0] == w && packed[1] == h, "item size");
                            check(item.getMaxStackSize() == 1, "packed item stacks");
                            checkConserved(helper, player, amount);
                        }
                    }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void packedArrayRoundTripsThroughPlacementAndReload(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HolderLookup.Provider registries = level.registryAccess();
        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos target = helper.absolutePos(new BlockPos(8, 1, 8));
        int index = 0;
        for (int[] size : SIZES)
            for (boolean creative : new boolean[] {false, true}) {
                int w = size[0], h = size[1];
                Fluid fluid = fuels().get(index++ % 2);
                int amount = w * w * h * GatewayFluxBatteryBlockEntity.capacityPerBlock() * 2 / 3 + 1;
                FakePlayer player = player(helper, creative);
                fill(build(level, origin, w, h), fluid, amount);
                check(wrench(level, player, origin) == InteractionResult.SUCCESS, "initial pickup");
                ItemStack item = onlyBattery(player);
                for (int cycle = 0; cycle < 3; cycle++) {
                    item = ItemStack.parse(registries, item.save(registries)).orElseThrow();
                    player.getInventory().clearContent();
                    check(place(player, item, target, level::isLoaded).consumesAction(), "placement refused " + w + "x" + h);
                    check(player.getMainHandItem().isEmpty(), "placement did not consume the item");
                    // Facing east, the array extends +x (forward) and +z (right) from the clicked position.
                    GatewayFluxBatteryBlockEntity controller = (GatewayFluxBatteryBlockEntity) level.getBlockEntity(target);
                    check(controller != null && controller.isController() && controller.getWidth() == w
                            && controller.getHeight() == h && controller.isArrayPrismIntact(), "placed array shape");
                    check(controller.storedFluid().getFluid().isSame(fluid)
                            && controller.storedFluid().getAmount() == amount, "placed fuel");
                    checkConserved(helper, player, amount);
                    var restored = new GatewayFluxBatteryBlockEntity(target, level.getBlockState(target));
                    restored.loadAdditional(controller.getUpdateTag(registries), registries);
                    check(restored.getWidth() == w && restored.getHeight() == h
                            && restored.storedFluid().getAmount() == amount, "reloaded controller");
                    check(wrench(level, player, target.offset(w - 1, 0, w - 1)) == InteractionResult.SUCCESS, "re-pickup");
                    checkSpaceEmpty(level, target, w, h);
                    item = onlyBattery(player);
                    check(GatewayFluxBatteryItem.rawFuel(item, registries).getAmount() == amount, "re-picked fuel");
                }
                player.getInventory().clearContent();
            }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void refusedArrayPickupChangesNothing(GameTestHelper helper) {
        listen();
        ServerLevel level = helper.getLevel();
        FakePlayer player = player(helper, false);
        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
        int amount = 12345;
        GatewayFluxBatteryBlockEntity controller = build(level, origin, 2, 2);
        fill(controller, fuels().get(0), amount);
        cancelBreaks = true;
        try {
            check(wrench(level, player, origin.offset(1, 1, 1)) == InteractionResult.FAIL, "cancelled break was ignored");
        } finally {
            cancelBreaks = false;
        }
        checkUnchanged(helper, level, player, origin, 2, 2, amount);
        BlockPos unloaded = origin.offset(1, 1, 1);
        check(GatewayFluxBatteryBlock.pickUp(level, player, origin, pos -> !pos.equals(unloaded)) == InteractionResult.FAIL,
                "unloaded member was ignored");
        checkUnchanged(helper, level, player, origin, 2, 2, amount);
        controller.scheduleConnectivityUpdate();
        check(wrench(level, player, origin) == InteractionResult.FAIL, "pending connectivity was ignored");
        checkUnchanged(helper, level, player, origin, 2, 2, amount);

        // The 1.2 single-battery path also respects a cancelled BreakEvent.
        BlockPos single = helper.absolutePos(new BlockPos(9, 1, 9));
        GatewayFluxBatteryBlockEntity battery = build(level, single, 1, 1);
        fill(battery, fuels().get(1), 777);
        cancelBreaks = true;
        try {
            check(wrench(level, player, single) == InteractionResult.FAIL, "single pickup ignored a cancelled break");
        } finally {
            cancelBreaks = false;
        }
        check(level.getBlockEntity(single) == battery && battery.storedFluid().getAmount() == 777, "single changed");
        check(player.getInventory().isEmpty(), "single refusal gave an item");
        check(wrench(level, player, single) == InteractionResult.SUCCESS, "single pickup");
        ItemStack item = onlyBattery(player);
        check(GatewayFluxBatteryItem.rawFuel(item, level.registryAccess()).getAmount() == 777, "single fuel");
        check(!item.get(DataComponents.CUSTOM_DATA).copyTag().contains(GatewayFluxBatteryItem.ARRAY_TAG),
                "single item changed format");
        player.getInventory().clearContent();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void refusedPackedPlacementKeepsTheItem(GameTestHelper helper) {
        listen();
        ServerLevel level = helper.getLevel();
        FakePlayer player = player(helper, false);
        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos target = helper.absolutePos(new BlockPos(8, 1, 8));
        int capacity = GatewayFluxBatteryBlockEntity.capacityPerBlock();
        int amount = 8 * capacity;
        fill(build(level, origin, 2, 2), fuels().get(1), amount);
        check(wrench(level, player, origin) == InteractionResult.SUCCESS, "pickup");
        ItemStack item = onlyBattery(player).copy();
        player.getInventory().clearContent();

        BlockPos obstacle = target.offset(1, 1, 1);
        level.setBlock(obstacle, Blocks.STONE.defaultBlockState(), 3);
        checkRefusedPlacement(helper, player, item, target, level::isLoaded, amount, "obstructed space");
        level.removeBlock(obstacle, false);

        BlockPos neighbor = target.offset(2, 0, 0);
        build(level, neighbor, 1, 1);
        checkRefusedPlacement(helper, player, item, target, level::isLoaded, amount, "adjacent battery");
        level.removeBlock(neighbor, false);

        BlockPos unloaded = target.offset(1, 0, 1);
        checkRefusedPlacement(helper, player, item, target, pos -> !pos.equals(unloaded), amount, "unloaded chunk");

        withCapacity(Math.max(500, capacity / 2), () ->
                checkRefusedPlacement(helper, player, item, target, level::isLoaded, amount, "lowered capacity"));

        cancelPlaces = true;
        try {
            checkRefusedPlacement(helper, player, item, target, level::isLoaded, amount, "cancelled place event");
        } finally {
            cancelPlaces = false;
        }

        check(place(player, item.copy(), target, level::isLoaded).consumesAction(), "placement after refusals");
        checkConserved(helper, player, amount);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void fullInventoryArrayPickupDropsExactlyOneItem(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakePlayer player = player(helper, false);
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++)
            player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
        fill(build(level, origin, 2, 2), fuels().get(1), 5000);
        check(wrench(level, player, origin.offset(1, 0, 1)) == InteractionResult.SUCCESS, "pickup");
        checkSpaceEmpty(level, origin, 2, 2);
        List<ItemEntity> drops = batteryDrops(helper);
        check(drops.size() == 1 && drops.get(0).getItem().getCount() == 1, "expected exactly one dropped item");
        check(GatewayFluxBatteryItem.rawFuel(drops.get(0).getItem(), level.registryAccess()).getAmount() == 5000, "drop fuel");
        checkConserved(helper, player, 5000);
        drops.forEach(ItemEntity::discard);
        player.getInventory().clearContent();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void emptyArrayPickupGivesPlainBatteries(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
        for (int[] size : SIZES) {
            int w = size[0], h = size[1];
            FakePlayer player = player(helper, false);
            build(level, origin, w, h);
            check(wrench(level, player, origin.offset(w - 1, h - 1, 0)) == InteractionResult.SUCCESS, "empty pickup");
            checkSpaceEmpty(level, origin, w, h);
            int count = 0;
            for (ItemStack stack : inventory(player)) {
                check(stack.get(DataComponents.CUSTOM_DATA) == null, "empty array gave a charged item");
                count += stack.getCount();
            }
            check(count == w * w * h, "expected " + w * w * h + " plain batteries, got " + count);
            player.getInventory().clearContent();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void normalMiningOfAFueledArrayIsUnchanged(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakePlayer player = player(helper, false);
        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
        int amount = 8 * GatewayFluxBatteryBlockEntity.capacityPerBlock();
        fill(build(level, origin, 2, 2), fuels().get(0), amount);
        for (int y = 1; y >= 0; y--)
            for (int x = 1; x >= 0; x--)
                for (int z = 1; z >= 0; z--) {
                    level.destroyBlock(origin.offset(x, y, z), true, player);
                    check(worldFuel(helper) <= amount, "mining created fuel");
                }
        List<ItemEntity> drops = batteryDrops(helper);
        int count = 0;
        for (ItemEntity drop : drops) {
            check(GatewayFluxBatteryItem.rawFuel(drop.getItem(), level.registryAccess()).isEmpty(), "mined drop carries fuel");
            count += drop.getItem().getCount();
        }
        check(count == 8, "expected 8 mined batteries, got " + count);
        check(worldFuel(helper) == 0 && inventoryFuel(player) == 0, "mined array kept fuel");
        drops.forEach(ItemEntity::discard);
        helper.succeed();
    }

    // ---- helpers ----

    private static void checkRefusedPlacement(GameTestHelper helper, FakePlayer player, ItemStack item, BlockPos target,
            Predicate<BlockPos> loaded, int amount, String scenario) {
        ServerLevel level = helper.getLevel();
        player.getInventory().clearContent();
        check(place(player, item.copy(), target, loaded) == InteractionResult.FAIL, scenario + " was not refused");
        ItemStack held = player.getMainHandItem();
        check(held.getCount() == 1 && GatewayFluxBatteryItem.rawFuel(held, level.registryAccess()).getAmount() == amount,
                scenario + " changed the item");
        for (int y = 0; y < 2; y++)
            for (int x = 0; x < 2; x++)
                for (int z = 0; z < 2; z++)
                    check(!GatewayFluxBatteryBlock.isBattery(level.getBlockState(target.offset(x, y, z))),
                            scenario + " left a battery in the space");
        checkConserved(helper, player, amount);
        player.getInventory().clearContent();
    }

    private static void checkUnchanged(GameTestHelper helper, ServerLevel level, Player player, BlockPos origin, int w, int h,
            int amount) {
        GatewayFluxBatteryBlockEntity controller = (GatewayFluxBatteryBlockEntity) level.getBlockEntity(origin);
        check(controller != null && controller.isController() && controller.getWidth() == w && controller.getHeight() == h
                && controller.isArrayPrismIntact(), "refusal changed the array");
        check(controller.storedFluid().getAmount() == amount && worldFuel(helper) == amount, "refusal changed the fuel");
        check(player.getInventory().isEmpty() && batteryDrops(helper).isEmpty(), "refusal gave an item");
    }

    /** World tanks + inventory items + dropped items == original fuel. */
    private static void checkConserved(GameTestHelper helper, Player player, int amount) {
        int world = worldFuel(helper), held = inventoryFuel(player), dropped = 0;
        for (ItemEntity drop : batteryDrops(helper))
            dropped += GatewayFluxBatteryItem.rawFuel(drop.getItem(), helper.getLevel().registryAccess()).getAmount()
                    * drop.getItem().getCount();
        check(world + held + dropped == amount,
                "fuel not conserved: world " + world + " + inventory " + held + " + drops " + dropped + " != " + amount);
    }

    private static int worldFuel(GameTestHelper helper) {
        int total = 0;
        for (BlockPos pos : BlockPos.betweenClosed(helper.absolutePos(BlockPos.ZERO), helper.absolutePos(new BlockPos(15, 7, 15))))
            if (helper.getLevel().getBlockEntity(pos) instanceof GatewayFluxBatteryBlockEntity battery)
                total += battery.getTankInventory().getFluidAmount();
        return total;
    }

    private static int inventoryFuel(Player player) {
        int total = 0;
        for (ItemStack stack : inventory(player))
            total += GatewayFluxBatteryItem.rawFuel(stack, player.level().registryAccess()).getAmount() * stack.getCount();
        return total;
    }

    private static List<ItemEntity> batteryDrops(GameTestHelper helper) {
        AABB area = new AABB(Vec3.atLowerCornerOf(helper.absolutePos(BlockPos.ZERO)),
                Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(16, 8, 16)))).inflate(2);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area,
                entity -> entity.isAlive() && entity.getItem().is(CESGRegistration.GATEWAY_FLUX_BATTERY.get().asItem()));
    }

    private static List<ItemStack> inventory(Player player) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(CESGRegistration.GATEWAY_FLUX_BATTERY.get().asItem())) stacks.add(stack);
        }
        return stacks;
    }

    private static ItemStack onlyBattery(Player player) {
        List<ItemStack> stacks = inventory(player);
        check(stacks.size() == 1 && stacks.get(0).getCount() == 1, "expected exactly one battery item, got " + stacks);
        return stacks.get(0);
    }

    private static void checkSpaceEmpty(ServerLevel level, BlockPos origin, int w, int h) {
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                for (int z = 0; z < w; z++)
                    check(level.getBlockState(origin.offset(x, y, z)).isAir(), "array space not empty at " + origin.offset(x, y, z));
    }

    /** Places a W×W×H array bottom layer first, as a player would, and checks Create formed it. */
    private static GatewayFluxBatteryBlockEntity build(ServerLevel level, BlockPos origin, int w, int h) {
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                for (int z = 0; z < w; z++)
                    level.setBlock(origin.offset(x, y, z), CESGRegistration.GATEWAY_FLUX_BATTERY.get().defaultBlockState(), 3);
        GatewayFluxBatteryBlockEntity controller = (GatewayFluxBatteryBlockEntity) level.getBlockEntity(origin);
        check(controller.isController() && controller.getWidth() == w && controller.getHeight() == h
                && controller.isArrayPrismIntact(), "array did not form " + w + "x" + h);
        return controller;
    }

    private static void fill(GatewayFluxBatteryBlockEntity controller, Fluid fluid, int amount) {
        check(controller.getTankInventory().fill(new FluidStack(fluid, amount), IFluidHandler.FluidAction.EXECUTE) == amount,
                "could not fill " + amount);
    }

    private static InteractionResult wrench(ServerLevel level, Player player, BlockPos pos) {
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        return CESGRegistration.GATEWAY_FLUX_BATTERY.get().onSneakWrenched(level.getBlockState(pos),
                new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    private static InteractionResult place(Player player, ItemStack stack, BlockPos target, Predicate<BlockPos> loaded) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var hit = new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false);
        var context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, player.getMainHandItem(), hit);
        return ((GatewayFluxBatteryItem) CESGRegistration.GATEWAY_FLUX_BATTERY.get().asItem()).place(context, loaded);
    }

    private static FakePlayer player(GameTestHelper helper, boolean creative) {
        FakePlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setGameMode(creative ? GameType.CREATIVE : GameType.SURVIVAL);
        player.getInventory().clearContent();
        player.setYRot(270f); // facing east
        BlockPos at = helper.absolutePos(new BlockPos(13, 1, 2));
        player.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        return player;
    }

    private static List<Fluid> fuels() {
        return List.<Fluid>of(CESGFluids.TELEPORT_ESSENCE.getSource(), CESGFluids.LIQUID_EYE_OF_ENDER.getSource());
    }

    private static void withCapacity(int capacity, Runnable action) {
        ModConfigSpec.IntValue value = (ModConfigSpec.IntValue) CESGConfig.SPEC.getValues()
                .get(List.of("battery", "capacityMbPerBlock"));
        int previous = value.get();
        try {
            value.set(capacity);
            action.run();
        } finally {
            value.set(previous);
        }
    }

    /** Test-only protection listeners; the flags are only set inside one synchronous test body. */
    private static void listen() {
        if (listening) return;
        listening = true;
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, BlockEvent.BreakEvent.class, event -> {
            if (cancelBreaks) event.setCanceled(true);
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, BlockEvent.EntityMultiPlaceEvent.class, event -> {
            if (cancelPlaces) event.setCanceled(true);
        });
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
