package com.cesg.gateways;

import java.util.List;

import com.cesg.CESGConfig;
import com.cesg.init.CESGBlockEntities;
import com.cesg.init.CESGFluids;
import com.cesg.util.CESGLang;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Gateway Flux Battery (Phase 7E): a large reservoir of the two gateway fuels that Create pipes/pumps
 * fill, placed beside a gateway ring. Every few ticks it tops up the connected Core's small tanks from
 * its reserves, so bursty travel/port/bridge demand never stalls the gateway while a pump can't keep up.
 *
 * <p>It only ever <em>pushes</em> fuel into the Core (never reads or changes the Core's consume logic),
 * so it is decoupled from gateway travel entirely. Teleport Essence and Liquid Eye of Ender are kept in
 * separate tanks; each feeds the matching Core tank.
 */
public class GatewayFluxBatteryBlockEntity extends BlockEntity implements IHaveGoggleInformation {
    /** How often (ticks) the battery attempts to top up its Core — the ring scan is bounded, not free. */
    private static final int REFILL_INTERVAL = 5;
    /** Client sync cadence for the goggle readout. */
    private static final int SYNC_INTERVAL = 10;

    private boolean syncDirty;

    private final int capacity = CESGConfig.batteryCapacity();

    final FluidTank essenceTank = new FluidTank(capacity, GatewayFluxBatteryBlockEntity::isEssence) {
        @Override
        protected void onContentsChanged() {
            contentsChanged();
        }
    };
    final FluidTank eyeTank = new FluidTank(capacity, GatewayFluxBatteryBlockEntity::isEye) {
        @Override
        protected void onContentsChanged() {
            contentsChanged();
        }
    };

    public GatewayFluxBatteryBlockEntity(BlockPos pos, BlockState state) {
        super(CESGBlockEntities.GATEWAY_FLUX_BATTERY.get(), pos, state);
    }

    private void contentsChanged() {
        setChanged();
        syncDirty = true;
    }

    /** Pipe-facing handler: fill either fuel, drain to reclaim it. */
    public IFluidHandler createFluidHandler() {
        return new BatteryFluidHandler(this);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GatewayFluxBatteryBlockEntity be) {
        if (be.syncDirty && level.getGameTime() % SYNC_INTERVAL == 0) {
            be.syncDirty = false;
            level.sendBlockUpdated(pos, state, state, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
        if (level.getGameTime() % REFILL_INTERVAL == 0)
            be.topUpCore();
    }

    /** Move fuel from the battery reserves into the connected Core's tanks, up to the per-cycle budget. */
    private void topUpCore() {
        if (!(level instanceof ServerLevel) || (essenceTank.isEmpty() && eyeTank.isEmpty()))
            return;
        CrossDimensionalGatewayCoreBlockEntity core = GatewayFuelHandler.findCore(level, worldPosition);
        if (core == null)
            return;

        int budget = CESGConfig.batteryMaxDrainPerTick() * REFILL_INTERVAL;

        int coreCap = CrossDimensionalGatewayCoreBlockEntity.TANK_CAPACITY;
        int essenceMove = Math.min(Math.min(budget, essenceTank.getFluidAmount()), coreCap - core.getEssenceMb());
        if (essenceMove > 0) {
            int filled = core.fillEssence(essenceMove, false);
            if (filled > 0) {
                essenceTank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                budget -= filled;
            }
        }

        int eyeMove = Math.min(Math.min(budget, eyeTank.getFluidAmount()), coreCap - core.getEyeMb());
        if (eyeMove > 0) {
            int filled = core.fillEye(eyeMove, false);
            if (filled > 0)
                eyeTank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("EssenceTank", essenceTank.writeToNBT(registries, new CompoundTag()));
        tag.put("EyeTank", eyeTank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        essenceTank.readFromNBT(registries, tag.getCompound("EssenceTank"));
        eyeTank.readFromNBT(registries, tag.getCompound("EyeTank"));
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        CESGLang.forGoggles(tooltip, "cesg.goggles.battery.title", ChatFormatting.WHITE);
        CESGLang.forGoggles(tooltip, "cesg.goggles.battery.essence", ChatFormatting.AQUA,
                essenceTank.getFluidAmount(), capacity);
        CESGLang.forGoggles(tooltip, "cesg.goggles.battery.eye", ChatFormatting.LIGHT_PURPLE,
                eyeTank.getFluidAmount(), capacity);
        return true;
    }

    static boolean isEssence(FluidStack stack) {
        return stack.getFluid().getFluidType() == CESGFluids.TELEPORT_ESSENCE.getType();
    }

    static boolean isEye(FluidStack stack) {
        return stack.getFluid().getFluidType() == CESGFluids.LIQUID_EYE_OF_ENDER.getType();
    }

    /** Tank 0: Teleport Essence. Tank 1: Liquid Eye of Ender. Fill routes by fluid; drain reclaims. */
    private record BatteryFluidHandler(GatewayFluxBatteryBlockEntity battery) implements IFluidHandler {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? battery.essenceTank.getFluid() : battery.eyeTank.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return battery.capacity;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 ? isEssence(stack) : isEye(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (isEssence(resource))
                return battery.essenceTank.fill(resource, action);
            if (isEye(resource))
                return battery.eyeTank.fill(resource, action);
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty())
                return FluidStack.EMPTY;
            if (FluidStack.isSameFluidSameComponents(resource, battery.essenceTank.getFluid()))
                return battery.essenceTank.drain(resource.getAmount(), action);
            if (FluidStack.isSameFluidSameComponents(resource, battery.eyeTank.getFluid()))
                return battery.eyeTank.drain(resource.getAmount(), action);
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (!battery.essenceTank.isEmpty())
                return battery.essenceTank.drain(maxDrain, action);
            return battery.eyeTank.drain(maxDrain, action);
        }
    }
}
