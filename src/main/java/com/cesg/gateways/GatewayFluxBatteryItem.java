package com.cesg.gateways;

import com.cesg.init.CESGBlockEntities;
import com.simibubi.create.api.connectivity.ConnectivityHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block item for the Gateway Flux Battery. Mirrors Create's {@code FluidTankItem}: after a normal
 * placement, if you click a vertical face of an existing 2×2+ array, it auto-places the whole W×W
 * layer above/below in one action (consuming from your stack, only if you have enough for the layer).
 */
public class GatewayFluxBatteryItem extends BlockItem {
    public GatewayFluxBatteryItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext ctx) {
        ItemStack stack = ctx.getItemInHand();
        var fuel = storedFuel(stack, ctx.getLevel().registryAccess());
        boolean charged = !fuel.isEmpty();
        if (charged && !isolatedPosition(ctx.getLevel(), ctx.getClickedPos())) {
            if (!ctx.getLevel().isClientSide && ctx.getPlayer() != null)
                ctx.getPlayer().displayClientMessage(net.minecraft.network.chat.Component.translatable("cesg.battery.pickup.place_alone"), true);
            return InteractionResult.FAIL;
        }
        InteractionResult result = super.place(ctx);
        if (!result.consumesAction()) return result;
        if (charged) {
            if (!ctx.getLevel().isClientSide
                    && ctx.getLevel().getBlockEntity(ctx.getClickedPos()) instanceof GatewayFluxBatteryBlockEntity battery)
                battery.getTankInventory().fill(fuel, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            // Creative placement normally copies items; a charged battery instead transfers ownership.
            if (ctx.getPlayer() != null && ctx.getPlayer().getAbilities().instabuild) stack.shrink(1);
        } else tryMultiPlace(ctx);
        return result;
    }

    static boolean isolatedPosition(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            if (!level.isLoaded(neighbor) || GatewayFluxBatteryBlock.isBattery(level.getBlockState(neighbor))) return false;
        }
        return true;
    }

    static ItemStack pickupStack(GatewayFluxBatteryBlockEntity battery) {
        ItemStack stack = new ItemStack(battery.getBlockState().getBlock());
        var fuel = battery.storedFluid().copy();
        if (!fuel.isEmpty()) {
            var tag = new net.minecraft.nbt.CompoundTag();
            tag.put("CESGBatteryFuel", fuel.save(battery.getLevel().registryAccess()));
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.of(tag));
        }
        return stack;
    }

    static net.neoforged.neoforge.fluids.FluidStack storedFuel(ItemStack stack, net.minecraft.core.HolderLookup.Provider registries) {
        var data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (data == null) return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
        var fuel = net.neoforged.neoforge.fluids.FluidStack.parseOptional(registries, data.copyTag().getCompound("CESGBatteryFuel"));
        if (!GatewayFluxBatteryBlockEntity.isEssence(fuel) && !GatewayFluxBatteryBlockEntity.isEye(fuel))
            return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
        return fuel.copyWithAmount(Math.min(fuel.getAmount(), GatewayFluxBatteryBlockEntity.capacityPerBlock()));
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        var data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().contains("CESGBatteryFuel") ? 1 : super.getMaxStackSize(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<net.minecraft.network.chat.Component> tooltip,
            net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (context.registries() == null) return;
        var fuel = storedFuel(stack, context.registries());
        if (!fuel.isEmpty()) tooltip.add(net.minecraft.network.chat.Component.translatable("cesg.battery.pickup.fuel",
                fuel.getHoverName(), fuel.getAmount()));
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
