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

    private static StorageTerminalMenu menu(GameTestHelper helper, ServerPlayer player) {
        return new StorageTerminalMenu(1, player.getInventory(), helper.absolutePos(new BlockPos(1, 2, 1)));
    }
}
