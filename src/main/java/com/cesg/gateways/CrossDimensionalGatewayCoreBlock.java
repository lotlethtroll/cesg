package com.cesg.gateways;

import com.cesg.gateways.teleport.GatewayPartner;
import com.cesg.gateways.teleport.TeleportResolver;
import com.cesg.init.CESGBlockEntities;
import com.cesg.init.CESGRegistration;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
            if (player.isShiftKeyDown())
                GatewayBindingItem.imprint(serverLevel, pos, held, serverPlayer);
            else if (GatewayBindingItem.isImprinted(held))
                GatewayBindingItem.applyBinding(serverLevel, pos, held, serverPlayer);
            else
                serverPlayer.displayClientMessage(Component.translatable("cesg.gateway.crystal_empty"), true);
            return ItemInteractionResult.SUCCESS;
        }

        if (held.is(CESGRegistration.TELEPORT_ESSENCE_BUCKET.get())) {
            if (gateway.addFuel(CrossDimensionalGatewayCoreBlockEntity.ESSENCE_FUEL_MB)) {
                if (!player.getAbilities().instabuild)
                    player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                serverPlayer.displayClientMessage(Component.translatable("cesg.gateway.fuel_added",
                        CrossDimensionalGatewayCoreBlockEntity.ESSENCE_FUEL_MB), true);
            } else {
                serverPlayer.displayClientMessage(Component.translatable("cesg.gateway.fuel_full"), true);
            }
            return ItemInteractionResult.SUCCESS;
        }

        if (held.is(CESGRegistration.LIQUID_EYE_OF_ENDER_BUCKET.get())) {
            if (gateway.addFuel(CrossDimensionalGatewayCoreBlockEntity.LIQUID_EYE_FUEL_MB)) {
                if (!player.getAbilities().instabuild)
                    player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                serverPlayer.displayClientMessage(Component.translatable("cesg.gateway.fuel_added",
                        CrossDimensionalGatewayCoreBlockEntity.LIQUID_EYE_FUEL_MB), true);
            } else {
                serverPlayer.displayClientMessage(Component.translatable("cesg.gateway.fuel_full"), true);
            }
            return ItemInteractionResult.SUCCESS;
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer))
            return InteractionResult.SUCCESS;

        var gateway = getBlockEntity(level, pos);
        if (gateway == null)
            return InteractionResult.PASS;

        String blockReason = gateway.getTravelBlockReason();
        if (blockReason != null) {
            serverPlayer.displayClientMessage(Component.translatable("cesg.gateway." + blockReason), true);
            return InteractionResult.FAIL;
        }

        if (!gateway.consumeFuel())
            return InteractionResult.FAIL;

        GatewayPartner partner = gateway.getPartner();
        TeleportResolver.teleportBound(serverPlayer, gateway.createSideState(),
                partner.resolve(serverLevel.getServer()));
        return InteractionResult.SUCCESS;
    }
}
