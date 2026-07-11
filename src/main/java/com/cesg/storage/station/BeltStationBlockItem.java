package com.cesg.storage.station;

import com.simibubi.create.content.kinetics.belt.BeltBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import org.jetbrains.annotations.Nullable;

/**
 * Placement helper for belt stations, which must sit exactly TWO blocks above a Create belt
 * (Create's belt-processing rule). Aiming at the belt — or anywhere in the two blocks above it —
 * retargets placement to the correct height automatically; with no belt in reach, placement is
 * blocked with an explanatory message instead of silently producing a non-functional station.
 */
public class BeltStationBlockItem extends BlockItem {
    public BeltStationBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    private static boolean beltAt(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof BeltBlock;
    }

    /** The station goes at belt+2; aiming at the belt or the two blocks above it all resolve there. */
    @Nullable
    private static BlockPos stationPosFor(Level level, BlockPos target) {
        if (beltAt(level, target.below(2)))
            return target;                 // already the right height
        if (beltAt(level, target.below(1)))
            return target.above(1);        // aimed one block above the belt
        if (beltAt(level, target))
            return target.above(2);        // aimed at the belt itself
        return null;
    }

    @Nullable
    @Override
    public BlockPlaceContext updatePlacementContext(BlockPlaceContext context) {
        BlockPos corrected = stationPosFor(context.getLevel(), context.getClickedPos());
        if (corrected == null)
            return null;
        if (corrected.equals(context.getClickedPos()))
            return context;
        if (!context.getLevel().getBlockState(corrected).canBeReplaced())
            return null;
        return BlockPlaceContext.at(context, corrected, context.getClickedFace());
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        InteractionResult result = super.place(context);
        if (!result.consumesAction() && context.getPlayer() != null && context.getLevel().isClientSide)
            context.getPlayer().displayClientMessage(Component.translatable("cesg.station.needs_belt"), true);
        return result;
    }
}
