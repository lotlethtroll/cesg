package com.cesg.upgrades;

import com.cesg.init.CESGDataComponents;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;

public class EnhancedShulkerItemStackHandler implements IItemHandlerModifiable {
    private final ItemStack parent;
    private final NonNullList<ItemStack> mainStacks;
    private final NonNullList<ItemStack> upgradeStacks;
    private final EnhancedShulkerContents contents;

    public EnhancedShulkerItemStackHandler(ItemStack parent, EnhancedShulkerContents contents) {
        this.parent = parent;
        this.contents = contents;
        this.mainStacks = NonNullList.withSize(contents.slotCount(), ItemStack.EMPTY);
        contents.mainInventory().copyInto(mainStacks);
        this.upgradeStacks = NonNullList.withSize(contents.upgradeSlotCount(), ItemStack.EMPTY);
        contents.upgradeInventory().copyInto(upgradeStacks);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        validateSlot(slot);
        mainStacks.set(slot, stack);
        sync();
    }

    @Override
    public int getSlots() {
        return mainStacks.size();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        validateSlot(slot);
        return mainStacks.get(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        validateSlot(slot);
        if (!isItemValid(slot, stack))
            return stack;
        ItemStack existing = mainStacks.get(slot);
        if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, stack))
            return stack;

        int max = getSlotLimit(slot);
        int space = max - existing.getCount();
        if (space <= 0)
            return stack;

        int toInsert = Math.min(space, stack.getCount());
        if (!simulate) {
            ItemStack copy = stack.copy();
            copy.setCount(existing.getCount() + toInsert);
            mainStacks.set(slot, copy);
            sync();
        }
        stack.shrink(toInsert);
        return stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        validateSlot(slot);
        ItemStack existing = mainStacks.get(slot);
        if (existing.isEmpty())
            return ItemStack.EMPTY;
        int toExtract = Math.min(amount, existing.getCount());
        ItemStack result = existing.copyWithCount(toExtract);
        if (!simulate) {
            existing.shrink(toExtract);
            if (existing.isEmpty())
                mainStacks.set(slot, ItemStack.EMPTY);
            sync();
        }
        return result;
    }

    @Override
    public int getSlotLimit(int slot) {
        int base = 64;
        if (hasStackDepthUpgrade())
            base = 1024;
        return base;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return true;
    }

    public NonNullList<ItemStack> getUpgradeStacks() {
        return upgradeStacks;
    }

    public boolean hasStackDepthUpgrade() {
        for (ItemStack upgrade : upgradeStacks) {
            if (upgrade.is(com.cesg.init.CESGRegistration.STACK_DEPTH_UPGRADE.get()))
                return true;
        }
        return false;
    }

    private void sync() {
        EnhancedShulkerContents updated = new EnhancedShulkerContents(
                contents.tier(), contents.slotCount(), contents.upgradeSlotCount(),
                ItemContainerContents.fromItems(mainStacks),
                ItemContainerContents.fromItems(upgradeStacks));
        parent.set(CESGDataComponents.ENHANCED_SHULKER.get(), updated);
    }

    private void validateSlot(int slot) {
        if (slot < 0 || slot >= mainStacks.size())
            throw new RuntimeException("Slot " + slot + " not in valid range [0," + mainStacks.size() + ")");
    }
}
