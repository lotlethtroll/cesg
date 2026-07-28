package com.cesg.gateways;

import static java.lang.Math.abs;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.cesg.CESG;
import com.cesg.CESGConfig;
import com.cesg.gateways.GatewayFluxBatteryBlock.Shape;
import com.cesg.init.CESGBlockEntities;
import com.cesg.init.CESGFluids;
import com.cesg.util.CESGLang;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;

import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
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
    /**
     * Create #8920 can score a queued candidate above one block during simulation,
     * then collapse it to zero blocks during the real pass. Create calls
     * setTankSize -> setWidth -> setHeight before notifying, so this flag converts
     * that invalid result into a safe singleton at the API boundary.
     */
    private boolean forceSingletonDimensions;
    private boolean syncDirty;
    private boolean window = true;
    /**
     * Which array face shows the charge gauge. Set from the placer's facing, re-aimable with the
     * wrench. Kept off the blockstate: a FACING property would quadruple the variant table and
     * y-rotate the corner window models, which are baked per corner.
     */
    private Direction gaugeFacing = Direction.NORTH;
    private LerpedFloat fluidLevel;

    final FluidTank tank = new FluidTank(capacityPerBlock(),
            s -> isEssence(s) || isEye(s)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            syncDirty = true;
            if (level != null && level.isClientSide)
                chaseFluidLevel();
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

    public FluidTank getTankInventory() {
        return tank;
    }

    public boolean hasWindow() {
        return window;
    }

    /** The single array face carrying the charge gauge (this BE's own value; the renderer asks the controller). */
    public Direction getGaugeFacing() {
        return gaugeFacing;
    }

    /**
     * Aim only this block's gauge face (used at placement). A block that joins an existing array keeps
     * the array's gauge where it was; this value takes effect if the block later becomes a controller.
     */
    public void setOwnGaugeFacing(Direction facing) {
        if (facing.getAxis().isVertical() || gaugeFacing == facing)
            return;
        gaugeFacing = facing;
        setChanged();
        if (level != null && !level.isClientSide)
            sync();
    }

    /** Aim the array's gauge at a horizontal face. No-op on the client or for vertical faces. */
    public void setGaugeFacing(Direction facing) {
        if (facing.getAxis().isVertical() || level == null || level.isClientSide)
            return;
        GatewayFluxBatteryBlockEntity c = controllerBE();
        if (c == null || c.gaugeFacing == facing)
            return;
        c.gaugeFacing = facing;
        c.setChanged();
        c.sync();
    }

    @Nullable
    public LerpedFloat getFluidLevel() {
        return fluidLevel;
    }

    public float getFillState() {
        if (tank.getCapacity() == 0)
            return 0;
        return (float) tank.getFluidAmount() / tank.getCapacity();
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

    public static void clientTick(Level level, BlockPos pos, BlockState state, GatewayFluxBatteryBlockEntity be) {
        if (be.fluidLevel != null)
            be.fluidLevel.tickChaser();
    }

    void updateConnectivity() {
        // A stale compatibility flag must never survive into an unrelated pass.
        forceSingletonDimensions = false;
        updateConnectivity = false;
        if (level == null || level.isClientSide)
            return;
        if (!isController())
            return;
        ConnectivityHandler.formMulti(this);
    }

    public void toggleWindows() {
        GatewayFluxBatteryBlockEntity be = controllerBE();
        if (be == null)
            return;
        be.setWindows(!be.window);
    }

    public void setWindows(boolean window) {
        this.window = window;
        // Server-only: wrench can invoke this on the client; never rewrite client blockstates here.
        if (level == null || level.isClientSide)
            return;
        // Controller-authoritative rewrite of SHAPE + TOP/BOTTOM for the whole footprint.
        // Rewrite all cells after connectivity has assigned the controller dimensions so stale
        // intermediate blockstates cannot leave phantom missing lids on upper layers.
        // If the connectivity footprint is incomplete, defer — do not mutate width/height.
        if (!isArrayPrismIntact()) {
            updateConnectivity = true;
            return;
        }
        for (int yOffset = 0; yOffset < height; yOffset++) {
            for (int xOffset = 0; xOffset < width; xOffset++) {
                for (int zOffset = 0; zOffset < width; zOffset++) {
                    BlockPos pos = this.worldPosition.offset(xOffset, yOffset, zOffset);
                    BlockState blockState = level.getBlockState(pos);
                    if (!GatewayFluxBatteryBlock.isBattery(blockState))
                        continue;

                    Shape shape = Shape.PLAIN;
                    if (window) {
                        if (width == 1)
                            shape = Shape.WINDOW;
                        if (width == 2)
                            shape = xOffset == 0 ? zOffset == 0 ? Shape.WINDOW_NW : Shape.WINDOW_SW
                                    : zOffset == 0 ? Shape.WINDOW_NE : Shape.WINDOW_SE;
                        if (width == 3 && abs(abs(xOffset) - abs(zOffset)) == 1)
                            shape = Shape.WINDOW;
                    }

                    level.setBlock(pos, blockState
                                    .setValue(GatewayFluxBatteryBlock.SHAPE, shape)
                                    .setValue(GatewayFluxBatteryBlock.BOTTOM, yOffset == 0)
                                    .setValue(GatewayFluxBatteryBlock.TOP, yOffset == height - 1),
                            Block.UPDATE_CLIENTS | Block.UPDATE_INVISIBLE | Block.UPDATE_KNOWN_SHAPE);
                }
            }
        }
    }

    /**
     * True when every cell in this controller's width×width×height prism is loaded, is a battery BE,
     * and resolves to this controller. Used by lid rewrite (server) and array BER (client).
     */
    public boolean isArrayPrismIntact() {
        if (level == null || !isController())
            return false;
        if (width < 1 || height < 1)
            return false;
        BlockPos origin = worldPosition;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < width; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (!level.hasChunkAt(pos))
                        return false;
                    if (!(level.getBlockEntity(pos) instanceof GatewayFluxBatteryBlockEntity be))
                        return false;
                    if (!origin.equals(be.getController()))
                        return false;
                }
            }
        }
        return true;
    }

    /** Request a ConnectivityHandler re-form on the next server tick (controller only). */
    public void scheduleConnectivityUpdate() {
        updateConnectivity = true;
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

    private void chaseFluidLevel() {
        float fill = getFillState();
        if (fluidLevel == null)
            fluidLevel = LerpedFloat.linear().startWithValue(fill);
        fluidLevel.chase(fill, 0.5f, Chaser.EXP);
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
        this.height = forceSingletonDimensions ? 1 : Math.max(1, height);
        forceSingletonDimensions = false;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public void setWidth(int width) {
        if (forceSingletonDimensions) {
            CESG.LOGGER.warn("Create connectivity returned zero blocks for Gateway Flux Battery at {}; "
                    + "forcing requested {}x? dimensions to 1x1x1", worldPosition, width);
            this.width = 1;
            return;
        }
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
        forceSingletonDimensions = false;
        updateConnectivity = true;
        if (!keepContents)
            applyTankSize(1);
        controller = null;
        width = 1;
        height = 1;

        BlockState state = getBlockState();
        if (GatewayFluxBatteryBlock.isBattery(state) && level != null) {
            state = state.setValue(GatewayFluxBatteryBlock.BOTTOM, true);
            state = state.setValue(GatewayFluxBatteryBlock.TOP, true);
            state = state.setValue(GatewayFluxBatteryBlock.SHAPE, window ? Shape.WINDOW : Shape.PLAIN);
            level.setBlock(worldPosition, state,
                    Block.UPDATE_CLIENTS | Block.UPDATE_INVISIBLE | Block.UPDATE_KNOWN_SHAPE);
        }

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
        BlockState state = getBlockState();
        if (GatewayFluxBatteryBlock.isBattery(state) && level != null) {
            // Use *this* BE's height: ConnectivityHandler sets it on each part before notify.
            // Reading the controller's height here is wrong — the controller is updated last,
            // so parts would still see height=1 and never get TOP=true on upper layers.
            BlockPos ctrl = getController();
            state = state.setValue(GatewayFluxBatteryBlock.BOTTOM, ctrl.getY() == getBlockPos().getY());
            state = state.setValue(GatewayFluxBatteryBlock.TOP,
                    ctrl.getY() + height - 1 == getBlockPos().getY());
            level.setBlock(getBlockPos(), state,
                    Block.UPDATE_CLIENTS | Block.UPDATE_INVISIBLE | Block.UPDATE_KNOWN_SHAPE);
        }
        if (isController())
            setWindows(window); // also rewrites TOP/BOTTOM for the full footprint
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

    @Override
    public void setExtraData(@Nullable Object data) {
        if (data instanceof Boolean b)
            window = b;
    }

    @Override
    @Nullable
    public Object getExtraData() {
        return window;
    }

    @Override
    public Object modifyExtraData(Object data) {
        if (data instanceof Boolean windows)
            return windows || window;
        return data;
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
        forceSingletonDimensions = blocks <= 0;
        applyTankSize(Math.max(1, blocks));
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
        // Saved on members too: a member promoted to controller keeps the face it was placed against.
        tag.putInt("GaugeFacing", gaugeFacing.get3DDataValue());
        if (isController()) {
            tag.putInt("Width", width);
            tag.putInt("Height", height);
            tag.putBoolean("Window", window);
            tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        BlockPos previousController = controller;
        int previousWidth = width;
        int previousHeight = height;
        controller = null;
        if (tag.contains("Controller"))
            controller = NbtUtils.readBlockPos(tag, "Controller").orElse(null);
        if (tag.contains("GaugeFacing")) {
            Direction saved = Direction.from3DDataValue(tag.getInt("GaugeFacing"));
            if (saved.getAxis().isHorizontal())
                gaugeFacing = saved;
        }
        if (isController()) {
            width = Math.max(1, tag.getInt("Width"));
            height = Math.max(1, tag.getInt("Height"));
            if (tag.contains("Window"))
                window = tag.getBoolean("Window");
            tank.setCapacity(getTotalTankSize() * capacityPerBlock());
            tank.readFromNBT(registries, tag.getCompound("Tank"));
            int overflow = tank.getFluidAmount() - tank.getCapacity();
            if (overflow > 0)
                tank.drain(overflow, IFluidHandler.FluidAction.EXECUTE);
            chaseFluidLevel();
        } else {
            // Members do not persist size; clear any stale controller dimensions from a prior role.
            width = 1;
            height = 1;
        }
        if (level != null && level.isClientSide
                && (!Objects.equals(previousController, controller)
                        || previousWidth != width || previousHeight != height))
            refreshClientModelDataAround();
    }

    /**
     * Connected textures and face culling depend on neighbouring BEs' controller
     * positions. Refresh both model data and chunk meshes when a client packet
     * changes this BE's controller or array dimensions.
     */
    private void refreshClientModelDataAround() {
        refreshClientModelDataAt(worldPosition);
        for (Direction direction : Direction.values())
            refreshClientModelDataAt(worldPosition.relative(direction));
    }

    private void refreshClientModelDataAt(BlockPos pos) {
        if (level == null || !level.hasChunkAt(pos))
            return;
        if (level.getBlockEntity(pos) instanceof GatewayFluxBatteryBlockEntity battery)
            battery.requestModelDataUpdate();
        BlockState state = level.getBlockState(pos);
        level.setBlocksDirty(pos, state, state);
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
