package com.cesg.storage.network;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("cesg")
@PrefixGameTestTemplate(false)
public class TerminalRegressionTest {
    @GameTest(template = "empty")
    public void compactLayoutPreservesInventorySlotIdentity(GameTestHelper helper) {
        ServerPlayer player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());
        StorageTerminalMenu menu = menu(helper, player);
        var craftingResult = menu.getSlot(0);
        var craftingInput = menu.getSlot(1);
        var original = java.util.stream.IntStream.range(10, 46)
                .mapToObj(menu::getSlot).toList();
        for (int offset : new int[] {-18, -36, 0, -18, 0}) {
            menu.positionPlayerInventory(offset);
            for (int i = 10; i < 46; i++) {
                var slot = menu.getSlot(i);
                var before = original.get(i - 10);
                helper.assertTrue(slot.index == i && slot.container == before.container
                        && slot.getContainerSlot() == before.getContainerSlot(), "Layout changed slot identity");
                helper.assertTrue(slot.x == before.x && slot.y == before.y + offset,
                        "Repeated resizing accumulated slot offsets");
            }
        }
        helper.assertTrue(menu.getSlot(0) == craftingResult && menu.getSlot(1) == craftingInput,
                "Layout replaced crafting slots");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void closingReturnsGridContentsExactlyOnce(GameTestHelper helper) {
        ServerPlayer player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());
        player.getInventory().clearContent();
        StorageTerminalMenu menu = menu(helper, player);
        menu.craftSlots().setItem(0, new ItemStack(Items.DIAMOND, 17));
        menu.removed(player);
        menu.removed(player);
        helper.assertTrue(menu.craftSlots().isEmpty(), "Closing left items in the grid");
        helper.assertTrue(player.getInventory().countItem(Items.DIAMOND) == 17, "Closing lost or duplicated items");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void partialCraftOutputNeverMovesWithoutConsumingIngredients(GameTestHelper helper) {
        ServerPlayer player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());
        player.getInventory().clearContent();
        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        player.getInventory().setItem(0, new ItemStack(Items.OAK_PLANKS, 63));
        StorageTerminalMenu menu = menu(helper, player);
        menu.craftSlots().setItem(0, new ItemStack(Items.OAK_LOG));
        menu.quickMoveStack(player, StorageTerminalMenu.RESULT_SLOT);
        helper.assertTrue(player.getInventory().countItem(Items.OAK_PLANKS) == 63, "Partial output duplicated planks");
        helper.assertTrue(menu.craftSlots().getItem(0).is(Items.OAK_LOG), "Blocked craft consumed its input");
        player.getInventory().setItem(1, ItemStack.EMPTY);
        menu.quickMoveStack(player, StorageTerminalMenu.RESULT_SLOT);
        helper.assertTrue(player.getInventory().countItem(Items.OAK_PLANKS) == 67, "A fitting craft did not deliver exactly four planks");
        helper.assertTrue(menu.craftSlots().isEmpty(), "Successful craft did not consume its log");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void recipeTransferPreservesRejectedGridContents(GameTestHelper helper) {
        ServerPlayer player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());
        player.getInventory().clearContent();
        player.getInventory().setItem(0, new ItemStack(Items.OAK_LOG, 2));
        StorageTerminalMenu menu = menu(helper, player);
        menu.craftSlots().setItem(0, new ItemStack(Items.DIAMOND, 17));
        var holder = helper.getLevel().getRecipeManager().byKey(
                net.minecraft.resources.ResourceLocation.withDefaultNamespace("oak_planks")).orElseThrow();
        var recipe = (net.minecraft.world.item.crafting.CraftingRecipe) holder.value();
        menu.fillFromRecipe(recipe); // disconnected network refuses all grid contents
        helper.assertTrue(menu.craftSlots().getItem(0).is(Items.DIAMOND)
                && menu.craftSlots().getItem(0).getCount() == 17, "Recipe transfer overwrote leftovers");
        helper.assertTrue(player.getInventory().countItem(Items.OAK_LOG) == 2, "Blocked transfer took new ingredients");
        menu.craftSlots().clearContent();
        menu.fillFromRecipe(recipe);
        helper.assertTrue(menu.craftSlots().getItem(0).is(Items.OAK_LOG), "Empty grid did not accept the recipe");
        helper.assertTrue(player.getInventory().countItem(Items.OAK_LOG) == 1, "Recipe transfer used the wrong ingredient count");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void batchRefillPreservesCakeBuckets(GameTestHelper helper) {
        ServerPlayer player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());
        player.getInventory().clearContent();
        var level = helper.getLevel();
        BlockPos anchor = helper.absolutePos(new BlockPos(1, 2, 1));
        level.setBlock(anchor, com.cesg.init.CESGRegistration.STORAGE_NETWORK_CONTROLLER.get().defaultBlockState(), 3);
        level.setBlock(anchor.east(), com.cesg.upgrades.EnhancedShulkerBoxes.DEFAULT.get().defaultBlockState(), 3);
        ItemStack box = new ItemStack(com.cesg.upgrades.EnhancedShulkerBoxes.DEFAULT.get());
        box.set(com.cesg.init.CESGDataComponents.ENHANCED_SHULKER.get(),
                new com.cesg.upgrades.EnhancedShulkerContents(2, 54, 2,
                        java.util.List.of(new com.cesg.upgrades.EnhancedShulkerContents.StoredSlot(0,
                                new ItemStack(Items.MILK_BUCKET))), java.util.List.of(), ItemStack.EMPTY));
        ((com.cesg.upgrades.EnhancedShulkerBoxBlockEntity) level.getBlockEntity(anchor.east())).loadFromItem(box);
        StorageTerminalMenu menu = menu(helper, player);
        for (int slot = 0; slot < 3; slot++) menu.craftSlots().setItem(slot, new ItemStack(Items.MILK_BUCKET));
        menu.craftSlots().setItem(3, new ItemStack(Items.SUGAR));
        menu.craftSlots().setItem(4, new ItemStack(Items.EGG));
        menu.craftSlots().setItem(5, new ItemStack(Items.SUGAR));
        for (int slot = 6; slot < 9; slot++) menu.craftSlots().setItem(slot, new ItemStack(Items.WHEAT));
        menu.quickMoveStack(player, StorageTerminalMenu.RESULT_SLOT);
        helper.assertTrue(player.getInventory().countItem(Items.CAKE) == 1, "Expected exactly one crafted cake");
        for (int slot = 0; slot < 3; slot++)
            helper.assertTrue(menu.craftSlots().getItem(slot).is(Items.BUCKET), "Refill overwrote a returned bucket");
        helper.assertTrue(StorageNetwork.extract(level, anchor, new ItemStack(Items.MILK_BUCKET), 1).getCount() == 1,
                "Refill extracted milk over a crafting remainder");
        menu.removed(player);
        helper.assertTrue(player.getInventory().countItem(Items.BUCKET) == 3, "Closing lost cake buckets");
        helper.succeed();
    }

    private static StorageTerminalMenu menu(GameTestHelper helper, ServerPlayer player) {
        return new StorageTerminalMenu(1, player.getInventory(), helper.absolutePos(new BlockPos(1, 2, 1)));
    }
}
