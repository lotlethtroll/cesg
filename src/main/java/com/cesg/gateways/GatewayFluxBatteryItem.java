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
        InteractionResult result = super.place(ctx);
        if (!result.consumesAction())
            return result;
        tryMultiPlace(ctx);
        return result;
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
                if (!state.canBeReplaced())
                    return; // layer footprint is obstructed — don't half-place
                toPlace++;
            }
        }
        if (!player.isCreative() && stack.getCount() < toPlace)
            return;

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < width; z++) {
                BlockPos offsetPos = startPos.offset(x, 0, z);
                if (level.getBlockState(offsetPos).getBlock() instanceof GatewayFluxBatteryBlock)
                    continue;
                super.place(BlockPlaceContext.at(ctx, offsetPos, face));
            }
        }
    }
}
