package com.cesg.gateways;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

import com.cesg.gateways.teleport.GatewayPartner;
import com.cesg.gateways.teleport.GatewayPortalShape;
import com.cesg.gateways.teleport.GatewaySideState;
import com.cesg.gateways.teleport.TeleportResolver;
import com.cesg.init.CESGBlockEntities;
import com.cesg.init.CESGRegistration;
import com.cesg.init.CESGSounds;
import com.cesg.util.CESGLang;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class CrossDimensionalGatewayCoreBlockEntity extends KineticBlockEntity {
    public static final int TANK_CAPACITY = 4000;
    public static final int ESSENCE_FUEL_MB = 1000;
    public static final int LIQUID_EYE_FUEL_MB = 1000;

    /** Fuel drained per teleport — configurable (Phase 6C). */
    public static int travelCost() {
        return com.cesg.CESGConfig.gatewayTravelCost();
    }

    private static int teleportCooldown() {
        return com.cesg.CESGConfig.gatewayTeleportCooldown();
    }

    /** Number of selectable destination channels (Phase 6A multi-binding). */
    public static final int CHANNEL_COUNT = 16;

    /** Channel -> bound partner. Only bound channels are stored. */
    private final java.util.Map<Integer, GatewayPartner> bindings = new java.util.HashMap<>();
    /** Channel -> routing filter (Phase 7B). Only channels whose filter has been edited are stored. */
    private final java.util.Map<Integer, ChannelFilter> channelFilters = new java.util.HashMap<>();
    /** Fan-out routing: distribute items to the channel whose filter accepts them, not just the active one. */
    private boolean routeMode;
    private int activeChannel;
    /** Player-given label for THIS gateway; carried into crystals and partner bindings (Phase 6A UX). */
    private String gatewayName = "";

    /**
     * Partner status for goggles/UI, synced. A cross-dimension partner is usually in UNLOADED chunks
     * (nobody there) — that is NOT a travel blocker (travel loads the chunk), so it must display as
     * "unknown", never as the alarming "offline".
     */
    public static final int PARTNER_UNKNOWN = 0;
    public static final int PARTNER_LIVE = 1;
    public static final int PARTNER_OFFLINE = 2;
    private int partnerStatus = PARTNER_UNKNOWN;

    /** Opt-in chunk loading: keep this core's chunk + the bound partner's chunk ticking (6A). */
    private boolean chunkLoading;
    /** The pair currently force-loaded (server side), so tickets release exactly once on change. */
    @org.jetbrains.annotations.Nullable
    private net.minecraft.core.GlobalPos forcedTarget;
    private int essenceMb;
    private int eyeMb;
    /** Interior cells we have turned into portal blocks; persisted so we can always clean them up. */
    private List<BlockPos> activePortalCells = List.of();
    /** Ring frame cells we have driven LIT; persisted so they can always be un-lit. */
    private List<BlockPos> activeFrameCells = List.of();

    public CrossDimensionalGatewayCoreBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(20);
    }

    public CrossDimensionalGatewayCoreBlockEntity(BlockPos pos, BlockState state) {
        this(CESGBlockEntities.CROSS_DIMENSIONAL_GATEWAY_CORE.get(), pos, state);
    }

    /** The partner bound on the currently active channel ({@link GatewayPartner#EMPTY} when unbound). */
    public GatewayPartner getPartner() {
        return getBinding(activeChannel);
    }

    /** Binds the active channel. Kept for callers that predate multi-binding. */
    public void setPartner(GatewayPartner partner) {
        setBinding(activeChannel, partner);
    }

    public GatewayPartner getBinding(int channel) {
        return bindings.getOrDefault(channel, GatewayPartner.EMPTY);
    }

    public void setBinding(int channel, GatewayPartner partner) {
        if (partner.isBound())
            bindings.put(channel, partner);
        else
            bindings.remove(channel);
        applyChunkLoading();
        notifyUpdate();
    }

    public int getActiveChannel() {
        return activeChannel;
    }

    public void setActiveChannel(int channel) {
        int clamped = Math.floorMod(channel, CHANNEL_COUNT);
        if (clamped == activeChannel)
            return;
        activeChannel = clamped;
        partnerStatus = PARTNER_UNKNOWN;
        applyChunkLoading();
        refreshPortalState(); // recolor glass + frames to the new destination's fuel now, not next lazyTick
        notifyUpdate();
    }

    // ---- Fan-out routing (Phase 7B) --------------------------------------------------------------

    /** Route mode, honouring the server's fan-out allow toggle (off = single active channel). */
    public boolean isRouteMode() {
        return routeMode && com.cesg.CESGConfig.gatewayFanOutAllowed();
    }

    public void setRouteMode(boolean value) {
        if (routeMode == value)
            return;
        routeMode = value;
        notifyUpdate();
    }

    /** The editable filter for {@code channel}, created on first edit (used by the filter GUI). */
    public ChannelFilter getOrCreateChannelFilter(int channel) {
        return channelFilters.computeIfAbsent(Math.floorMod(channel, CHANNEL_COUNT),
                c -> new ChannelFilter(this::onChannelFilterChanged));
    }

    /** True when {@code channel} has a non-default filter (picker indicator). */
    public boolean hasChannelFilter(int channel) {
        ChannelFilter filter = channelFilters.get(Math.floorMod(channel, CHANNEL_COUNT));
        return filter != null && filter.hasContent();
    }

    public boolean isChannelBlacklist(int channel) {
        ChannelFilter filter = channelFilters.get(Math.floorMod(channel, CHANNEL_COUNT));
        return filter != null && filter.isBlacklist();
    }

    public void toggleChannelBlacklist(int channel) {
        ChannelFilter filter = getOrCreateChannelFilter(channel);
        filter.setBlacklist(!filter.isBlacklist());
        onChannelFilterChanged();
    }

    private void onChannelFilterChanged() {
        notifyUpdate();
    }

    /**
     * The bound channel that should receive {@code sample}: in route mode, the first bound channel
     * whose filter accepts it (deterministic, so items never flip-flop); otherwise the active channel.
     * Returns -1 when nothing accepts it (route mode) or the active channel is unbound.
     */
    public int routeChannel(ItemStack sample) {
        if (!isRouteMode())
            return getPartner().isBound() ? activeChannel : -1;
        for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
            if (!getBinding(channel).isBound())
                continue;
            ChannelFilter filter = channelFilters.get(channel);
            if (filter != null && filter.accepts(sample))
                return channel;
        }
        return -1;
    }

    public int getPartnerStatus() {
        return partnerStatus;
    }

    public boolean isChunkLoading() {
        return chunkLoading;
    }

    public void setChunkLoading(boolean value) {
        if (!com.cesg.CESGConfig.gatewayChunkLoadingAllowed())
            value = false;
        if (value == chunkLoading)
            return;
        chunkLoading = value;
        applyChunkLoading();
        notifyUpdate();
    }

    /**
     * Reconciles chunk tickets with the CURRENT active binding. Called on toggle, channel switch,
     * rebinding, load, and removal — switching destinations mid-transfer releases the old pair's
     * tickets and forces the new pair, so ports immediately serve the new destination and the old
     * one is free to unload.
     */
    private void applyChunkLoading() {
        if (!(level instanceof ServerLevel serverLevel))
            return;
        GatewayPartner partner = getPartner();
        net.minecraft.core.GlobalPos desired =
                chunkLoading && partner.isBound() && com.cesg.CESGConfig.gatewayChunkLoadingAllowed()
                        ? net.minecraft.core.GlobalPos.of(partner.dimension(), partner.position())
                        : null;
        if (java.util.Objects.equals(desired, forcedTarget))
            return;
        if (forcedTarget != null)
            GatewayChunkLoader.setForced(serverLevel.getServer(), serverLevel, worldPosition, forcedTarget, false);
        forcedTarget = desired;
        if (desired != null)
            GatewayChunkLoader.setForced(serverLevel.getServer(), serverLevel, worldPosition, desired, true);
        setChanged();
    }

    public String getGatewayName() {
        return gatewayName;
    }

    public void setGatewayName(String name) {
        String clamped = name == null ? "" : (name.length() > 32 ? name.substring(0, 32) : name).trim();
        if (clamped.equals(gatewayName))
            return;
        gatewayName = clamped;
        notifyUpdate();
    }

    public boolean addEssence(int amount) {
        int before = essenceMb;
        essenceMb = Math.min(TANK_CAPACITY, essenceMb + amount);
        if (essenceMb != before) {
            notifyUpdate();
            updateCoreFuelVisual();
        }
        return essenceMb > before;
    }

    public boolean addEye(int amount) {
        int before = eyeMb;
        eyeMb = Math.min(TANK_CAPACITY, eyeMb + amount);
        if (eyeMb != before) {
            notifyUpdate();
            updateCoreFuelVisual();
        }
        return eyeMb > before;
    }

    public int getEssenceMb() {
        return essenceMb;
    }

    public int getEyeMb() {
        return eyeMb;
    }

    /** Fluid-handler fill: returns the amount accepted (0 when simulating overflow). */
    public int fillEssence(int amount, boolean simulate) {
        int filled = Math.min(amount, TANK_CAPACITY - essenceMb);
        if (filled > 0 && !simulate) {
            essenceMb += filled;
            notifyUpdate();
            updateCoreFuelVisual();
        }
        return Math.max(0, filled);
    }

    public int fillEye(int amount, boolean simulate) {
        int filled = Math.min(amount, TANK_CAPACITY - eyeMb);
        if (filled > 0 && !simulate) {
            eyeMb += filled;
            notifyUpdate();
            updateCoreFuelVisual();
        }
        return Math.max(0, filled);
    }

    public int drainEssence(int amount, boolean simulate) {
        int drained = Math.min(amount, essenceMb);
        if (drained > 0 && !simulate) {
            essenceMb -= drained;
            notifyUpdate();
            updateCoreFuelVisual();
        }
        return Math.max(0, drained);
    }

    public int drainEye(int amount, boolean simulate) {
        int drained = Math.min(amount, eyeMb);
        if (drained > 0 && !simulate) {
            eyeMb -= drained;
            notifyUpdate();
            updateCoreFuelVisual();
        }
        return Math.max(0, drained);
    }

    /** Cross-dimension travel costs Liquid Eye of Ender; same-dimension travel costs Teleport Essence. */
    public boolean partnerIsCrossDimension() {
        GatewayPartner partner = getPartner();
        return partner.isBound() && level != null && !partner.dimension().equals(level.dimension());
    }

    private int requiredFuel() {
        return partnerIsCrossDimension() ? eyeMb : essenceMb;
    }

    public boolean canTravel() {
        return getTravelBlockReason() == null;
    }

    public boolean hasValidFrame() {
        return level != null && GatewayPortalShape.detect(level, worldPosition).isPresent();
    }

    /** @return translation key suffix for cesg.gateway.*, or null when travel is allowed */
    public String getTravelBlockReason() {
        if (getSpeed() == 0)
            return "unpowered";
        if (!getPartner().isBound())
            return "unbound";
        if (requiredFuel() < travelCost())
            return "need_fuel";
        return null;
    }

    private boolean consumeFuel() {
        if (requiredFuel() < travelCost())
            return false;
        if (partnerIsCrossDimension())
            eyeMb -= travelCost();
        else
            essenceMb -= travelCost();
        notifyUpdate();
        updateCoreFuelVisual();
        return true;
    }

    /**
     * Fuel gate for automation (Gateway Ports now, the Storage Bridge later). Charges {@code cost} of the
     * active fuel, but respects the Gateway Flux Battery reserve: while a battery of the relevant fuel (or
     * a dry one) is on the ring, automation may only draw the combined Core+battery fuel down to
     * {@link CESGConfig#batteryReserveFloor()} — that charge is left for player travel, which is never
     * gated ({@link #consumeFuel()} ignores this). With no battery on the ring it is a plain fuel spend.
     *
     * @return true if the cost was paid and the transfer may proceed; false to pause automation this tick.
     */
    public boolean tryConsumeAutomationFuel(int cost) {
        if (cost <= 0)
            return true;
        boolean crossDim = partnerIsCrossDimension();
        int coreFuel = crossDim ? eyeMb : essenceMb;

        long batteryFuel = 0;
        boolean batteryPresent = false;
        for (GatewayFluxBatteryBlockEntity battery : scanRingBatteries()) {
            net.neoforged.neoforge.fluids.FluidStack fluid = battery.storedFluid();
            if (fluid.isEmpty()) {
                batteryPresent = true; // a dry battery still gates — that is the safety the reserve provides
            } else if (crossDim ? GatewayFluxBatteryBlockEntity.isEye(fluid)
                    : GatewayFluxBatteryBlockEntity.isEssence(fluid)) {
                batteryPresent = true;
                batteryFuel += fluid.getAmount();
            }
        }

        if (batteryPresent && (coreFuel + batteryFuel) - cost < com.cesg.CESGConfig.batteryReserveFloor())
            return false; // protect the reserve for travel
        if (coreFuel < cost)
            return false; // Core not yet topped up this tick — the battery refills it, retry next flush

        if (crossDim)
            eyeMb -= cost;
        else
            essenceMb -= cost;
        notifyUpdate();
        updateCoreFuelVisual();
        return true;
    }

    /**
     * True when a Gateway Flux Battery on the ring currently holds the given fuel. While one does, it is
     * the authoritative source that refills the Core, so the Gateway Frame transit buffers stop feeding
     * the Core (travel drain then shows cleanly on the battery instead of being split with parked ring
     * fluid). A dry battery does not count — the ring stays a fallback so an empty battery never bricks
     * travel while the frames still hold fuel.
     */
    public boolean ringHasBatteryWithFuel(boolean eye) {
        for (GatewayFluxBatteryBlockEntity battery : scanRingBatteries()) {
            net.neoforged.neoforge.fluids.FluidStack fluid = battery.storedFluid();
            if (fluid.isEmpty())
                continue;
            if (eye ? GatewayFluxBatteryBlockEntity.isEye(fluid) : GatewayFluxBatteryBlockEntity.isEssence(fluid))
                return true;
        }
        return false;
    }

    /**
     * A ring block this Core's own-ring walks may traverse: frames, and THIS Core — but never another
     * Core. Stopping at a foreign Core keeps two gateways that share ring blocks from bleeding into each
     * other's fuel/battery scans (each owns its own side of the shared run). {@code start != this} is a
     * foreign Core and acts as a boundary.
     */
    private boolean isOwnRingBlock(BlockPos pos) {
        return GatewayFuelHandler.isOwnRingBlock(level, pos, worldPosition);
    }

    /** Unique Gateway Flux Battery controllers whose array touches this gateway ring. */
    private java.util.Collection<GatewayFluxBatteryBlockEntity> scanRingBatteries() {
        java.util.Map<BlockPos, GatewayFluxBatteryBlockEntity> controllers = new java.util.HashMap<>();
        if (level == null)
            return controllers.values();
        java.util.Set<BlockPos> visited = new java.util.HashSet<>();
        java.util.Queue<BlockPos> queue = new java.util.ArrayDeque<>();
        queue.add(worldPosition);
        visited.add(worldPosition);
        while (!queue.isEmpty() && visited.size() <= 64) {
            BlockPos pos = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (level.getBlockEntity(next) instanceof GatewayFluxBatteryBlockEntity battery) {
                    GatewayFluxBatteryBlockEntity controller = battery.getControllerBE();
                    if (controller != null)
                        controllers.putIfAbsent(controller.getBlockPos(), controller);
                }
                if (!visited.contains(next) && isOwnRingBlock(next)) {
                    visited.add(next);
                    queue.add(next);
                }
            }
        }
        return controllers.values();
    }

    public GatewaySideState createSideState() {
        return new GatewaySideState(level.dimension(), worldPosition, getSpeed() != 0, requiredFuel() >= travelCost(),
                level.dimension().equals(Level.END));
    }

    @Override
    public void initialize() {
        super.initialize();
        // On load the Core re-announces itself to its ring so frame fuel-handler caches (and the Create
        // pipes attached to them) re-resolve — they can't track the Core's lifecycle on their own.
        if (level != null && !level.isClientSide) {
            GatewayFuelHandler.invalidateRing(level, worldPosition);
            applyChunkLoading();
            updateCoreFuelVisual();
        }
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level == null || level.isClientSide)
            return;
        refreshPortalState();
        updatePartnerLiveness();
    }

    /**
     * Re-evaluate portal shape/fuel/power and repaint the Core glass + frame conduits. Frames are lit
     * with the ACTIVE channel's fuel, so calling this on a channel switch recolors the ring to the new
     * destination's fuel immediately instead of lagging a lazyTick.
     */
    private void refreshPortalState() {
        if (level == null || level.isClientSide)
            return;
        Optional<GatewayPortalShape> shape = GatewayPortalShape.detect(level, worldPosition);
        if (shape.isPresent() && canTravel())
            activate(shape.get());
        else
            deactivate();
        updateCoreFuelVisual();
    }

    /** Refreshes the eye texture when transit fluid moves through connected frames into an empty tank. */
    public void refreshFuelVisual() {
        if (level != null && !level.isClientSide)
            updateCoreFuelVisual();
    }

    /** Polls the active partner's powered/fueled state without force-loading its chunk. */
    private void updatePartnerLiveness() {
        int status = PARTNER_UNKNOWN;
        GatewayPartner partner = getPartner();
        if (partner.isBound() && level instanceof ServerLevel serverLevel) {
            ServerLevel partnerLevel = serverLevel.getServer().getLevel(partner.dimension());
            if (partnerLevel == null) {
                status = PARTNER_OFFLINE; // dimension itself is gone — genuinely broken binding
            } else if (partnerLevel.isLoaded(partner.position())) {
                GatewaySideState side = partner.resolve(serverLevel.getServer());
                status = side.powered() && side.fueled() ? PARTNER_LIVE : PARTNER_OFFLINE;
            }
            // else: chunk unloaded -> UNKNOWN; travel will load it, so this is not a fault state.
        }
        setPartnerStatus(status);
    }

    private void setPartnerStatus(int status) {
        if (status == partnerStatus)
            return;
        partnerStatus = status;
        sendData();
    }

    /**
     * Glass eye shows the fuel in the tanks, or fluid in transit on the ring when the tanks are empty.
     * Liquid Eye of Ender wins when both tanks hold fuel.
     */
    /** The fuel the CURRENTLY-ACTIVE channel travels on — Eye (cross-dimension) or Essence (same). */
    public GatewayFrameBlock.FrameFuel activeFuelType() {
        return partnerIsCrossDimension() ? GatewayFrameBlock.FrameFuel.EYE : GatewayFrameBlock.FrameFuel.ESSENCE;
    }

    /**
     * What the Core's glass shows: the ACTIVE channel's fuel when the Core (or ring transit) holds any,
     * so the visual follows the destination — a same-dim channel reads Essence even while an Eye tank is
     * full. Falls back to the other stored fuel, then to whatever is in transit through the frames.
     */
    private GatewayFrameBlock.FrameFuel displayFuel() {
        boolean crossDim = partnerIsCrossDimension();
        GatewayFrameBlock.FrameFuel active = crossDim ? GatewayFrameBlock.FrameFuel.EYE
                : GatewayFrameBlock.FrameFuel.ESSENCE;
        GatewayFrameBlock.FrameFuel other = crossDim ? GatewayFrameBlock.FrameFuel.ESSENCE
                : GatewayFrameBlock.FrameFuel.EYE;
        if ((crossDim ? eyeMb : essenceMb) > 0)
            return active;
        if ((crossDim ? essenceMb : eyeMb) > 0)
            return other;
        return transitFuel(active, other);
    }

    /** Scans connected frame buffers for fuel being pumped toward this core (active fuel preferred). */
    private GatewayFrameBlock.FrameFuel transitFuel(GatewayFrameBlock.FrameFuel active,
            GatewayFrameBlock.FrameFuel other) {
        if (level == null)
            return GatewayFrameBlock.FrameFuel.NONE;
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(worldPosition);
        visited.add(worldPosition);
        boolean essence = false;
        boolean eye = false;
        while (!queue.isEmpty() && visited.size() <= 64) {
            BlockPos pos = queue.poll();
            if (level.getBlockEntity(pos) instanceof GatewayFrameBlockEntity frame && !frame.buffer.isEmpty()) {
                if (GatewayFrameBlockEntity.fuelTypeOf(frame.buffer.getFluid()) == GatewayFrameBlock.FrameFuel.EYE)
                    eye = true;
                else
                    essence = true;
            }
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (!visited.contains(next) && isOwnRingBlock(next)) {
                    visited.add(next);
                    queue.add(next);
                }
            }
        }
        boolean hasActive = active == GatewayFrameBlock.FrameFuel.EYE ? eye : essence;
        boolean hasOther = other == GatewayFrameBlock.FrameFuel.EYE ? eye : essence;
        if (hasActive)
            return active;
        if (hasOther)
            return other;
        return GatewayFrameBlock.FrameFuel.NONE;
    }

    /** Glass eye shows fluid while the core holds fuel or is receiving it; empty (clear glass) otherwise. */
    private void updateCoreFuelVisual() {
        GatewayFrameBlock.FrameFuel fuel = displayFuel();
        boolean hasFuel = fuel != GatewayFrameBlock.FrameFuel.NONE;
        BlockState state = getBlockState();
        BlockState next = state;
        if (state.hasProperty(CrossDimensionalGatewayCoreBlock.LIT))
            next = next.setValue(CrossDimensionalGatewayCoreBlock.LIT, hasFuel);
        if (state.hasProperty(CrossDimensionalGatewayCoreBlock.FUEL))
            next = next.setValue(CrossDimensionalGatewayCoreBlock.FUEL, hasFuel ? fuel : GatewayFrameBlock.FrameFuel.NONE);
        if (next != state)
            level.setBlock(worldPosition, next, Block.UPDATE_CLIENTS);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide || activePortalCells.isEmpty())
            return;
        AABB box = interiorBox();
        if (box == null)
            return;
        // Players, mobs and dropped items all pass through like a nether portal. The vanilla portal
        // cooldown prevents instant ping-pong and survives the cross-dimension entity swap.
        for (Entity entity : level.getEntitiesOfClass(Entity.class, box, e -> !e.isOnPortalCooldown())) {
            entity.setPortalCooldown(teleportCooldown());
            travel(entity);
        }
    }

    private void travel(Entity entity) {
        if (!(level instanceof ServerLevel serverLevel))
            return;
        if (getTravelBlockReason() != null)
            return;
        // Capture the side state (fueled/powered) BEFORE spending fuel, so the resolver doesn't deny
        // the trip when the traveler is spending the last charge.
        GatewaySideState thisSide = createSideState();
        if (!consumeFuel())
            return;
        // Travel resolves (and loads) the partner anyway — use that knowledge to refresh the goggle status.
        GatewaySideState partnerSide = getPartner().resolve(serverLevel.getServer());
        setPartnerStatus(partnerSide.powered() && partnerSide.fueled() ? PARTNER_LIVE : PARTNER_OFFLINE);
        TeleportResolver.teleportThroughPortal(entity, thisSide, partnerSide);
    }

    private void activate(GatewayPortalShape shape) {
        boolean opening = activePortalCells.isEmpty();
        BlockState portal = CESGRegistration.GATEWAY_PORTAL.get().defaultBlockState()
                .setValue(GatewayPortalBlock.AXIS, shape.axis);
        boolean changed = false;
        for (BlockPos old : activePortalCells)
            if (!shape.interior.contains(old) && isPortal(old)) {
                level.setBlock(old, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                changed = true;
            }
        for (BlockPos cell : shape.interior)
            if (level.getBlockState(cell).isAir()) {
                level.setBlock(cell, portal, Block.UPDATE_ALL);
                changed = true;
            }
        activePortalCells = new ArrayList<>(shape.interior);
        GatewayFrameBlock.FrameFuel fuel = partnerIsCrossDimension()
                ? GatewayFrameBlock.FrameFuel.EYE : GatewayFrameBlock.FrameFuel.ESSENCE;
        for (BlockPos frameCell : shape.frame)
            changed |= setFrameFuel(frameCell, fuel);
        activeFrameCells = new ArrayList<>(shape.frame);
        if (changed) {
            notifyUpdate();
            if (opening && level instanceof ServerLevel serverLevel) {
                serverLevel.playSound(null, worldPosition, CESGSounds.PORTAL_OPEN.value(), SoundSource.BLOCKS,
                        0.9f, 1.0f);
                serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                        worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                        24, 0.45, 0.7, 0.45, 0.08);
            }
        }
    }

    private void deactivate() {
        if (activePortalCells.isEmpty() && activeFrameCells.isEmpty())
            return;
        for (BlockPos cell : activePortalCells)
            if (isPortal(cell))
                level.setBlock(cell, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        for (BlockPos frameCell : activeFrameCells)
            setFrameFuel(frameCell, GatewayFrameBlock.FrameFuel.NONE);
        activePortalCells = List.of();
        activeFrameCells = List.of();
        notifyUpdate();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, worldPosition, CESGSounds.PORTAL_CLOSE.value(), SoundSource.BLOCKS,
                    0.75f, 1.0f);
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                    10, 0.35, 0.45, 0.35, 0.02);
        }
    }

    /** Called by the block on removal so a broken Core never leaves orphaned portal/lit-frame blocks. */
    public void clearPortal() {
        if (level != null) {
            deactivate();
            chunkLoading = false;
            applyChunkLoading();
        }
    }

    private boolean setFrameFuel(BlockPos pos, GatewayFrameBlock.FrameFuel fuel) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(CESGRegistration.GATEWAY_FRAME.get()))
            return false;
        boolean lit = fuel != GatewayFrameBlock.FrameFuel.NONE;
        if (level.getBlockEntity(pos) instanceof GatewayFrameBlockEntity frame)
            frame.clearFlow(); // the core owns the state now; cosmetic flow must not revert it
        if (state.getValue(GatewayFrameBlock.FUEL) != fuel || state.getValue(GatewayFrameBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(GatewayFrameBlock.FUEL, fuel)
                    .setValue(GatewayFrameBlock.LIT, lit), Block.UPDATE_ALL);
            return true;
        }
        return false;
    }

    private boolean isPortal(BlockPos pos) {
        return level.getBlockState(pos).is(CESGRegistration.GATEWAY_PORTAL.get());
    }

    private AABB interiorBox() {
        AABB box = null;
        for (BlockPos cell : activePortalCells) {
            AABB cellBox = new AABB(cell);
            box = box == null ? cellBox : box.minmax(cellBox);
        }
        return box;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        net.minecraft.nbt.ListTag bindingList = new net.minecraft.nbt.ListTag();
        bindings.forEach((channel, bound) -> {
            CompoundTag entry = bound.save();
            entry.putInt("Channel", channel);
            bindingList.add(entry);
        });
        tag.put("Bindings", bindingList);
        net.minecraft.nbt.ListTag filterList = new net.minecraft.nbt.ListTag();
        channelFilters.forEach((channel, filter) -> {
            if (!filter.hasContent())
                return; // don't persist/sync fresh defaults
            CompoundTag entry = filter.save(registries);
            entry.putInt("Channel", channel);
            filterList.add(entry);
        });
        tag.put("ChannelFilters", filterList);
        tag.putBoolean("RouteMode", routeMode);
        tag.putInt("ActiveChannel", activeChannel);
        tag.putInt("PartnerStatus", partnerStatus);
        tag.putString("GatewayName", gatewayName);
        tag.putBoolean("ChunkLoading", chunkLoading);
        if (forcedTarget != null) {
            tag.putString("ForcedDim", forcedTarget.dimension().location().toString());
            tag.putLong("ForcedPos", forcedTarget.pos().asLong());
        }
        tag.putInt("Essence", essenceMb);
        tag.putInt("Eye", eyeMb);
        tag.putLongArray("Portal", packCells(activePortalCells));
        tag.putLongArray("FrameLit", packCells(activeFrameCells));
    }

    private static long[] packCells(List<BlockPos> cells) {
        long[] packed = new long[cells.size()];
        for (int i = 0; i < packed.length; i++)
            packed[i] = cells.get(i).asLong();
        return packed;
    }

    private static List<BlockPos> unpackCells(long[] packed) {
        List<BlockPos> cells = new ArrayList<>(packed.length);
        for (long value : packed)
            cells.add(BlockPos.of(value));
        return cells;
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        bindings.clear();
        for (net.minecraft.nbt.Tag raw : tag.getList("Bindings", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) raw;
            GatewayPartner bound = GatewayPartner.load(entry);
            if (bound.isBound())
                bindings.put(Math.floorMod(entry.getInt("Channel"), CHANNEL_COUNT), bound);
        }
        // Pre-6A saves stored a single partner; migrate it onto channel 0.
        if (tag.contains("Partner")) {
            GatewayPartner legacy = GatewayPartner.load(tag.getCompound("Partner"));
            if (legacy.isBound())
                bindings.putIfAbsent(0, legacy);
        }
        channelFilters.clear();
        for (net.minecraft.nbt.Tag raw : tag.getList("ChannelFilters", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) raw;
            ChannelFilter filter = new ChannelFilter(this::onChannelFilterChanged);
            filter.load(registries, entry);
            channelFilters.put(Math.floorMod(entry.getInt("Channel"), CHANNEL_COUNT), filter);
        }
        routeMode = tag.getBoolean("RouteMode");
        activeChannel = Math.floorMod(tag.getInt("ActiveChannel"), CHANNEL_COUNT);
        partnerStatus = Math.floorMod(tag.getInt("PartnerStatus"), 3);
        gatewayName = tag.getString("GatewayName");
        chunkLoading = tag.getBoolean("ChunkLoading");
        forcedTarget = tag.contains("ForcedDim")
                ? net.minecraft.core.GlobalPos.of(
                        net.minecraft.resources.ResourceKey.create(
                                net.minecraft.core.registries.Registries.DIMENSION,
                                net.minecraft.resources.ResourceLocation.parse(tag.getString("ForcedDim"))),
                        BlockPos.of(tag.getLong("ForcedPos")))
                : null;
        essenceMb = tag.getInt("Essence");
        eyeMb = tag.getInt("Eye");
        activePortalCells = unpackCells(tag.getLongArray("Portal"));
        activeFrameCells = unpackCells(tag.getLongArray("FrameLit"));
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        CESGLang.forGoggles(tooltip, "cesg.goggles.gateway.essence", ChatFormatting.AQUA, essenceMb, TANK_CAPACITY);
        CESGLang.forGoggles(tooltip, "cesg.goggles.gateway.eye", ChatFormatting.AQUA, eyeMb, TANK_CAPACITY);
        if (getSpeed() == 0)
            CESGLang.forGoggles(tooltip, "cesg.goggles.gateway.unpowered", ChatFormatting.GRAY);
        // The portal also needs a valid ring, which none of the other lines report: a fuelled, powered,
        // bound gateway with a broken frame otherwise reads as completely healthy and just stays shut.
        // Name the specific rule that broke — "no frame" alone leaves the player hunting blind.
        if (level != null) {
            String frameIssue = GatewayPortalShape.describeFailure(level, worldPosition);
            if (frameIssue != null)
                CESGLang.forGoggles(tooltip, frameIssue, ChatFormatting.RED);
        }
        CESGLang.forGoggles(tooltip, "cesg.goggles.gateway.channel", ChatFormatting.WHITE, activeChannel + 1);
        if (chunkLoading)
            CESGLang.forGoggles(tooltip, "cesg.goggles.gateway.chunkloading", ChatFormatting.GOLD);
        if (getPartner().isBound() && getPartner().hasName())
            CESGLang.forGoggles(tooltip, "cesg.goggles.gateway.destination", ChatFormatting.AQUA,
                    getPartner().name());
        if (getPartner().isBound()) {
            CESGLang.forGoggles(tooltip, partnerIsCrossDimension() ? "cesg.goggles.gateway.cross_dimension"
                    : "cesg.goggles.gateway.same_dimension", ChatFormatting.GREEN);
            // UNKNOWN (partner chunks unloaded — the usual cross-dimension case) shows nothing:
            // it isn't a fault, and surfacing it raises more questions than it answers.
            if (partnerStatus == PARTNER_LIVE)
                CESGLang.forGoggles(tooltip, "cesg.goggles.gateway.partner_live", ChatFormatting.GREEN);
            else if (partnerStatus == PARTNER_OFFLINE)
                CESGLang.forGoggles(tooltip, "cesg.goggles.gateway.partner_offline", ChatFormatting.RED);
        } else {
            CESGLang.forGoggles(tooltip, "cesg.goggles.gateway.unbound", ChatFormatting.GRAY);
        }
        CESGLang.forGoggles(tooltip, "cesg.goggles.gateway.travel_cost", ChatFormatting.WHITE, travelCost());
        return true;
    }
}
