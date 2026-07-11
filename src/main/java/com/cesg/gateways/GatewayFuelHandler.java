package com.cesg.gateways;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import com.cesg.init.CESGFluids;
import com.cesg.init.CESGRegistration;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import org.jetbrains.annotations.Nullable;

/**
 * Exposes a Cross-Dimensional Gateway Core's two fuel tanks as a Create-pipe-fillable fluid handler.
 * Teleport Essence routes to the same-dimension tank, Liquid Eye of Ender to the cross-dimension tank.
 * Gateway Frame blocks delegate to their connected Core (found by walking the ring), so fuel can be
 * pumped into any segment of the frame.
 */
public class GatewayFuelHandler implements IFluidHandler {
    private static final int RING_SCAN_LIMIT = 64;

    @Nullable
    private final CrossDimensionalGatewayCoreBlockEntity core;
    @Nullable
    private final GatewayFrameBlockEntity sourceFrame;

    /** Core-mode: pipes attached directly to the Core. */
    public GatewayFuelHandler(CrossDimensionalGatewayCoreBlockEntity core) {
        this.core = core;
        this.sourceFrame = null;
    }

    /** Frame-mode: the core is resolved live per operation (frames work with NO core at all). */
    public GatewayFuelHandler(GatewayFrameBlockEntity sourceFrame) {
        this.core = null;
        this.sourceFrame = sourceFrame;
    }

    @Nullable
    private CrossDimensionalGatewayCoreBlockEntity core() {
        if (core != null)
            return core;
        if (sourceFrame == null || sourceFrame.getLevel() == null)
            return null;
        return findCore(sourceFrame.getLevel(), sourceFrame.getBlockPos());
    }

    @Override
    public int getTanks() {
        return 2;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        CrossDimensionalGatewayCoreBlockEntity core = core();
        int essence = core != null ? core.getEssenceMb() : 0;
        int eye = core != null ? core.getEyeMb() : 0;
        if (sourceFrame != null) {
            FluidStack held = sourceFrame.buffer.getFluid();
            GatewayFrameBlock.FrameFuel type = GatewayFrameBlockEntity.fuelTypeOf(held);
            if (type == GatewayFrameBlock.FrameFuel.ESSENCE)
                essence += held.getAmount();
            else if (type == GatewayFrameBlock.FrameFuel.EYE)
                eye += held.getAmount();
        }
        return tank == 0
                ? stack(CESGFluids.TELEPORT_ESSENCE.getSource(), essence)
                : stack(CESGFluids.LIQUID_EYE_OF_ENDER.getSource(), eye);
    }

    @Override
    public int getTankCapacity(int tank) {
        return CrossDimensionalGatewayCoreBlockEntity.TANK_CAPACITY;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return tank == 0 ? isEssence(stack) : isEye(stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !(isEssence(resource) || isEye(resource)))
            return 0;
        CrossDimensionalGatewayCoreBlockEntity core = core();
        int filled = 0;
        if (core != null)
            filled = isEssence(resource)
                    ? core.fillEssence(resource.getAmount(), action.simulate())
                    : core.fillEye(resource.getAmount(), action.simulate());
        // Core full or absent: overflow parks in the frame buffer (visible transit fluid).
        if (sourceFrame != null && filled < resource.getAmount())
            filled += sourceFrame.buffer.fill(
                    resource.copyWithAmount(resource.getAmount() - filled), action);
        if (filled > 0 && action.execute())
            propagateFlow(isEssence(resource)
                    ? com.cesg.gateways.GatewayFrameBlock.FrameFuel.ESSENCE
                    : com.cesg.gateways.GatewayFrameBlock.FrameFuel.EYE);
        return filled;
    }

