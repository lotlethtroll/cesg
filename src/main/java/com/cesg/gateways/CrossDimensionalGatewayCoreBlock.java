package com.cesg.gateways;

import com.cesg.gateways.teleport.GatewayPartner;
import com.cesg.init.CESGBlockEntities;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class CrossDimensionalGatewayCoreBlock extends DirectionalKineticBlock implements IBE<CrossDimensionalGatewayCoreBlockEntity> {
    public CrossDimensionalGatewayCoreBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<CrossDimensionalGatewayCoreBlockEntity> getBlockEntityClass() {
        return CrossDimensionalGatewayCoreBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CrossDimensionalGatewayCoreBlockEntity> getBlockEntityType() {
        return CESGBlockEntities.CROSS_DIMENSIONAL_GATEWAY_CORE.get();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING).getOpposite();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer))
            return ItemInteractionResult.SUCCESS;

        var gateway = getBlockEntity(level, pos);
        if (gateway == null)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof GatewayBindingItem) {
            GatewayBindingItem.bind(serverLevel, pos, held, gateway);
            return ItemInteractionResult.SUCCESS;
        }

        if (held.is(com.cesg.init.CESGRegistration.LIQUID_EYE_OF_ENDER_BUCKET.get())) {
            gateway.addFuel(1000);
            if (!player.getAbilities().instabuild)
                player.setItemInHand(hand, new ItemStack(net.minecraft.world.item.Items.BUCKET));
            return ItemInteractionResult.SUCCESS;
        }

        if (!gateway.canTravel())
            return ItemInteractionResult.FAIL;

        if (!gateway.consumeFuel())
            return ItemInteractionResult.FAIL;

        GatewayPartner partner = gateway.getPartner();
        if (partner == null || !partner.isBound())
            return ItemInteractionResult.FAIL;

        com.cesg.gateways.teleport.TeleportResolver.teleportBound(serverPlayer, gateway.createSideState(),
                partner.resolve(serverLevel));
        return ItemInteractionResult.SUCCESS;
    }
}
