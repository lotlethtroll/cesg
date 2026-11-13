package com.cesg.gateways;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.cesg.init.CESGBlockEntities;
import com.cesg.init.CESGFluids;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.createmod.catnip.lang.Lang;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * Gateway Flux Battery (Phase 7E): a fuel reservoir placed beside a gateway ring that tops up the Core.
 * Assembles into a Create-fluid-tank-style multiblock (see {@link GatewayFluxBatteryBlockEntity}); place
 * batteries adjacent / stacked and they merge into one larger tank. Visuals use TOP/BOTTOM/SHAPE
 * window models + connected textures, matching Create's fluid tank.
 */
public class GatewayFluxBatteryBlock extends BaseEntityBlock implements IWrenchable {
    private static final MapCodec<GatewayFluxBatteryBlock> MAP_CODEC = simpleCodec(GatewayFluxBatteryBlock::new);
    private static final int BUCKET_MB = 1000;

    public static final BooleanProperty TOP = BooleanProperty.create("top");
    public static final BooleanProperty BOTTOM = BooleanProperty.create("bottom");
    public static final EnumProperty<Shape> SHAPE = EnumProperty.create("shape", Shape.class);

    public GatewayFluxBatteryBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(TOP, true)
                .setValue(BOTTOM, true)
                .setValue(SHAPE, Shape.WINDOW));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return MAP_CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TOP, BOTTOM, SHAPE);
    }

    public static boolean isBattery(BlockState state) {
        return state.getBlock() instanceof GatewayFluxBatteryBlock;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GatewayFluxBatteryBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        return createTickerHelper(type, CESGBlockEntities.GATEWAY_FLUX_BATTERY.get(),
                level.isClientSide ? GatewayFluxBatteryBlockEntity::clientTick
                        : GatewayFluxBatteryBlockEntity::serverTick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * Wrenching a horizontal face the gauge isn't on moves the gauge there; wrenching the face it is
     * already on (or a top/bottom face) toggles windows as before.
     */
    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        if (!(level.getBlockEntity(context.getClickedPos()) instanceof GatewayFluxBatteryBlockEntity be))
            return InteractionResult.SUCCESS;
        if (level.isClientSide)
            return InteractionResult.SUCCESS;

        Direction face = context.getClickedFace();
        GatewayFluxBatteryBlockEntity controller = be.getControllerBE();
        if (face.getAxis().isHorizontal() && controller != null && controller.getGaugeFacing() != face) {
            be.setGaugeFacing(face);
            if (context.getPlayer() instanceof ServerPlayer serverPlayer)
                serverPlayer.displayClientMessage(Component.translatable("cesg.battery.gauge_moved",
                        Component.translatable("cesg.direction." + face.getSerializedName())), true);
            return InteractionResult.SUCCESS;
        }
        be.toggleWindows();
        return InteractionResult.SUCCESS;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || placer == null)
            return;
        // Gauge faces the placer: the face of the block they were looking at.
        if (level.getBlockEntity(pos) instanceof GatewayFluxBatteryBlockEntity be)
            be.setOwnGaugeFacing(placer.getDirection().getOpposite());
    }

    /**
     * Fuel-preserving pickup. An isolated 1x1x1 battery keeps its 1.2 behaviour; any block of a larger
     * array picks up the whole array into one item (see {@link #pickUp}).
     */
    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null || player.isSpectator() || com.simibubi.create.foundation.utility.AdventureUtil.isAdventure(player))
            return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        return pickUp(level, player, context.getClickedPos(), level::isLoaded);
    }

    /**
     * All-or-nothing pickup of the array containing {@code clickedPos}. Refuses, changing nothing, unless
     * every member is loaded (per {@code loaded}), the prism is intact with no pending connectivity update,
     * and no {@link BlockEvent.BreakEvent} is cancelled. Then all tank fuel is summed, every tank emptied
     * and the members removed (controller last), so Create's split has nothing to distribute. A fuelled
     * array becomes one unstackable item holding all of it; an empty array becomes plain batteries.
     */
    static InteractionResult pickUp(Level level, Player player, BlockPos clickedPos, Predicate<BlockPos> loaded) {
        GatewayFluxBatteryBlockEntity controller = level.getBlockEntity(clickedPos) instanceof GatewayFluxBatteryBlockEntity clicked
                ? clicked.getControllerBE() : null;
        if (controller == null) return refuse(player, "cesg.battery.pickup.refused");
        int width = controller.getWidth(), height = controller.getHeight();
        BlockPos origin = controller.getBlockPos();
        // A 1x1x1 keeps the 1.2 rule: only an isolated battery, never one touching other batteries.
        if (width == 1 && height == 1 && !GatewayFluxBatteryItem.isolatedPosition(level, clickedPos, loaded))
            return refuse(player, "cesg.battery.pickup.array");
        List<BlockPos> cells = new ArrayList<>();
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                for (int z = 0; z < width; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (!loaded.test(pos)) return refuse(player, "cesg.battery.pickup.refused");
                    if (!pos.equals(origin)) cells.add(pos);
                }
        cells.add(origin); // controller removed last
        if (!cells.contains(clickedPos) || !controller.isArrayPrismIntact())
            return refuse(player, "cesg.battery.pickup.refused");
        List<GatewayFluxBatteryBlockEntity> members = new ArrayList<>();
        FluidStack total = FluidStack.EMPTY;
        for (BlockPos pos : cells) {
            GatewayFluxBatteryBlockEntity member = (GatewayFluxBatteryBlockEntity) level.getBlockEntity(pos);
            if (member.connectivityPending() || !level.mayInteract(player, pos)) return refuse(player, "cesg.battery.pickup.refused");
            FluidStack fuel = member.getTankInventory().getFluid();
            if (!fuel.isEmpty()) {
                if (total.isEmpty()) total = fuel.copy();
                else if (FluidStack.isSameFluidSameComponents(total, fuel)) total.grow(fuel.getAmount());
                else return refuse(player, "cesg.battery.pickup.refused");
            }
            members.add(member);
        }
        for (BlockPos pos : cells)
            if (NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, level.getBlockState(pos), player)).isCanceled())
                return refuse(player, "cesg.battery.pickup.refused");

        Block block = controller.getBlockState().getBlock();
        withoutSnapshotCapture(level, () -> {
            for (GatewayFluxBatteryBlockEntity member : members) member.getTankInventory().setFluid(FluidStack.EMPTY);
            for (BlockPos pos : cells)
                if (!level.removeBlock(pos, false))
                    com.cesg.CESG.LOGGER.warn("Gateway Flux Battery pickup could not remove {}", pos);
            return true;
        });
        ItemHandlerHelper.giveItemToPlayer(player, total.isEmpty() ? new ItemStack(block, cells.size())
                : GatewayFluxBatteryItem.packedStack(block, total, width, height, level.registryAccess()));
        return InteractionResult.SUCCESS;
    }

    /**
     * NeoForge captures block snapshots while an item is used and replays or reverts them afterwards,
     * deferring {@code onPlace}. Multi-block pickup/placement must see Create's connectivity immediately
     * and fires its own Break/Place events, so it runs with capture off.
     */
    static boolean withoutSnapshotCapture(Level level, BooleanSupplier action) {
        boolean capturing = level.captureBlockSnapshots;
        level.captureBlockSnapshots = false;
        try {
            return action.getAsBoolean();
        } finally {
            level.captureBlockSnapshots = capturing;
        }
    }

    static InteractionResult refuse(@Nullable Player player, String key, Object... args) {
        if (player != null) player.displayClientMessage(Component.translatable(key, args), true);
        return InteractionResult.FAIL;
    }

    /**
     * Bucket pour/drain against any member block — always targets the controller tank
     * (same single-fuel lock as pipes). Essence and Eye buckets only.
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide)
            return ItemInteractionResult.SUCCESS;

        if (!(level.getBlockEntity(pos) instanceof GatewayFluxBatteryBlockEntity part))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        GatewayFluxBatteryBlockEntity controller = part.getControllerBE();
        if (controller == null)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        IFluidHandler tank = controller.createFluidHandler();
        ItemStack held = player.getItemInHand(hand);
        ServerPlayer serverPlayer = player instanceof ServerPlayer sp ? sp : null;

        if (tryPourBucket(level, pos, tank, held, player, hand, CESGFluids.TELEPORT_ESSENCE.getSource(),
                CESGFluids.TELEPORT_ESSENCE.get().getBucket(), serverPlayer)
                || tryPourBucket(level, pos, tank, held, player, hand, CESGFluids.LIQUID_EYE_OF_ENDER.getSource(),
                        CESGFluids.LIQUID_EYE_OF_ENDER.get().getBucket(), serverPlayer))
            return ItemInteractionResult.SUCCESS;

        if (held.is(Items.BUCKET)) {
            FluidStack sim = tank.drain(BUCKET_MB, IFluidHandler.FluidAction.SIMULATE);
            if (sim.getAmount() < BUCKET_MB) {
                if (serverPlayer != null)
                    serverPlayer.displayClientMessage(Component.translatable("cesg.battery.need_bucket_fuel",
                            BUCKET_MB, sim.getAmount()), true);
                return ItemInteractionResult.SUCCESS;
            }
            FluidStack drained = tank.drain(BUCKET_MB, IFluidHandler.FluidAction.EXECUTE);
            ItemStack filled = new ItemStack(drained.getFluid().getBucket());
            player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, filled));
            if (serverPlayer != null)
                serverPlayer.displayClientMessage(Component.translatable("cesg.battery.fuel_drained", BUCKET_MB), true);
            level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.8f, 1.0f);
            return ItemInteractionResult.SUCCESS;
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private static boolean tryPourBucket(Level level, BlockPos pos, IFluidHandler tank, ItemStack held, Player player,
            InteractionHand hand, Fluid fluid, net.minecraft.world.item.Item bucket,
            @Nullable ServerPlayer serverPlayer) {
        if (!held.is(bucket))
            return false;
        FluidStack resource = new FluidStack(fluid, BUCKET_MB);
        int accepted = tank.fill(resource, IFluidHandler.FluidAction.SIMULATE);
        if (accepted < BUCKET_MB) {
            if (serverPlayer != null) {
                FluidStack stored = tank.getFluidInTank(0);
                if (!stored.isEmpty() && !stored.getFluid().isSame(fluid)) {
                    // Single-fuel lock: array already holds the other gateway fuel.
                    serverPlayer.displayClientMessage(
                            Component.translatable("cesg.battery.wrong_fuel", stored.getHoverName()), true);
                } else {
                    // Vanilla buckets are all-or-nothing; need a full 1000 mB of free space.
                    int room = tank.getTankCapacity(0) - stored.getAmount();
                    serverPlayer.displayClientMessage(
                            Component.translatable("cesg.battery.need_bucket_space", BUCKET_MB, Math.max(0, room)),
                            true);
                }
            }
            return true;
        }
        tank.fill(resource, IFluidHandler.FluidAction.EXECUTE);
        if (!player.getAbilities().instabuild)
            player.setItemInHand(hand, new ItemStack(Items.BUCKET));
        if (serverPlayer != null)
            serverPlayer.displayClientMessage(Component.translatable("cesg.battery.fuel_added", BUCKET_MB), true);
        level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.8f,
                fluid == CESGFluids.TELEPORT_ESSENCE.getSource() ? 1.15f : 0.9f);
        return true;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (oldState.is(state.getBlock()) || movedByPiston)
            return;
        if (level.getBlockEntity(pos) instanceof GatewayFluxBatteryBlockEntity be) {
            be.updateConnectivity();
            // Connectivity may have rewritten TOP/BOTTOM/SHAPE — force neighbour notify like Create tanks.
            BlockState newState = level.getBlockState(pos);
            if (state != newState && newState.getBlock() == this)
                level.markAndNotifyBlock(pos, level.getChunkAt(pos), oldState, newState, UPDATE_ALL_IMMEDIATE, 512);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.hasBlockEntity() && (state.getBlock() != newState.getBlock() || !newState.hasBlockEntity())) {
            if (level.getBlockEntity(pos) instanceof GatewayFluxBatteryBlockEntity be) {
                level.removeBlockEntity(pos);
                ConnectivityHandler.splitMulti(be);
            }
        }
    }

    public enum Shape implements StringRepresentable {
        PLAIN, WINDOW, WINDOW_NW, WINDOW_SW, WINDOW_NE, WINDOW_SE;

        @Override
        public String getSerializedName() {
            return Lang.asId(name());
        }
    }
}
