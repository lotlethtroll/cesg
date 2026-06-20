package com.cesg.storage.util;

import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

import net.minecraft.world.item.ItemStack;

public final class BeltShulkerProcessing {
    private BeltShulkerProcessing() {}

    @FunctionalInterface
    public interface ShulkerProcessor {
        BeltProcessingBehaviour.ProcessingResult process(ItemStack shulker, boolean continuing);
    }

    public static BeltProcessingBehaviour create(SmartBlockEntity be, ShulkerProcessor processor) {
        return new BeltProcessingBehaviour(be)
                .whenItemEnters((transported, inventory) -> {
                    BeltProcessingBehaviour.ProcessingResult result =
                            processor.process(transported.stack, false);
                    if (result == BeltProcessingBehaviour.ProcessingResult.HOLD)
                        transported.stack = transported.stack.copy();
                    return result;
                })
                .whileItemHeld((transported, inventory) -> {
                    ItemStack before = transported.stack.copy();
                    BeltProcessingBehaviour.ProcessingResult result =
                            processor.process(transported.stack, true);
                    if (!ItemStack.matches(before, transported.stack))
                        transported.stack = transported.stack.copy();
                    return result;
                });
    }
}
