package com.cesg.upgrades;

import com.cesg.init.CESGMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

import org.jetbrains.annotations.Nullable;

public final class EnhancedShulkerMenu extends AbstractContainerMenu {
    public static final int MAIN_PANEL_WIDTH = 176;
    private static final int STORAGE_LEFT = 8;
    /** Matches left border width stripped when blitting the main section beside the upgrade column. */
    public static final int MAIN_PANEL_LEFT_BORDER = 7;
    public static final int UPGRADE_SLOT_HEIGHT = 18;
    /** Gap between the upgrade slot and the main panel (half of {@link #MAIN_PANEL_LEFT_BORDER}). */
    public static final int UPGRADE_SLOT_MARGIN = MAIN_PANEL_LEFT_BORDER / 2;
    /** Left padding: outer border + slot + inner margin before the main panel. */
    public static final int UPGRADE_COLUMN_WIDTH =
            MAIN_PANEL_LEFT_BORDER + UPGRADE_SLOT_HEIGHT + UPGRADE_SLOT_MARGIN;
    /** Centered in the column with equal border and margin on each side of the slot. */
    public static final int UPGRADE_SLOT_X = MAIN_PANEL_LEFT_BORDER;
    public static final int UPGRADE_SLOT_Y = 6;
    /** Vertical gap between the last upgrade slot and the filter configuration slot. */
    public static final int FILTER_SLOT_GAP = 4;

    private final EnhancedShulkerItemStackHandler handler;
    private final EnhancedShulkerContainer container;
    private final int storageSlotCount;
    private final int storageColumns;
    private final int storageRows;
    private final int upgradeSlotCount;
    private final boolean hasFilterConfigSlot;
    @Nullable
    private final BlockPos blockPos;

    public EnhancedShulkerMenu(int containerId, Inventory playerInventory, ItemStack shulker) {
        this(containerId, playerInventory, shulker, playerInventory.player.level(), null);
    }

    public EnhancedShulkerMenu(int containerId, Inventory playerInventory, ItemStack shulker,
            @Nullable Level level, @Nullable BlockPos blockPos) {
        super(CESGMenus.ENHANCED_SHULKER.get(), containerId);
        this.blockPos = blockPos;
        EnhancedShulkerContents contents = EnhancedShulkerBoxItem.ensureContents(shulker);
        this.handler = new EnhancedShulkerItemStackHandler(shulker, contents, level);
        this.handler.setChangeListener(() -> markBlockEntityChanged(level, blockPos));
        this.container = new EnhancedShulkerContainer(handler, shulker, level, blockPos);
        this.storageSlotCount = handler.getSlots();
        this.storageColumns = EnhancedShulkerGuiLayout.columns(storageSlotCount);
        this.storageRows = EnhancedShulkerGuiLayout.rows(storageSlotCount);
        this.upgradeSlotCount = contents.upgradeSlotCount();
        this.hasFilterConfigSlot = handler.hasFilterUpgrade();
        checkContainerSize(container, handler.getSlots());

        container.startOpen(playerInventory.player);

        int storageLeft = getStorageSlotLeft();
        int storageTop = EnhancedShulkerGuiLayout.STORAGE_TOP;

        for (int i = 0; i < upgradeSlotCount; i++) {
            addSlot(new UpgradeSlot(handler, i, UPGRADE_SLOT_X, UPGRADE_SLOT_Y + i * UPGRADE_SLOT_HEIGHT));
        }

        if (hasFilterConfigSlot) {
            addSlot(new FilterConfigSlot(handler, UPGRADE_SLOT_X, getFilterConfigSlotY()));
        }

        if (handler.hasCompactingUpgrade())
            handler.compactInventoryFully();

        this.handler.setSlotSyncListener(this::refreshStorageSlots);

        for (int i = 0; i < storageSlotCount; i++) {
            int row = i / storageColumns;
            int col = i % storageColumns;
            addSlot(new EnhancedShulkerBoxSlot(container, i,
                    storageLeft + col * EnhancedShulkerGuiLayout.SLOT,
                    storageTop + row * EnhancedShulkerGuiLayout.SLOT, EnhancedShulkerGuiLayout.SLOT));
        }

        addPlayerInventory(playerInventory, getPlayerInventoryTopY(), getPlayerInventoryLeft());
    }

