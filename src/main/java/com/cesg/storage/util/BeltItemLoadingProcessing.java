package com.cesg.storage.util;

import static com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult.HOLD;
import static com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult.PASS;
import static com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult.REMOVE;

import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

import net.minecraft.world.item.ItemStack;

/**
 * Hooks a {@link BeltProcessingBehaviour} for machines placed two blocks above a belt
 * (Create's mechanical-press placement rule).
 */
public final class BeltItemLoadingProcessing {
    private BeltItemLoadingProcessing() {}

    @FunctionalInterface
    public interface ItemProcessor {
        /**
         * @param stack      item on the belt (may be mutated when items are extracted)
         * @param continuing {@code false} when the item first arrives; {@code true} while held
         */
        ProcessingState process(ItemStack stack, boolean continuing);
    }

    public enum ProcessingState {
        PASS, HOLD, REMOVE
    }

    public static BeltProcessingBehaviour create(SmartBlockEntity be, ItemProcessor processor) {
        return new BeltProcessingBehaviour(be)
                .whenItemEnters((transported, inventory) -> onEnter(processor, transported))
                .whileItemHeld((transported, inventory) -> onHeld(processor, transported));
    }

    private static BeltProcessingBehaviour.ProcessingResult onEnter(ItemProcessor processor,
            TransportedItemStack transported) {
        ProcessingState state = processor.process(transported.stack, false);
        if (state == ProcessingState.HOLD)
            transported.stack = transported.stack.copy();
        return map(state);
    }

    private static BeltProcessingBehaviour.ProcessingResult onHeld(ItemProcessor processor,
            TransportedItemStack transported) {
        ProcessingState state = processor.process(transported.stack, true);
        // REMOVE only when the processor has fully depleted the stack
        if (state == ProcessingState.REMOVE && !transported.stack.isEmpty())
            return HOLD;
        return map(state);
    }

    private static BeltProcessingBehaviour.ProcessingResult map(ProcessingState state) {
        return switch (state) {
            case PASS -> PASS;
            case HOLD -> HOLD;
            case REMOVE -> REMOVE;
        };
    }
}
