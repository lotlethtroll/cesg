package com.cesg.client;

import com.cesg.init.CESGBlockEntities;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.OrientedRotatingVisual;

import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.minecraft.core.Direction;

public final class CESGKineticVisuals {
    private CESGKineticVisuals() {}

    public static void register() {
        // BER renders the filter preview; must not suppress vanilla BER (Flywheel default).
        SimpleBlockEntityVisualizer.builder(CESGBlockEntities.SHULKER_LOADER.get())
                .factory((ctx, be, pt) -> {
                    Direction shaftFace = be.getBlockState().getValue(DirectionalKineticBlock.FACING).getOpposite();
                    return new OrientedRotatingVisual<>(ctx, (KineticBlockEntity) be, pt, Direction.SOUTH, shaftFace,
                            Models.partial(AllPartialModels.SHAFT_HALF));
                })
                .neverSkipVanillaRender()
                .apply();

        // BER renders the filter preview; must not suppress vanilla BER (Flywheel default).
        SimpleBlockEntityVisualizer.builder(CESGBlockEntities.SHULKER_UNLOADER.get())
                .factory((ctx, be, pt) -> {
                    Direction shaftFace = be.getBlockState().getValue(DirectionalKineticBlock.FACING).getOpposite();
                    return new OrientedRotatingVisual<>(ctx, (KineticBlockEntity) be, pt, Direction.SOUTH, shaftFace,
                            Models.partial(AllPartialModels.SHAFT_HALF));
                })
                .neverSkipVanillaRender()
                .apply();

        // BER renders intake tube; must not suppress vanilla BER (Flywheel default).
        SimpleBlockEntityVisualizer.builder(CESGBlockEntities.SHULKER_BELT_LOADER.get())
                .factory((ctx, be, pt) -> {
                    Direction shaftFace = be.getBlockState().getValue(DirectionalKineticBlock.FACING).getOpposite();
                    return new OrientedRotatingVisual<>(ctx, (KineticBlockEntity) be, pt, Direction.SOUTH, shaftFace,
                            Models.partial(AllPartialModels.SHAFT_HALF));
                })
                .neverSkipVanillaRender()
                .apply();

        // BER renders extraction tube; must not suppress vanilla BER (Flywheel default).
        SimpleBlockEntityVisualizer.builder(CESGBlockEntities.SHULKER_BELT_UNLOADER.get())
                .factory((ctx, be, pt) -> {
                    Direction shaftFace = be.getBlockState().getValue(DirectionalKineticBlock.FACING).getOpposite();
                    return new OrientedRotatingVisual<>(ctx, (KineticBlockEntity) be, pt, Direction.SOUTH, shaftFace,
                            Models.partial(AllPartialModels.SHAFT_HALF));
                })
                .neverSkipVanillaRender()
                .apply();

        SimpleBlockEntityVisualizer.builder(CESGBlockEntities.CROSS_DIMENSIONAL_GATEWAY_CORE.get())
                .factory((ctx, be, pt) -> {
                    Direction shaftFace = be.getBlockState().getValue(DirectionalKineticBlock.FACING).getOpposite();
                    return new OrientedRotatingVisual<>(ctx, be, pt, Direction.SOUTH, shaftFace,
                            Models.partial(AllPartialModels.SHAFT_HALF));
                })
                .neverSkipVanillaRender()
                .apply();

        SimpleBlockEntityVisualizer.builder(CESGBlockEntities.END_GATEWAY.get())
                .factory((ctx, be, pt) -> {
                    Direction shaftFace = be.getBlockState().getValue(DirectionalKineticBlock.FACING).getOpposite();
                    return new OrientedRotatingVisual<>(ctx, be, pt, Direction.SOUTH, shaftFace,
                            Models.partial(AllPartialModels.SHAFT_HALF));
                })
                .neverSkipVanillaRender()
                .apply();
    }
}
