package com.cesg.gateways;

import com.cesg.init.CESGBlockEntities;
import com.cesg.init.CESGFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Gateway Frame block entity. Besides hosting the BE-backed fuel capability, each frame holds a
 * small REAL fluid buffer so pumped fuel is visible in the conduit unconditionally: no core, no
 * power, no binding required. Pipes always connect; fluid parks in frames when the core is
 * full/absent, creeps frame-to-frame along decorative runs, and forwards into the core whenever
 * it has room. Transit fluid is never voided (drainable, and forwarded automatically).
 */
public class GatewayFrameBlockEntity extends BlockEntity {
    /**
     * Per-frame transit buffer: just enough to keep the conduit visibly flowing, NOT a fuel reservoir.
     * Kept small so a ring can't hoard several trips of invisible backup fuel (a full ring is only
     * {@code BUFFER_CAPACITY × frame count}). When a fuelled battery is on the ring the frames don't
     * forward into the Core at all — see {@link #moveBufferAlong}.
     */
    public static final int BUFFER_CAPACITY = 25;
    /** How long the cosmetic flow lingers after the buffer empties, in ticks. */
    private static final int FLOW_LINGER_TICKS = 15;
    private static final int SPREAD_INTERVAL = 5;
    private static final int SPREAD_AMOUNT = 5;
    private static final int FORWARD_AMOUNT = 25;

    final FluidTank buffer = new FluidTank(BUFFER_CAPACITY,
            fs -> fs.getFluid().getFluidType() == CESGFluids.TELEPORT_ESSENCE.getType()
                    || fs.getFluid().getFluidType() == CESGFluids.LIQUID_EYE_OF_ENDER.getType()) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private long flowUntil;
    private boolean flowOwned;

    public GatewayFrameBlockEntity(BlockPos pos, BlockState state) {
        super(CESGBlockEntities.GATEWAY_FRAME.get(), pos, state);
    }

    static GatewayFrameBlock.FrameFuel fuelTypeOf(FluidStack stack) {
        if (stack.isEmpty())
            return GatewayFrameBlock.FrameFuel.NONE;
        return stack.getFluid().getFluidType() == CESGFluids.LIQUID_EYE_OF_ENDER.getType()
                ? GatewayFrameBlock.FrameFuel.EYE : GatewayFrameBlock.FrameFuel.ESSENCE;
    }

    /** Cosmetic flow flash: show the fuel in the conduit of an UNLIT frame while pumping continues. */
    public void showFlow(GatewayFrameBlock.FrameFuel fuel) {
        if (level == null || level.isClientSide || fuel == GatewayFrameBlock.FrameFuel.NONE)
            return;
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof GatewayFrameBlock) || state.getValue(GatewayFrameBlock.LIT))
            return; // an active (lit) ring is owned by the Core - never fight it
        flowUntil = level.getGameTime() + FLOW_LINGER_TICKS;
        flowOwned = true;
        if (state.getValue(GatewayFrameBlock.FUEL) != fuel)
            level.setBlock(worldPosition, state.setValue(GatewayFrameBlock.FUEL, fuel), Block.UPDATE_CLIENTS);
    }

    /** Called by the Core when it takes ownership of the frame's state (activate/deactivate). */
    public void clearFlow() {
        flowOwned = false;
        flowUntil = 0;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GatewayFrameBlockEntity be) {
        if (!(state.getBlock() instanceof GatewayFrameBlock))
            return;

        // Buffered fluid keeps the conduit visible unconditionally (and spreads/forwards).
        if (!be.buffer.isEmpty()) {
            if (!state.getValue(GatewayFrameBlock.LIT)) {
                GatewayFrameBlock.FrameFuel fuel = fuelTypeOf(be.buffer.getFluid());
                if (state.getValue(GatewayFrameBlock.FUEL) != fuel)
                    level.setBlock(pos, state.setValue(GatewayFrameBlock.FUEL, fuel), Block.UPDATE_CLIENTS);
                be.flowOwned = true;
                be.flowUntil = level.getGameTime() + FLOW_LINGER_TICKS;
            }
            CrossDimensionalGatewayCoreBlockEntity core = GatewayFuelHandler.findCore(level, pos);
            if (core != null)
                core.refreshFuelVisual();
            if (level.getGameTime() % SPREAD_INTERVAL == 0)
                be.moveBufferAlong(level, pos);
            return;
        }

        // Buffer empty: fade the flow flash shortly after pumping stops.
        if (!be.flowOwned || level.getGameTime() < be.flowUntil)
            return;
        be.flowOwned = false;
        if (!state.getValue(GatewayFrameBlock.LIT)
                && state.getValue(GatewayFrameBlock.FUEL) != GatewayFrameBlock.FrameFuel.NONE)
            level.setBlock(pos, state.setValue(GatewayFrameBlock.FUEL, GatewayFrameBlock.FrameFuel.NONE),
                    Block.UPDATE_CLIENTS);
        CrossDimensionalGatewayCoreBlockEntity core = GatewayFuelHandler.findCore(level, pos);
        if (core != null)
            core.refreshFuelVisual();
    }

    /** Forward into the core when it has room; otherwise creep into an emptier adjacent frame. */
    private void moveBufferAlong(Level level, BlockPos pos) {
        CrossDimensionalGatewayCoreBlockEntity core = GatewayFuelHandler.findCore(level, pos);
        FluidStack held = buffer.getFluid();
        boolean eye = fuelTypeOf(held) == GatewayFrameBlock.FrameFuel.EYE;
        // A fuelled Gateway Flux Battery owns Core refills; the frame buffer then stays put (pure
        // transit/visual) so travel drain shows cleanly on the battery, not split with parked ring fluid.
        if (core != null && !core.ringHasBatteryWithFuel(eye)) {
            int accepted = eye
                    ? core.fillEye(Math.min(FORWARD_AMOUNT, held.getAmount()), false)
                    : core.fillEssence(Math.min(FORWARD_AMOUNT, held.getAmount()), false);
            if (accepted > 0) {
                buffer.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
                return;
            }
        }
        // No core (or full): equalize into a neighboring frame so decorative runs fill visibly.
        for (Direction dir : Direction.values()) {
            if (level.getBlockEntity(pos.relative(dir)) instanceof GatewayFrameBlockEntity other) {
                FluidStack theirs = other.buffer.getFluid();
                if (!theirs.isEmpty() && !FluidStack.isSameFluidSameComponents(theirs, held))
                    continue;
                if (other.buffer.getFluidAmount() + SPREAD_AMOUNT <= buffer.getFluidAmount()) {
                    int moved = other.buffer.fill(held.copyWithAmount(SPREAD_AMOUNT),
                            IFluidHandler.FluidAction.EXECUTE);
                    if (moved > 0) {
                        buffer.drain(moved, IFluidHandler.FluidAction.EXECUTE);
                        return;
                    }
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Buffer", buffer.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        buffer.readFromNBT(registries, tag.getCompound("Buffer"));
    }
}
