package com.cesg.gateways;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.cesg.init.CESGBlockEntities;
import com.simibubi.create.api.connectivity.ConnectivityHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Block item for the Gateway Flux Battery. Mirrors Create's {@code FluidTankItem}: after a normal
 * placement, if you click a vertical face of an existing 2×2+ array, it auto-places the whole W×W
 * layer above/below in one action (consuming from your stack, only if you have enough for the layer).
 * Charged items carry fuel picked up with the wrench; a packed array also records its W×W×H size.
 */
public class GatewayFluxBatteryItem extends BlockItem {
    static final String FUEL_TAG = "CESGBatteryFuel";
    static final String ARRAY_TAG = "CESGBatteryArray";

    public GatewayFluxBatteryItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext ctx) {
        return place(ctx, ctx.getLevel()::isLoaded);
    }

    /** {@link #place(BlockPlaceContext)} with an injectable chunk-loaded check (GameTests). */
    InteractionResult place(BlockPlaceContext ctx, Predicate<BlockPos> loaded) {
        ItemStack stack = ctx.getItemInHand();
        int[] size = arraySize(stack);
        if (size == null)
            return GatewayFluxBatteryBlock.refuse(ctx.getLevel().isClientSide ? null : ctx.getPlayer(),
                    "cesg.battery.pickup.place_failed");
        if (size[0] > 1 || size[1] > 1) return placeArray(ctx, size[0], size[1], loaded);
        var fuel = storedFuel(stack, ctx.getLevel().registryAccess());
        boolean charged = !fuel.isEmpty();
        if (charged && !isolatedPosition(ctx.getLevel(), ctx.getClickedPos(), loaded)) {
            if (!ctx.getLevel().isClientSide && ctx.getPlayer() != null)
                ctx.getPlayer().displayClientMessage(Component.translatable("cesg.battery.pickup.place_alone"), true);
            return InteractionResult.FAIL;
        }
        InteractionResult result = super.place(ctx);
        if (!result.consumesAction()) return result;
        if (charged) {
            if (!ctx.getLevel().isClientSide
                    && ctx.getLevel().getBlockEntity(ctx.getClickedPos()) instanceof GatewayFluxBatteryBlockEntity battery)
                battery.getTankInventory().fill(fuel, IFluidHandler.FluidAction.EXECUTE);
            // Creative placement normally copies items; a charged battery instead transfers ownership.
            if (ctx.getPlayer() != null && ctx.getPlayer().getAbilities().instabuild) stack.shrink(1);
        } else tryMultiPlace(ctx);
        return result;
    }

    /**
     * Places a packed W×W×H array extending away from the player (forward, to their right, and up, or
     * down when placed against a block's underside) from the placement position. The whole space is
     * validated first; then the empty blocks are placed bottom layer first so Create forms the array, a
     * place event is fired, the formed shape is verified, and exactly the stored fuel is filled. Any
     * failure removes the placed (empty) blocks and keeps the item, which still owns all the fuel.
     */
    private InteractionResult placeArray(BlockPlaceContext ctx, int width, int height, Predicate<BlockPos> loaded) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        ItemStack stack = ctx.getItemInHand();
        FluidStack fuel = rawFuel(stack, level.registryAccess());
        int capacity = width * width * height * GatewayFluxBatteryBlockEntity.capacityPerBlock();
        if (fuel.isEmpty()) return GatewayFluxBatteryBlock.refuse(player, "cesg.battery.pickup.place_failed");
        if (fuel.getAmount() > capacity)
            return GatewayFluxBatteryBlock.refuse(player, "cesg.battery.pickup.place_capacity",
                    fuel.getAmount(), width, width, height, capacity);

        Direction forward = ctx.getHorizontalDirection();
        Direction right = forward.getClockWise();
        Direction face = ctx.getClickedFace();
        Direction vertical = face == Direction.DOWN ? Direction.DOWN : Direction.UP;
        List<BlockPos> cells = new ArrayList<>();
        for (int y = 0; y < height; y++)
            for (int d = 0; d < width; d++)
                for (int r = 0; r < width; r++)
                    cells.add(ctx.getClickedPos().relative(vertical, y).relative(forward, d).relative(right, r));
        cells.sort(Comparator.comparingInt(BlockPos::getY)); // bottom layer first
        Set<BlockPos> space = new HashSet<>(cells);
        BlockState state = getBlock().defaultBlockState();
        CollisionContext collision = CollisionContext.of(player);
        for (BlockPos cell : cells) {
            boolean clear = loaded.test(cell) && !level.isOutsideBuildHeight(cell)
                    && level.getBlockState(cell).canBeReplaced() && level.isUnobstructed(state, cell, collision)
                    && level.mayInteract(player, cell) && player.mayUseItemAt(cell, face, stack);
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = cell.relative(direction);
                if (!space.contains(neighbor) && (!loaded.test(neighbor)
                        || GatewayFluxBatteryBlock.isBattery(level.getBlockState(neighbor))))
                    clear = false;
            }
            if (!clear)
                return GatewayFluxBatteryBlock.refuse(player, "cesg.battery.pickup.place_space", width, width, height);
        }

        BlockPos origin = new BlockPos(cells.stream().mapToInt(BlockPos::getX).min().orElseThrow(),
                cells.get(0).getY(), cells.stream().mapToInt(BlockPos::getZ).min().orElseThrow());
        List<BlockPos> placed = new ArrayList<>();
        boolean filled = GatewayFluxBatteryBlock.withoutSnapshotCapture(level, () -> {
            List<BlockSnapshot> snapshots = new ArrayList<>();
            Direction gauge = player.getDirection().getOpposite();
            for (BlockPos cell : cells) {
                snapshots.add(BlockSnapshot.create(level.dimension(), level, cell));
                if (!level.setBlock(cell, state, Block.UPDATE_ALL)) return false;
                placed.add(cell);
                if (level.getBlockEntity(cell) instanceof GatewayFluxBatteryBlockEntity battery)
                    battery.setOwnGaugeFacing(gauge);
            }
            if (EventHooks.onMultiBlockPlace(player, snapshots, face)) return false;
            if (!(level.getBlockEntity(origin) instanceof GatewayFluxBatteryBlockEntity controller)
                    || !controller.isController() || controller.getWidth() != width
                    || controller.getHeight() != height || !controller.isArrayPrismIntact())
                return false;
            var tank = controller.getTankInventory();
            return tank.fill(fuel, IFluidHandler.FluidAction.SIMULATE) == fuel.getAmount()
                    && tank.fill(fuel, IFluidHandler.FluidAction.EXECUTE) == fuel.getAmount();
        });
        if (!filled) {
            // Anything partially filled leaves with these blocks; the item still holds all of the fuel.
            GatewayFluxBatteryBlock.withoutSnapshotCapture(level, () -> {
                for (int i = placed.size() - 1; i >= 0; i--) level.removeBlock(placed.get(i), false);
                return true;
            });
            return GatewayFluxBatteryBlock.refuse(player, "cesg.battery.pickup.place_failed");
        }
        stack.shrink(1);
        // Vanilla restores a creative player's held count after useOn; detach the spent stack so the
        // packed array is consumed in creative as well.
        if (player.getItemInHand(ctx.getHand()) == stack) player.setItemInHand(ctx.getHand(), ItemStack.EMPTY);
        SoundType sound = state.getSoundType(level, ctx.getClickedPos(), player);
        level.playSound(null, ctx.getClickedPos(), sound.getPlaceSound(), SoundSource.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        return InteractionResult.CONSUME;
    }

    static boolean isolatedPosition(Level level, BlockPos pos, Predicate<BlockPos> loaded) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            if (!loaded.test(neighbor) || GatewayFluxBatteryBlock.isBattery(level.getBlockState(neighbor))) return false;
        }
        return true;
    }

    static ItemStack pickupStack(GatewayFluxBatteryBlockEntity battery) {
        return packedStack(battery.getBlockState().getBlock(), battery.storedFluid().copy(), 1, 1,
                battery.getLevel().registryAccess());
    }

    /** One battery item owning {@code fuel}; a 1x1x1 keeps the 1.2 format, larger arrays also record W and H. */
    static ItemStack packedStack(Block block, FluidStack fuel, int width, int height, HolderLookup.Provider registries) {
        ItemStack stack = new ItemStack(block);
        if (fuel.isEmpty()) return stack;
        CompoundTag tag = new CompoundTag();
        tag.put(FUEL_TAG, fuel.save(registries));
        if (width > 1 || height > 1) {
            CompoundTag array = new CompoundTag();
            array.putInt("W", width);
            array.putInt("H", height);
            tag.put(ARRAY_TAG, array);
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    /** {width, height} recorded on the item: {1, 1} for single and plain items, null if the record is invalid. */
    @Nullable
    static int[] arraySize(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return new int[] {1, 1};
        CompoundTag tag = data.copyTag();
        if (!tag.contains(ARRAY_TAG)) return new int[] {1, 1};
        CompoundTag array = tag.getCompound(ARRAY_TAG);
        int width = array.getInt("W"), height = array.getInt("H");
        return width >= 1 && width <= GatewayFluxBatteryBlockEntity.MAX_WIDTH && height >= 1 && height <= width
                ? new int[] {width, height} : null;
    }

    /** The item's recorded fuel, not bounded by the current capacity config; empty unless a gateway fuel. */
    static FluidStack rawFuel(ItemStack stack, HolderLookup.Provider registries) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return FluidStack.EMPTY;
        var fuel = FluidStack.parseOptional(registries, data.copyTag().getCompound(FUEL_TAG));
        if (!GatewayFluxBatteryBlockEntity.isEssence(fuel) && !GatewayFluxBatteryBlockEntity.isEye(fuel))
            return FluidStack.EMPTY;
        return fuel;
    }

    /** {@link #rawFuel} bounded by the configured capacity of the recorded W×W×H size (one block for 1.2 items). */
    static FluidStack storedFuel(ItemStack stack, HolderLookup.Provider registries) {
        int[] size = arraySize(stack);
        FluidStack fuel = rawFuel(stack, registries);
        if (size == null || fuel.isEmpty()) return FluidStack.EMPTY;
        int blocks = size[0] * size[0] * size[1];
        return fuel.copyWithAmount(Math.min(fuel.getAmount(), blocks * GatewayFluxBatteryBlockEntity.capacityPerBlock()));
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().contains(FUEL_TAG) ? 1 : super.getMaxStackSize(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (context.registries() == null) return;
        int[] size = arraySize(stack);
        boolean array = size != null && (size[0] > 1 || size[1] > 1);
        if (array) tooltip.add(Component.translatable("cesg.battery.pickup.array_size", size[0], size[0], size[1]));
        // An array shows what it really holds, even when a lowered capacity config would refuse placement.
        var fuel = array ? rawFuel(stack, context.registries()) : storedFuel(stack, context.registries());
        if (!fuel.isEmpty())
            tooltip.add(Component.translatable("cesg.battery.pickup.fuel", fuel.getHoverName(), fuel.getAmount()));
    }

    private void tryMultiPlace(BlockPlaceContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null || player.isShiftKeyDown())
            return;
        Direction face = ctx.getClickedFace();
        if (!face.getAxis().isVertical())
            return;

        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockPos placedOnPos = pos.relative(face.getOpposite());
        if (!(level.getBlockState(placedOnPos).getBlock() instanceof GatewayFluxBatteryBlock))
            return;

        GatewayFluxBatteryBlockEntity part =
                ConnectivityHandler.partAt(CESGBlockEntities.GATEWAY_FLUX_BATTERY.get(), level, placedOnPos);
        if (part == null)
            return;
        GatewayFluxBatteryBlockEntity controller = part.getControllerBE();
        if (controller == null)
            return;

        int width = controller.getWidth();
        if (width == 1)
            return; // only a formed base auto-stacks a layer; single columns don't multi-place

        BlockPos startPos = face == Direction.DOWN
                ? controller.getBlockPos().below()
                : controller.getBlockPos().above(controller.getHeight());
        if (startPos.getY() != pos.getY())
            return; // the block just placed must sit in the target layer

        ItemStack stack = ctx.getItemInHand();
        int toPlace = 0;
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < width; z++) {
                BlockState state = level.getBlockState(startPos.offset(x, 0, z));
                if (state.getBlock() instanceof GatewayFluxBatteryBlock)
                    continue;
                if (!state.canBeReplaced()) {
                    // First block from super.place already remains; re-form via its controller.
                    scheduleControllerReform(level, pos);
                    return; // layer footprint is obstructed — don't half-place
                }
                toPlace++;
            }
        }
        if (!player.isCreative() && stack.getCount() < toPlace) {
            scheduleControllerReform(level, pos);
            return;
        }

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < width; z++) {
                BlockPos offsetPos = startPos.offset(x, 0, z);
                if (level.getBlockState(offsetPos).getBlock() instanceof GatewayFluxBatteryBlock)
                    continue;
                super.place(BlockPlaceContext.at(ctx, offsetPos, face));
            }
        }
    }

    /** After an aborted auto-fill, the first placed cell may have joined mid-form — re-form its controller. */
    private static void scheduleControllerReform(Level level, BlockPos placedPos) {
        if (level.isClientSide)
            return;
        GatewayFluxBatteryBlockEntity part =
                ConnectivityHandler.partAt(CESGBlockEntities.GATEWAY_FLUX_BATTERY.get(), level, placedPos);
        if (part == null)
            return;
        GatewayFluxBatteryBlockEntity ctrl = part.getControllerBE();
        if (ctrl != null)
            ctrl.scheduleConnectivityUpdate();
    }
}
