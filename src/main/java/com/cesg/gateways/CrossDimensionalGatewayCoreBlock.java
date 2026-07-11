package com.cesg.gateways;

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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class CrossDimensionalGatewayCoreBlock extends DirectionalKineticBlock implements IBE<CrossDimensionalGatewayCoreBlockEntity> {
    /** True while the Core holds (or is receiving) fuel; drives the glass eye to show fluid vs empty. */
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    /** Which fuel fills the eye — green Liquid Eye of Ender or lilac Teleport Essence. */
    public static final net.minecraft.world.level.block.state.properties.EnumProperty<GatewayFrameBlock.FrameFuel> FUEL =
            GatewayFrameBlock.FUEL;

    public CrossDimensionalGatewayCoreBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LIT, false).setValue(FUEL, GatewayFrameBlock.FrameFuel.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
        builder.add(FUEL);
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
        // Empty hand must fall through to useWithoutItem ON BOTH SIDES — returning SUCCESS here on the
        // client consumes the interaction and the channel-picker screen never opens.
        if (stack.isEmpty())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer))
            return ItemInteractionResult.SUCCESS;

        var gateway = getBlockEntity(level, pos);
        if (gateway == null)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        ItemStack held = player.getItemInHand(hand);

        if (held.getItem() instanceof GatewayBindingItem) {
            GatewayBindingItem.handleUse(serverLevel, pos, held, serverPlayer);
            return ItemInteractionResult.SUCCESS;
        }

        if (held.is(com.cesg.init.CESGFluids.TELEPORT_ESSENCE.get().getBucket())) {
            if (gateway.addEssence(CrossDimensionalGatewayCoreBlockEntity.ESSENCE_FUEL_MB)) {
                if (!player.getAbilities().instabuild)
                    player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                serverPlayer.displayClientMessage(Component.translatable("cesg.gateway.fuel_added",
                        CrossDimensionalGatewayCoreBlockEntity.ESSENCE_FUEL_MB), true);
            } else {
                serverPlayer.displayClientMessage(Component.translatable("cesg.gateway.fuel_full"), true);
            }
            return ItemInteractionResult.SUCCESS;
        }

        if (held.is(com.cesg.init.CESGFluids.LIQUID_EYE_OF_ENDER.get().getBucket())) {
            if (gateway.addEye(CrossDimensionalGatewayCoreBlockEntity.LIQUID_EYE_FUEL_MB)) {
                if (!player.getAbilities().instabuild)
                    player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                serverPlayer.displayClientMessage(Component.translatable("cesg.gateway.fuel_added",
                        CrossDimensionalGatewayCoreBlockEntity.LIQUID_EYE_FUEL_MB), true);
            } else {
                serverPlayer.displayClientMessage(Component.translatable("cesg.gateway.fuel_full"), true);
            }
            return ItemInteractionResult.SUCCESS;
        }

        // Empty bucket drains a full bucket (1000 mB) of fuel back out — essence first, then eye.
        if (held.is(Items.BUCKET)) {
            int bucket = 1000;
            ItemStack filled;
            if (gateway.getEssenceMb() >= bucket) {
                gateway.drainEssence(bucket, false);
                filled = new ItemStack(com.cesg.init.CESGFluids.TELEPORT_ESSENCE.get().getBucket());
            } else if (gateway.getEyeMb() >= bucket) {
                gateway.drainEye(bucket, false);
                filled = new ItemStack(com.cesg.init.CESGFluids.LIQUID_EYE_OF_ENDER.get().getBucket());
            } else {
                serverPlayer.displayClientMessage(Component.translatable("cesg.gateway.tank_empty"), true);
                return ItemInteractionResult.SUCCESS;
            }
            player.setItemInHand(hand, net.minecraft.world.item.ItemUtils.createFilledResult(held, player, filled));
            serverPlayer.displayClientMessage(Component.translatable("cesg.gateway.fuel_drained", bucket), true);
            return ItemInteractionResult.SUCCESS;
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /**
     * Travel is by walking through the portal plane. Empty-hand use opens the channel picker;
     * sneak + empty hand reports the gateway's status instead.
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (!player.isShiftKeyDown()) {
            var be = getBlockEntity(level, pos);
            if (be == null)
                return InteractionResult.PASS;
            if (level.isClientSide)
                net.createmod.catnip.platform.CatnipServices.PLATFORM
                        .executeOnClientOnly(() -> () -> openChannelScreen(be));
            return InteractionResult.SUCCESS;
        }

        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer))
            return InteractionResult.SUCCESS;

        var gateway = getBlockEntity(level, pos);
        if (gateway == null)
            return InteractionResult.PASS;

        String reason = gateway.getTravelBlockReason();
        if (reason == null && !gateway.hasValidFrame())
            reason = "no_frame";
        serverPlayer.displayClientMessage(
                Component.translatable(reason == null ? "cesg.gateway.ready" : "cesg.gateway." + reason), true);
        return InteractionResult.SUCCESS;
    }

    @net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
    private static void openChannelScreen(CrossDimensionalGatewayCoreBlockEntity be) {
        net.createmod.catnip.gui.ScreenOpener.open(new com.cesg.client.GatewayChannelScreen(be));
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        // A newly placed Core gives surrounding frames something to route fuel to — refresh their caches.
        if (!oldState.is(this))
            GatewayFuelHandler.invalidateRing(level, pos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (getBlockEntity(level, pos) != null)
                getBlockEntity(level, pos).clearPortal();
            GatewayFuelHandler.invalidateRing(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
