package com.cesg.upgrades;

import com.cesg.init.CESGBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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
    /** Tier received via update tag on the client, where the full shulker stack is not synced. */
    private int clientTier = -1;
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
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    /** Tier for rendering; on the client this comes from the update tag rather than the stack. */
    public int displayTier() {
        return clientTier > 0 ? clientTier : EnhancedShulkerUpgrades.tierOf(getShulkerStack());
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

    /**
     * True while any player has this box's screen open. External writers (the storage network) must
     * stay out then: the open menu holds a snapshot handler over the same item stack, and two
     * component-backed handlers writing snapshots clobber each other (dupe/void).
     */
    public boolean isViewed() {
        return openCount > 0;
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
        blockEntity.magnetTick(level);
    }

    // --- Magnet module: pull in dropped items the box can accept ---

    private static final int MAGNET_SCAN_INTERVAL = 4;
    private Object magnetContentsRef;
    private int magnetTier;
    private EnhancedShulkerBlockItemHandler magnetHandler;

    /** Installed magnet tier, cached against the contents component identity (recomputed on change). */
    private int installedMagnetTier() {
        ItemStack stack = getShulkerStack();
        Object contents = stack.get(com.cesg.init.CESGDataComponents.ENHANCED_SHULKER.get());
        if (contents != magnetContentsRef) {
            magnetContentsRef = contents;
            magnetTier = ShulkerUpgradeItems.highestInstalledMagnetTier(
                    EnhancedShulkerUpgradeTooltip.getInstalledUpgrades(stack));
        }
        return magnetTier;
    }

    private void magnetTick(Level level) {
        if (level.isClientSide || level.getGameTime() % MAGNET_SCAN_INTERVAL != 0)
            return;
        int tier = installedMagnetTier();
        if (tier <= 0 || isViewed())
            return;

        if (magnetHandler == null)
            magnetHandler = new EnhancedShulkerBlockItemHandler(this);
        double radius = MagnetUpgradeItem.radiusForTier(tier);
        double pull = MagnetUpgradeItem.pullStrengthForTier(tier) * MAGNET_SCAN_INTERVAL;
        net.minecraft.world.phys.Vec3 center = net.minecraft.world.phys.Vec3.atCenterOf(worldPosition);
        net.minecraft.world.phys.AABB range = new net.minecraft.world.phys.AABB(worldPosition).inflate(radius);

        for (net.minecraft.world.entity.item.ItemEntity drop : level.getEntitiesOfClass(
                net.minecraft.world.entity.item.ItemEntity.class, range,
                e -> e.isAlive() && !e.hasPickUpDelay())) {
            ItemStack stack = drop.getItem();
            if (com.cesg.storage.ShulkerInventoryAccess.isShulkerBox(stack))
                continue;
            // Only attract what the box would actually take, so items never orbit a full box.
            ItemStack simulated = net.neoforged.neoforge.items.ItemHandlerHelper
                    .insertItemStacked(magnetHandler, stack.copy(), true);
            if (simulated.getCount() >= stack.getCount())
                continue;

            if (drop.position().distanceTo(center) < 1.25) {
                ItemStack remainder = net.neoforged.neoforge.items.ItemHandlerHelper
                        .insertItemStacked(magnetHandler, stack.copy(), false);
                if (remainder.getCount() != stack.getCount()) {
                    if (remainder.isEmpty())
                        drop.discard();
                    else
                        drop.setItem(remainder);
                }
            } else {
                net.minecraft.world.phys.Vec3 motion = center.subtract(drop.position()).normalize().scale(pull);
                drop.setDeltaMovement(drop.getDeltaMovement().scale(0.75).add(motion));
                drop.hasImpulse = true;
            }
        }
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
        // "Tier" is only present in the client update tag (see getUpdateTag); disk loads carry the
        // full stack instead.
        if (tag.contains("Tier"))
            clientTier = tag.getInt("Tier");
    }

    /** Client sync carries only the tier — the stored items stay server-side. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("Tier", EnhancedShulkerUpgrades.tierOf(getShulkerStack()));
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
