package com.cesg.upgrades;

import com.cesg.init.CESGBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import org.jetbrains.annotations.Nullable;

public class EnhancedShulkerBoxBlockEntity extends BlockEntity implements LidBlockEntity {
    private static final int EVENT_SET_OPEN_COUNT = 1;

    private ItemStack shulkerStack = ItemStack.EMPTY;
    private int openCount;
    private float progress;
    private float progressOld;

    public EnhancedShulkerBoxBlockEntity(BlockPos pos, BlockState state) {
        super(CESGBlockEntities.ENHANCED_SHULKER_BOX.get(), pos, state);
    }

    public ItemStack getShulkerStack() {
        if (shulkerStack.isEmpty())
            shulkerStack = defaultStackForBlock();
        return shulkerStack;
    }

    public void loadFromItem(ItemStack stack) {
        if (stack.isEmpty()) {
            shulkerStack = defaultStackForBlock();
        } else {
            shulkerStack = stack.copyWithCount(1);
            EnhancedShulkerBoxItem.ensureContents(shulkerStack);
        }
        setChanged();
    }

    private ItemStack defaultStackForBlock() {
        if (!(getBlockState().getBlock() instanceof ShulkerBoxBlock shulkerBox))
            return ItemStack.EMPTY;
        @Nullable DyeColor color = shulkerBox.getColor();
        ItemStack stack = new ItemStack(EnhancedShulkerBoxes.byColor(color).get());
        EnhancedShulkerBoxItem.ensureContents(stack);
        return stack;
    }

    public void openScreen(ServerPlayer player) {
        ItemStack stack = getShulkerStack();
        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return stack.getHoverName();
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player ignored) {
                return new EnhancedShulkerMenu(containerId, inventory, stack, level, worldPosition);
            }
        }, buf -> EnhancedShulkerMenu.writeMenuData(buf, stack, worldPosition));
    }

    public void startOpen(Player player) {
        if (player.isSpectator())
            return;
        if (openCount++ == 0) {
            level.gameEvent(player, GameEvent.CONTAINER_OPEN, worldPosition);
            level.playSound(null, worldPosition, SoundEvents.SHULKER_BOX_OPEN, SoundSource.BLOCKS, 0.5F,
                    level.random.nextFloat() * 0.1F + 0.9F);
        }
        level.blockEvent(worldPosition, getBlockState().getBlock(), EVENT_SET_OPEN_COUNT, openCount);
    }

    public void stopOpen(Player player) {
        if (player.isSpectator())
            return;
        if (openCount == 0)
            return;
        if (--openCount == 0) {
            level.gameEvent(player, GameEvent.CONTAINER_CLOSE, worldPosition);
            level.playSound(null, worldPosition, SoundEvents.SHULKER_BOX_CLOSE, SoundSource.BLOCKS, 0.5F,
                    level.random.nextFloat() * 0.1F + 0.9F);
        }
        level.blockEvent(worldPosition, getBlockState().getBlock(), EVENT_SET_OPEN_COUNT, openCount);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EnhancedShulkerBoxBlockEntity blockEntity) {
        blockEntity.tickAnimation();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, EnhancedShulkerBoxBlockEntity blockEntity) {
        blockEntity.tickAnimation();
    }

    private void tickAnimation() {
        progressOld = progress;
        if (openCount > 0)
            progress = Math.min(1.0F, progress + 0.1F);
        else
            progress = Math.max(0.0F, progress - 0.1F);
    }

    @Override
    public float getOpenNess(float partialTicks) {
        return Mth.lerp(partialTicks, progressOld, progress);
    }

    @Override
    public boolean triggerEvent(int id, int type) {
        if (id == EVENT_SET_OPEN_COUNT) {
            openCount = type;
            return true;
        }
        return super.triggerEvent(id, type);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!shulkerStack.isEmpty())
            tag.put("ShulkerStack", shulkerStack.save(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        shulkerStack = tag.contains("ShulkerStack")
                ? ItemStack.parse(registries, tag.getCompound("ShulkerStack")).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
    }
}
