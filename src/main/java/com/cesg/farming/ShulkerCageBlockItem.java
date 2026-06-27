package com.cesg.farming;

import java.util.List;

import com.cesg.init.CESGRegistration;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class ShulkerCageBlockItem extends BlockItem {
    public ShulkerCageBlockItem(net.minecraft.world.level.block.Block block, Properties properties) {
        super(block, properties);
    }

    /** A cage carrying a captured shulker stores it as block-entity data; empty cages have none. */
    public static boolean holdsShulker(ItemStack stack) {
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        return data != null && data.copyTag().contains("TrappedShulker");
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return holdsShulker(stack) ? 1 : super.getMaxStackSize(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (holdsShulker(stack))
            tooltip.add(Component.translatable("block.cesg.shulker_cage.occupied").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
        if (player.level().isClientSide)
            return InteractionResult.SUCCESS;
        if (!(entity instanceof Shulker shulker) || !shulker.isAlive())
            return InteractionResult.PASS;

        Level level = player.level();
        BlockPos pos = shulker.blockPosition();
        if (!level.getBlockState(pos).canBeReplaced())
            return InteractionResult.FAIL;

        BlockState cageState = CESGRegistration.SHULKER_CAGE.get().defaultBlockState();
        if (!level.setBlock(pos, cageState, net.minecraft.world.level.block.Block.UPDATE_ALL | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE))
            return InteractionResult.FAIL;

        if (!(level.getBlockEntity(pos) instanceof ShulkerCageBlockEntity cage))
            return InteractionResult.FAIL;

        if (!cage.trapShulker(shulker))
            return InteractionResult.FAIL;

        if (!player.getAbilities().instabuild)
            stack.shrink(1);

        level.playSound(null, pos, SoundEvents.SHULKER_CLOSE, SoundSource.BLOCKS, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }
}
