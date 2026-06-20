package com.cesg.upgrades;

import com.cesg.init.CESGMenus;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class EnhancedShulkerMenu extends AbstractContainerMenu {
    private final ItemStack shulker;
    private final EnhancedShulkerItemStackHandler handler;

    public EnhancedShulkerMenu(int containerId, Inventory playerInventory, ItemStack shulker) {
        super(CESGMenus.ENHANCED_SHULKER.get(), containerId);
        this.shulker = shulker;
        this.handler = new EnhancedShulkerItemStackHandler(shulker,
                shulker.get(com.cesg.init.CESGDataComponents.ENHANCED_SHULKER.get()));

        int rows = handler.getSlots() / 9;
        int y = 18;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new SlotItemHandler(handler, col + row * 9, 8 + col * 18, y + row * 18));
            }
        }

        y += rows * 18 + 8;
        for (int i = 0; i < handler.getUpgradeStacks().size(); i++) {
            addSlot(new UpgradeSlot(handler, i, 8 + i * 18, y));
        }

        addPlayerInventory(playerInventory, y + 24);
    }

    public EnhancedShulkerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, ItemStack.STREAM_CODEC.decode(buf));
    }

    private void addPlayerInventory(Inventory playerInventory, int y) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, y + 58));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getInventory().contains(shulker);
    }

    private static class UpgradeSlot extends SlotItemHandler {
        public UpgradeSlot(EnhancedShulkerItemStackHandler handler, int index, int x, int y) {
            super(new net.neoforged.neoforge.items.ItemStackHandler(handler.getUpgradeStacks()) {
                @Override
                public int getSlots() {
                    return handler.getUpgradeStacks().size();
                }

                @Override
                public ItemStack getStackInSlot(int slot) {
                    return handler.getUpgradeStacks().get(slot);
                }

                @Override
                public void setStackInSlot(int slot, ItemStack stack) {
                    handler.getUpgradeStacks().set(slot, stack);
                }
            }, index, x, y);
        }
    }
}
