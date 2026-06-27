package com.cesg.upgrades;

import com.cesg.init.CESGDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.chat.Component;

import java.util.List;

public class EnhancedShulkerBoxItem extends BlockItem {
    private final int tier;

    public EnhancedShulkerBoxItem(Block block, Properties properties, int tier) {
        super(block, properties);
        this.tier = tier;
    }

    public int getTier() {
        return tier;
    }

    @Override
    public Component getName(ItemStack stack) {
        int displayTier = ensureContents(stack).tier();
        return super.getName(stack).copy().append(Component.translatable("cesg.enhanced_shulker.tier", displayTier));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        EnhancedShulkerUpgradeTooltip.appendContentsPreview(stack, tooltip);
        EnhancedShulkerUpgradeTooltip.appendItemTooltip(stack, tooltip, flag);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ensureContents(stack);

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return stack.getHoverName();
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player ignored) {
                    return new EnhancedShulkerMenu(containerId, inventory, stack);
                }
            }, buf -> EnhancedShulkerMenu.writeMenuData(buf, stack, null));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, Player player, ItemStack stack, BlockState state) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof EnhancedShulkerBoxBlockEntity box) {
            ensureContents(stack);
            box.loadFromItem(stack);
            return true;
        }
        return false;
    }

    @Override
    protected boolean canPlace(BlockPlaceContext context, BlockState state) {
        return super.canPlace(context, state);
    }

    public static EnhancedShulkerContents ensureContents(ItemStack stack) {
        EnhancedShulkerContents contents = stack.get(CESGDataComponents.ENHANCED_SHULKER.get());
        if (contents == null) {
            contents = EnhancedShulkerContents.forTier(EnhancedShulkerBoxes.TIER);
            stack.set(CESGDataComponents.ENHANCED_SHULKER.get(), contents);
        }
        return contents;
    }
}
