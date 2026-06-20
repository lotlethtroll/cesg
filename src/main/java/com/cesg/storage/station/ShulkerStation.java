package com.cesg.storage.station;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

public interface ShulkerStation {
    boolean hasDockedShulker();

    ItemStack getHeldShulker();

    void setHeldShulker(ItemStack stack);

    void clearHeldShulkerAfterEject();

    boolean canExposeShulkerForEject(Direction side);
}
