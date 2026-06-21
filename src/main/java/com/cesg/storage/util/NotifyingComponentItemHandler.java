package com.cesg.storage.util;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.items.ComponentItemHandler;

public class NotifyingComponentItemHandler extends ComponentItemHandler {
    private final Runnable onChanged;

    public NotifyingComponentItemHandler(
            ItemStack container,
            DataComponentType<ItemContainerContents> component,
            int size,
            Runnable onChanged) {
        super(container, component, size);
        this.onChanged = onChanged;
    }

    @Override
    protected void onContentsChanged(int slot, ItemStack oldStack, ItemStack newStack) {
        super.onContentsChanged(slot, oldStack, newStack);
        onChanged.run();
    }
}
