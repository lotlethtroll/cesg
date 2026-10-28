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

    private static StorageTerminalMenu menu(GameTestHelper helper, ServerPlayer player) {
        return new StorageTerminalMenu(1, player.getInventory(), helper.absolutePos(new BlockPos(1, 2, 1)));
    }
}