    /**
     * Cosmetic: light the pumped fluid through every frame on the ring path from the pumped frame
     * to the Core, so builders SEE the fuel travel (even on an unlit ring). Purely visual.
     */
    private void propagateFlow(com.cesg.gateways.GatewayFrameBlock.FrameFuel fuel) {
        if (sourceFrame == null || sourceFrame.getLevel() == null)
            return;
        Level level = sourceFrame.getLevel();
        // BFS with parents from the pumped frame to the core, then walk the path back.
        java.util.Map<BlockPos, BlockPos> parents = new java.util.HashMap<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        BlockPos start = sourceFrame.getBlockPos().immutable();
        parents.put(start, start);
        queue.add(start);
        BlockPos corePos = null;
        while (!queue.isEmpty() && parents.size() <= RING_SCAN_LIMIT) {
            BlockPos pos = queue.poll();
            if (level.getBlockEntity(pos) instanceof CrossDimensionalGatewayCoreBlockEntity) {
                corePos = pos;
                break;
            }
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (!parents.containsKey(next) && isRingBlock(level.getBlockState(next))) {
                    parents.put(next.immutable(), pos);
                    queue.add(next.immutable());
                }
            }
        }
        if (corePos == null)
            return;
        BlockPos step = parents.get(corePos);
        while (step != null && !step.equals(parents.get(step))) {
            if (level.getBlockEntity(step) instanceof GatewayFrameBlockEntity frame)
                frame.showFlow(fuel);
            step = parents.get(step);
        }
        if (level.getBlockEntity(start) instanceof GatewayFrameBlockEntity frame)
            frame.showFlow(fuel);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty())
            return FluidStack.EMPTY;
        // Frame buffers give their transit fluid back first (draining decorative runs is lossless).
        if (sourceFrame != null && FluidStack.isSameFluidSameComponents(sourceFrame.buffer.getFluid(), resource)) {
            FluidStack fromBuffer = sourceFrame.buffer.drain(resource.getAmount(), action);
            if (!fromBuffer.isEmpty())
                return fromBuffer;
        }
        CrossDimensionalGatewayCoreBlockEntity core = core();
        if (core == null)
            return FluidStack.EMPTY;
        if (isEssence(resource))
            return stack(CESGFluids.TELEPORT_ESSENCE.getSource(), core.drainEssence(resource.getAmount(), action.simulate()));
        if (isEye(resource))
            return stack(CESGFluids.LIQUID_EYE_OF_ENDER.getSource(), core.drainEye(resource.getAmount(), action.simulate()));
        return FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (sourceFrame != null && !sourceFrame.buffer.isEmpty())
            return sourceFrame.buffer.drain(maxDrain, action);
        CrossDimensionalGatewayCoreBlockEntity core = core();
        if (core == null)
            return FluidStack.EMPTY;
        if (core.getEssenceMb() > 0)
            return stack(CESGFluids.TELEPORT_ESSENCE.getSource(), core.drainEssence(maxDrain, action.simulate()));
        if (core.getEyeMb() > 0)
            return stack(CESGFluids.LIQUID_EYE_OF_ENDER.getSource(), core.drainEye(maxDrain, action.simulate()));
        return FluidStack.EMPTY;
    }

    private static FluidStack stack(Fluid fluid, int amount) {
        return amount > 0 ? new FluidStack(fluid, amount) : FluidStack.EMPTY;
    }

    private static boolean isEssence(FluidStack stack) {
        return stack.getFluid().getFluidType() == CESGFluids.TELEPORT_ESSENCE.getType();
    }

    private static boolean isEye(FluidStack stack) {
        return stack.getFluid().getFluidType() == CESGFluids.LIQUID_EYE_OF_ENDER.getType();
    }

    /** Walks the connected ring of Gateway Frame / Core blocks from {@code start} to find the Core. */
    @Nullable
    public static CrossDimensionalGatewayCoreBlockEntity findCore(Level level, BlockPos start) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(start.immutable());
        visited.add(start.immutable());
        while (!queue.isEmpty() && visited.size() <= RING_SCAN_LIMIT) {
            BlockPos pos = queue.poll();
            if (level.getBlockEntity(pos) instanceof CrossDimensionalGatewayCoreBlockEntity core)
                return core;
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (!visited.contains(next) && isRingBlock(level.getBlockState(next))) {
                    visited.add(next);
                    queue.add(next);
                }
            }
        }
        return null;
    }

    /** Single source of truth for what counts as a gateway-ring block (frames route, ports attach). */
    public static boolean isRingBlock(BlockState state) {
        return state.is(CESGRegistration.GATEWAY_FRAME.get())
                || state.is(CESGRegistration.CROSS_DIMENSIONAL_GATEWAY_CORE.get());
    }

    /**
     * Frame blocks have no BlockEntity, so NeoForge caches their fluid-handler lookup and only refreshes
     * it when that block's own state changes. When a ring block is added/removed, the Core a frame routes
     * to may appear or vanish without the frame's own state changing — so we must manually invalidate the
     * whole connected ring (and thus any attached Create pipe/pump caches) so they re-resolve the Core.
     */
    public static void invalidateRing(Level level, BlockPos start) {
        if (level.isClientSide)
            return;
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(start.immutable());
        visited.add(start.immutable());
        while (!queue.isEmpty() && visited.size() <= RING_SCAN_LIMIT) {
            BlockPos pos = queue.poll();
            level.invalidateCapabilities(pos);
            // Nudge adjacent blocks (Create pipes/pumps) so they re-evaluate their connection to this
            // ring block now that its fluid handler has (re)appeared — a bare cap invalidation won't
            // always rebuild an already-saved "no connection" pipe shape.
            level.updateNeighborsAt(pos, level.getBlockState(pos).getBlock());
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (!visited.contains(next) && isRingBlock(level.getBlockState(next))) {
                    visited.add(next);
                    queue.add(next);
                }
            }
        }
    }
}
