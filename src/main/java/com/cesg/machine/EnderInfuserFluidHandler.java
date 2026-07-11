package com.cesg.machine;

import com.cesg.init.CESGFluids;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Exposes the Ender Infuser's two tanks to Create pipes: fill goes to the input, drain comes from the output. */
public class EnderInfuserFluidHandler implements IFluidHandler {
    private final EnderInfuserBlockEntity be;

    public EnderInfuserFluidHandler(EnderInfuserBlockEntity be) {
        this.be = be;
    }

    @Override
    public int getTanks() {
        return 2;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return tank == 0 ? be.getInput() : be.getOutput();
    }

    @Override
    public int getTankCapacity(int tank) {
        return EnderInfuserBlockEntity.TANK_CAPACITY;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        if (tank != 0)
            return false;
        return stack.getFluid().getFluidType() == CESGFluids.TELEPORT_ESSENCE.getType()
                || stack.getFluid().getFluidType() == CESGFluids.LIQUID_EYE_OF_ENDER.getType();
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return be.fillInput(resource, action.simulate());
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !FluidStack.isSameFluidSameComponents(be.getOutput(), resource))
            return FluidStack.EMPTY;
        return be.drainOutput(resource.getAmount(), action.simulate());
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return be.drainOutput(maxDrain, action.simulate());
    }
}
