package com.cesg.gateways;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import com.cesg.CESGConfig;
import com.cesg.init.CESGBlockEntities;
import com.cesg.init.CESGFluids;
import com.cesg.util.CESGLang;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.IFluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Gateway Flux Battery (Phase 7E): a fuel reservoir that Create pipes fill and that tops up a connected
 * gateway Core's tank to smooth bursty travel/Port demand. Modelled on Create's fluid tank multiblock
 * (via {@link IMultiBlockEntityContainer.Fluid} + {@link ConnectivityHandler}): batteries assemble into
 * a square-base array — up to 2×2×2 or 3×3×3 — that acts as one tank whose capacity scales with the
 * block count. Like a Create fluid tank it is single-fuel: it locks to the first fuel pumped in
 * (Teleport Essence OR Liquid Eye of Ender); build a second array to buffer the other fuel.
 */
public class GatewayFluxBatteryBlockEntity extends BlockEntity
        implements IHaveGoggleInformation, IMultiBlockEntityContainer.Fluid {

    /** Max base width (3×3). Height is capped to the width, giving 2×2×2 and 3×3×3 as the two arrays. */
    private static final int MAX_WIDTH = 3;
    private static final int REFILL_INTERVAL = 5;
    private static final int SYNC_INTERVAL = 10;
    private static final int MEMBER_SCAN_LIMIT = 32;

    /** Per-block fuel capacity; a W×W×H array holds W*W*H times this. */
    private static int capacityPerBlock() {
        return CESGConfig.batteryCapacity();
    }

    private BlockPos controller;
    private BlockPos lastKnownPos;
    private boolean updateConnectivity = true;
    private int width = 1;
    private int height = 1;
    private boolean syncDirty;

    final FluidTank tank = new FluidTank(capacityPerBlock(),
            s -> isEssence(s) || isEye(s)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            syncDirty = true;
        }
    };

    public GatewayFluxBatteryBlockEntity(BlockPos pos, BlockState state) {
        super(CESGBlockEntities.GATEWAY_FLUX_BATTERY.get(), pos, state);
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        lastKnownPos = worldPosition;
    }

    /** Pipe-facing handler: always operates on the CONTROLLER's tank so any member is a valid endpoint. */
    public IFluidHandler createFluidHandler() {
        return new BatteryFluidHandler(this);
    }

    /** The array's stored fuel (read from the controller). Empty if the controller is unresolved. */
    public FluidStack storedFluid() {
        GatewayFluxBatteryBlockEntity c = controllerBE();
        return c == null ? FluidStack.EMPTY : c.tank.getFluid();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GatewayFluxBatteryBlockEntity be) {
        be.lastKnownPos = pos;
        if (be.updateConnectivity)
            be.updateConnectivity();
        if (!be.isController())
            return;
        if (be.syncDirty && level.getGameTime() % SYNC_INTERVAL == 0) {
            be.syncDirty = false;
            be.sync();
        }
        if (level.getGameTime() % REFILL_INTERVAL == 0)
            be.topUpCore();
    }

    void updateConnectivity() {
        updateConnectivity = false;
        if (level == null || level.isClientSide)
            return;
        if (!isController())
            return;
        ConnectivityHandler.formMulti(this);
    }

    /** Push held fuel into the connected gateway Core's matching tank, up to the per-cycle budget. */
    private void topUpCore() {
        if (tank.isEmpty())
            return;
        CrossDimensionalGatewayCoreBlockEntity core = findConnectedCore();
        if (core == null)
            return;
        FluidStack held = tank.getFluid();
        boolean essence = isEssence(held);
        int coreCap = CrossDimensionalGatewayCoreBlockEntity.TANK_CAPACITY;
        int room = essence ? coreCap - core.getEssenceMb() : coreCap - core.getEyeMb();
        int budget = CESGConfig.batteryMaxDrainPerTick() * REFILL_INTERVAL;
        int move = Math.min(Math.min(budget, tank.getFluidAmount()), room);
        if (move <= 0)
            return;
        int filled = essence ? core.fillEssence(move, false) : core.fillEye(move, false);
        if (filled > 0)
            tank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
    }

    /** Any battery block in the array may touch the ring — BFS the array and resolve the Core from a member. */
    private CrossDimensionalGatewayCoreBlockEntity findConnectedCore() {
        if (level == null)
            return null;
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(worldPosition);
        visited.add(worldPosition);
        Block self = getBlockState().getBlock();
        while (!queue.isEmpty() && visited.size() <= MEMBER_SCAN_LIMIT) {
            BlockPos pos = queue.poll();
            CrossDimensionalGatewayCoreBlockEntity core = GatewayFuelHandler.findCore(level, pos);
            if (core != null)
                return core;
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (!visited.contains(next) && level.getBlockState(next).is(self)) {
                    visited.add(next);
                    queue.add(next);
                }
            }
        }
        return null;
    }

    private GatewayFluxBatteryBlockEntity controllerBE() {
        if (isController() || level == null)
            return this;
        return level.getBlockEntity(controller) instanceof GatewayFluxBatteryBlockEntity be ? be : null;
    }

    private void sync() {
        if (level != null)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    // ---- IMultiBlockEntityContainer ----

    @Override
    public Direction.Axis getMainConnectionAxis() {
        return Direction.Axis.Y;
    }

    @Override
    public int getMaxWidth() {
        return MAX_WIDTH;
    }

    @Override
    public int getMaxLength(Direction.Axis longAxis, int width) {
        // Height is capped to the base width: 2×2 stacks to 2 high, 3×3 to 3 high; a 1×1 never stacks.
        return longAxis == Direction.Axis.Y ? width : getMaxWidth();
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public void setWidth(int width) {
        this.width = width;
    }

    public int getTotalTankSize() {
        return width * width * height;
    }

    @Override
    public BlockPos getController() {
        return isController() ? worldPosition : controller;
    }

    @Override
    public boolean isController() {
        return controller == null || worldPosition.equals(controller);
    }

    @Override
    public void setController(BlockPos controller) {
        if (level != null && level.isClientSide)
            return;
        if (controller.equals(this.controller))
            return;
        this.controller = controller;
        if (level != null)
            level.invalidateCapabilities(worldPosition);
        setChanged();
        sync();
    }

    @Override
    public void removeController(boolean keepContents) {
        if (level != null && level.isClientSide)
            return;
        updateConnectivity = true;
        if (!keepContents)
            applyTankSize(1);
        controller = null;
        width = 1;
        height = 1;
        if (level != null)
            level.invalidateCapabilities(worldPosition);
        setChanged();
        sync();
    }

    @Override
    public BlockPos getLastKnownPos() {
        return lastKnownPos != null ? lastKnownPos : worldPosition;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends BlockEntity & IMultiBlockEntityContainer> T getControllerBE() {
        return (T) controllerBE();
    }

    @Override
    public void notifyMultiUpdated() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.invalidateCapabilities(worldPosition);
            sync();
        }
    }

    @Override
    public void preventConnectivityUpdate() {
        updateConnectivity = false;
    }

    // ---- IMultiBlockEntityContainer.Fluid ----

    @Override
    public boolean hasTank() {
        return true;
    }

    @Override
    public int getTankSize(int tank) {
        return capacityPerBlock();
    }

    @Override
    public void setTankSize(int tank, int blocks) {
        applyTankSize(blocks);
    }

    @Override
    public IFluidTank getTank(int tank) {
        return this.tank;
    }

    @Override
    public FluidStack getFluid(int tank) {
        return this.tank.getFluid().copy();
    }

    private void applyTankSize(int blocks) {
        tank.setCapacity(blocks * capacityPerBlock());
        int overflow = tank.getFluidAmount() - tank.getCapacity();
        if (overflow > 0)
            tank.drain(overflow, IFluidHandler.FluidAction.EXECUTE);
    }

    // ---- persistence ----

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
        if (!isController())
            tag.put("Controller", NbtUtils.writeBlockPos(controller));
        if (isController()) {
            tag.putInt("Width", width);
            tag.putInt("Height", height);
            tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        controller = null;
        if (tag.contains("Controller"))
            controller = NbtUtils.readBlockPos(tag, "Controller").orElse(null);
        if (isController()) {
            width = Math.max(1, tag.getInt("Width"));
            height = Math.max(1, tag.getInt("Height"));
            tank.setCapacity(getTotalTankSize() * capacityPerBlock());
            tank.readFromNBT(registries, tag.getCompound("Tank"));
            int overflow = tank.getFluidAmount() - tank.getCapacity();
            if (overflow > 0)
                tank.drain(overflow, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    // ---- goggles ----

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        GatewayFluxBatteryBlockEntity c = controllerBE();
        if (c == null)
            c = this;
        CESGLang.forGoggles(tooltip, "cesg.goggles.battery.title", ChatFormatting.WHITE);
        FluidStack fluid = c.tank.getFluid();
        if (fluid.isEmpty())
            CESGLang.forGoggles(tooltip, "cesg.goggles.battery.empty", ChatFormatting.GRAY);
        else
            CESGLang.forGoggles(tooltip, "cesg.goggles.battery.stored", ChatFormatting.AQUA,
                    fluid.getHoverName().getString(), c.tank.getFluidAmount(), c.tank.getCapacity());
        if (c.width > 1 || c.height > 1)
            CESGLang.forGoggles(tooltip, "cesg.goggles.battery.array", ChatFormatting.DARK_AQUA,
                    c.width, c.width, c.height);
        if (CESGConfig.gatewayPortTransferCost() > 0 && CESGConfig.batteryReserveFloor() > 0)
            CESGLang.forGoggles(tooltip, "cesg.goggles.battery.reserve", ChatFormatting.GOLD,
                    CESGConfig.batteryReserveFloor());
        return true;
    }

    static boolean isEssence(FluidStack stack) {
        return stack.getFluid().getFluidType() == CESGFluids.TELEPORT_ESSENCE.getType();
    }

    static boolean isEye(FluidStack stack) {
        return stack.getFluid().getFluidType() == CESGFluids.LIQUID_EYE_OF_ENDER.getType();
    }

    /** Pipe endpoint that always targets the controller's tank (any member block is a valid connection). */
    private record BatteryFluidHandler(GatewayFluxBatteryBlockEntity battery) implements IFluidHandler {
        private FluidTank target() {
            GatewayFluxBatteryBlockEntity c = battery.controllerBE();
            return c == null ? null : c.tank;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            FluidTank t = target();
            return t == null ? FluidStack.EMPTY : t.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            FluidTank t = target();
            return t == null ? 0 : t.getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return isEssence(stack) || isEye(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            FluidTank t = target();
            return t == null ? 0 : t.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            FluidTank t = target();
            return t == null ? FluidStack.EMPTY : t.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            FluidTank t = target();
            return t == null ? FluidStack.EMPTY : t.drain(maxDrain, action);
        }
    }
}
