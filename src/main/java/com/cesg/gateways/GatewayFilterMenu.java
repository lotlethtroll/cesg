package com.cesg.gateways;

import com.cesg.init.CESGMenus;
import com.cesg.init.CESGRegistration;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import org.jetbrains.annotations.Nullable;

/**
 * Per-channel routing filter editor (Phase 7B). Opened from the destination picker for a single
 * gateway channel: one row of ghost-filter slots plus a whitelist/blacklist toggle, backed by the
 * Core's {@link ChannelFilter} for that channel.
 */
public class GatewayFilterMenu extends AbstractContainerMenu {
    public static final int PANEL_W = 176;
    public static final int SLOT = 18;
    public static final int FILTER_COLS = ChannelFilter.SLOTS;
    public static final int FILTER_X = 8;
    public static final int FILTER_Y = 36;
    public static final int PLAYER_INV_Y = 74;
    public static final int HOTBAR_Y = 132;
    public static final int IMAGE_H = 156;

    public static final int BTN_BLACKLIST = 0;
    public static final int BTN_FLUIDS = 1;
    public static final int GHOST_END = 2 * FILTER_COLS;
    private static final int DATA_BLACKLIST = 0;

    private final BlockPos pos;
    private final int channel;
    private final Player player;
    @Nullable
    private final CrossDimensionalGatewayCoreBlockEntity core;
    private final ContainerData toggle = new SimpleContainerData(2);

    public GatewayFilterMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos(), buf.readVarInt());
    }

    public GatewayFilterMenu(int containerId, Inventory playerInventory, BlockPos pos, int channel) {
        super(CESGMenus.GATEWAY_FILTER.get(), containerId);
        this.pos = pos;
        this.channel = channel;
        this.player = playerInventory.player;
        this.core = player.level().getBlockEntity(pos) instanceof CrossDimensionalGatewayCoreBlockEntity be ? be : null;

        ItemStackHandler filter = core != null ? core.getOrCreateChannelFilter(channel).items()
                : new ItemStackHandler(FILTER_COLS);
        for (int col = 0; col < FILTER_COLS; col++)
            addSlot(new GhostSlot(filter, col, FILTER_X + 1 + col * SLOT, FILTER_Y + 1, false));
        ItemStackHandler fluids = core != null ? core.getOrCreateChannelFilter(channel).fluidItems()
                : new ItemStackHandler(FILTER_COLS);
        for (int col = 0; col < FILTER_COLS; col++)
            addSlot(new GhostSlot(fluids, col, FILTER_X + 1 + col * SLOT, FILTER_Y + 1, true));

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        FILTER_X + col * SLOT, PLAYER_INV_Y + row * SLOT));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(playerInventory, col, FILTER_X + col * SLOT, HOTBAR_Y));

        addDataSlots(toggle);
        syncToggle();
    }

    public int channel() {
        return channel;
    }

    public boolean isBlacklist() {
        return toggle.get(DATA_BLACKLIST) != 0;
    }

    public boolean editingFluids() { return toggle.get(1) != 0; }

    private void syncToggle() {
        if (core != null) {
            ChannelFilter filter = core.getOrCreateChannelFilter(channel);
            toggle.set(DATA_BLACKLIST, (editingFluids() ? filter.isFluidBlacklist() : filter.isBlacklist()) ? 1 : 0);
        }
    }

    @Override
    public void broadcastChanges() {
        syncToggle();
        super.broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (core == null) return false;
        if (id == BTN_FLUIDS) toggle.set(1, editingFluids() ? 0 : 1);
        else if (id == BTN_BLACKLIST) {
            if (editingFluids()) core.toggleChannelFluidBlacklist(channel);
            else core.toggleChannelBlacklist(channel);
        } else return false;
        syncToggle();
        return true;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < GHOST_END && slots.get(slotId) instanceof GhostSlot ghost) {
            if (ghost.isActive()) ghost.setFilter(getCarried());
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockState(pos).is(CESGRegistration.CROSS_DIMENSIONAL_GATEWAY_CORE.get())
                && player.canInteractWithBlock(pos, 20);
    }

    private class GhostSlot extends SlotItemHandler {
        private final boolean fluid;
        GhostSlot(IItemHandler handler, int index, int x, int y, boolean fluid) {
            super(handler, index, x, y);
            this.fluid = fluid;
        }

        @Override
        public boolean isActive() { return fluid == editingFluids(); }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        void setFilter(ItemStack carried) {
            ItemStack filter = carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1);
            if (fluid && !filter.isEmpty()
                    && net.neoforged.neoforge.fluids.FluidUtil.getFluidContained(filter).isEmpty()) return;
            ((IItemHandlerModifiable) getItemHandler()).setStackInSlot(getSlotIndex(), filter);
        }
    }
}