    public EnhancedShulkerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, readOpenData(buf, playerInventory));
    }

    private EnhancedShulkerMenu(int containerId, Inventory playerInventory, MenuOpenData data) {
        this(containerId, playerInventory, data.stack, data.level, data.blockPos);
    }

    public int getStorageRows() {
        return storageRows;
    }

    public int getStorageColumns() {
        return storageColumns;
    }

    public int getStorageSlotCount() {
        return storageSlotCount;
    }

    public int getUpgradeSlotCount() {
        return upgradeSlotCount;
    }

    public int getActiveUpgradeSlotCount() {
        return upgradeSlotCount;
    }

    public boolean hasUpgradeColumn() {
        return upgradeSlotCount > 0;
    }

    /** Left edge of the storage grid; shifted right by the upgrade column when one is present. */
    public int getStorageSlotLeft() {
        if (!hasUpgradeColumn())
            return STORAGE_LEFT;
        return UPGRADE_COLUMN_WIDTH + STORAGE_LEFT;
    }

    /** Player inventory is centered beneath the (wider) storage grid. */
    public int getPlayerInventoryLeft() {
        int storageWidth = storageColumns * EnhancedShulkerGuiLayout.SLOT;
        int playerWidth = EnhancedShulkerGuiLayout.PLAYER_COLUMNS * EnhancedShulkerGuiLayout.SLOT;
        return getStorageSlotLeft() + Math.max(0, (storageWidth - playerWidth) / 2);
    }

    public boolean hasFilterConfigSlot() {
        return hasFilterConfigSlot;
    }

    public int getFilterConfigSlotY() {
        return UPGRADE_SLOT_Y + upgradeSlotCount * UPGRADE_SLOT_HEIGHT + FILTER_SLOT_GAP;
    }

    public int getImageWidth() {
        return getStorageSlotLeft() + storageColumns * EnhancedShulkerGuiLayout.SLOT + STORAGE_LEFT;
    }

    public int getPlayerInventoryTopY() {
        return EnhancedShulkerGuiLayout.playerInventoryTopY(storageRows);
    }

    public int getHotbarY() {
        return EnhancedShulkerGuiLayout.hotbarY(storageRows);
    }

    public int getImageHeight() {
        return EnhancedShulkerGuiLayout.imageHeight(storageRows);
    }

    public static void writeMenuData(RegistryFriendlyByteBuf buf, ItemStack stack, @Nullable BlockPos blockPos) {
        buf.writeBoolean(blockPos != null);
        if (blockPos != null)
            buf.writeBlockPos(blockPos);
        ItemStack.STREAM_CODEC.encode(buf, stack);
    }

    private static MenuOpenData readOpenData(RegistryFriendlyByteBuf buf, Inventory playerInventory) {
        boolean fromBlock = buf.readBoolean();
        BlockPos pos = fromBlock ? buf.readBlockPos() : null;
        ItemStack stack = ItemStack.STREAM_CODEC.decode(buf);
        Level level = playerInventory.player.level();
        return new MenuOpenData(stack, level, pos);
    }

    private record MenuOpenData(ItemStack stack, @Nullable Level level, @Nullable BlockPos blockPos) {}

    public boolean isForBlock(BlockPos pos) {
        return blockPos != null && blockPos.equals(pos);
    }

    /** Flags the backing block entity (if any) dirty so edited contents are persisted to disk. */
    private static void markBlockEntityChanged(@Nullable Level level, @Nullable BlockPos pos) {
        if (level == null || pos == null || level.isClientSide)
            return;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null)
            blockEntity.setChanged();
    }

    private void addPlayerInventory(Inventory playerInventory, int topY, int storageLeft) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, storageLeft + col * 18, topY + row * 18));
            }
        }
        int hotbar = getHotbarY();
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, storageLeft + col * 18, hotbar));
        }
    }

    private int filterSlotIndex() {
        return hasFilterConfigSlot ? upgradeSlotCount : -1;
    }

    private int storageStartIndex() {
        return upgradeSlotCount + (hasFilterConfigSlot ? 1 : 0);
    }

    private int storageEndIndex() {
        return storageStartIndex() + handler.getSlots();
    }

    /** Re-reads every storage slot after compaction merges items across the grid. */
    private void refreshStorageSlots() {
        int start = storageStartIndex();
        int end = storageEndIndex();
        for (int i = start; i < end; i++)
            slots.get(i).setChanged();
        broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem())
            return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        int filterIndex = filterSlotIndex();
        int storageStart = storageStartIndex();
        int storageEnd = storageEndIndex();

        if (index < upgradeSlotCount) {
            if (!moveItemStackTo(stack, storageStart, storageEnd, false)
                    && !moveItemStackTo(stack, storageEnd, slots.size(), true))
                return ItemStack.EMPTY;
        } else if (index == filterIndex) {
            if (!moveItemStackTo(stack, storageEnd, slots.size(), true))
                return ItemStack.EMPTY;
        } else if (index < storageEnd) {
            if (ShulkerUpgradeItems.isUpgradeItem(stack)
                    && moveItemStackTo(stack, 0, upgradeSlotCount, false)) {
                // upgrade module into sidebar
            } else if (handler.hasFilterUpgrade()
                    && ShulkerStorageUpgrades.isValidFilterConfiguration(stack)
                    && filterIndex >= 0
                    && moveItemStackTo(stack, filterIndex, filterIndex + 1, false)) {
                // filter configuration
            } else if (!moveItemStackTo(stack, storageEnd, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (ShulkerUpgradeItems.isUpgradeItem(stack)
                    && moveItemStackTo(stack, 0, upgradeSlotCount, false)) {
                // upgrade module placed in sidebar
            } else if (handler.hasFilterUpgrade()
                    && ShulkerStorageUpgrades.isValidFilterConfiguration(stack)
                    && filterIndex >= 0
                    && moveItemStackTo(stack, filterIndex, filterIndex + 1, false)) {
                // filter configuration
            } else if (!moveItemStackTo(stack, storageStart, storageEnd, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty())
            slot.setByPlayer(ItemStack.EMPTY);
        else
            slot.setChanged();

        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }

    private static class FilterConfigSlot extends SlotItemHandler {
        private final EnhancedShulkerItemStackHandler handler;

        public FilterConfigSlot(EnhancedShulkerItemStackHandler handler, int x, int y) {
            super(handler.getFilterStackHandler(), 0, x, y);
            this.handler = handler;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return handler.hasFilterUpgrade() && ShulkerStorageUpgrades.isValidFilterConfiguration(stack);
        }
    }

    private static class UpgradeSlot extends SlotItemHandler {
        public UpgradeSlot(EnhancedShulkerItemStackHandler handler, int index, int x, int y) {
            super(handler.getUpgradeItemHandler(), index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return ShulkerUpgradeItems.isValidForUpgradeSlot(stack);
        }
    }
}
