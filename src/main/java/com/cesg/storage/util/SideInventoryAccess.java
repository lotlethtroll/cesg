package com.cesg.storage.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

public final class SideInventoryAccess {
    private SideInventoryAccess() {}

    public static IItemHandler getAttachedInventory(Level level, BlockPos pos, Direction side) {
        if (level == null)
            return null;
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos.relative(side), side.getOpposite());
    }
}
