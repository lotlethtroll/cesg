package com.cesg.machine;

import com.cesg.init.CESGBlockEntities;
import com.cesg.init.CESGFluids;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Ender Infuser: a kinetic machine that converts Teleport Essence (+ Blaze Powder) into Liquid Eye of
 * Ender, or reclaims Teleport Essence from Liquid Eye of Ender. Rotation drives it via the back shaft.
 */
public class EnderInfuserBlock extends DirectionalKineticBlock implements IBE<EnderInfuserBlockEntity> {

    public EnderInfuserBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<EnderInfuserBlockEntity> getBlockEntityClass() {
        return EnderInfuserBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends EnderInfuserBlockEntity> getBlockEntityType() {
        return CESGBlockEntities.ENDER_INFUSER.get();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING).getOpposite();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    private static final int BUCKET = 1000;

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide || !(level instanceof ServerLevel))
            return ItemInteractionResult.SUCCESS;
        EnderInfuserBlockEntity be = getBlockEntity(level, pos);
        if (be == null)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        ItemStack held = player.getItemInHand(hand);

        // Empty bucket drains a full bucket — output (the product) first, otherwise the input.
        if (held.is(Items.BUCKET)) {
            boolean fromOutput = be.getOutput().getAmount() >= BUCKET;
            boolean fromInput = !fromOutput && be.getInput().getAmount() >= BUCKET;
            if (fromOutput || fromInput) {
                FluidStack drained = fromOutput ? be.drainOutput(BUCKET, false) : be.drainInput(BUCKET, false);
                ItemStack filled = new ItemStack(drained.getFluid().getBucket());
                player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, filled));
            }
            return ItemInteractionResult.SUCCESS;
        }

        // A fuel bucket pours a full bucket into the input tank.
        if (tryFillFromBucket(be, held, player, hand, CESGFluids.TELEPORT_ESSENCE.getSource(),
                CESGFluids.TELEPORT_ESSENCE.get().getBucket())
                || tryFillFromBucket(be, held, player, hand, CESGFluids.LIQUID_EYE_OF_ENDER.getSource(),
                        CESGFluids.LIQUID_EYE_OF_ENDER.get().getBucket()))
            return ItemInteractionResult.SUCCESS;

        // Any held item loads a catalyst slot (blaze powder, chorus fruit, sugar, cocoa beans, …).
        if (!held.isEmpty()) {
            ItemStack remainder = net.neoforged.neoforge.items.ItemHandlerHelper.insertItem(
                    be.getCatalyst(), held.copy(), false);
            int inserted = held.getCount() - remainder.getCount();
            if (inserted > 0) {
                if (!player.getAbilities().instabuild)
                    held.shrink(inserted);
                return ItemInteractionResult.SUCCESS;
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private static boolean tryFillFromBucket(EnderInfuserBlockEntity be, ItemStack held, Player player,
            InteractionHand hand, Fluid fluid, net.minecraft.world.item.Item bucket) {
        if (!held.is(bucket))
            return false;
        FluidStack resource = new FluidStack(fluid, BUCKET);
        if (be.fillInput(resource, true) != BUCKET)
            return true; // matched the bucket but the tank can't take a full bucket; consume the click
        be.fillInput(resource, false);
        if (!player.getAbilities().instabuild)
            player.setItemInHand(hand, new ItemStack(Items.BUCKET));
        return true;
    }
}
