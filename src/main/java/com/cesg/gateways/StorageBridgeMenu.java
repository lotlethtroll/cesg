package com.cesg.gateways;

import com.cesg.init.CESGMenus;
import com.cesg.init.CESGRegistration;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
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
 * Config GUI for the Storage Bridge's passive auto-transfer (Phase 7A/D3). Two ghost-filter rows —
 * push (local → partner) and pull (partner → local) — plus four toggle buttons (enable + whitelist/
 * blacklist per direction), driven through {@link StorageBridgeBlockEntity}. The manual terminal
 * partner section is unaffected by any of this; these controls only gate the unattended flush loop.
 */
public class StorageBridgeMenu extends AbstractContainerMenu {
    public static final int PANEL_W = 176;
    public static final int SLOT = 18;
    public static final int FILTER_COLS = 9;
    public static final int SEND_X = 8;
    public static final int SEND_Y = 34;
    public static final int PULL_X = 8;
    public static final int PULL_Y = 76;
    public static final int PLAYER_INV_Y = 112;
    public static final int HOTBAR_Y = 170;
    public static final int IMAGE_H = 194;

    private static final int SEND_SLOT_START = 0;
    private static final int PULL_SLOT_START = FILTER_COLS;
    private static final int GHOST_END = 2 * FILTER_COLS;

    public static final int BTN_PUSH = 0;
    public static final int BTN_PULL = 1;
    public static final int BTN_SEND_BLACKLIST = 2;
    public static final int BTN_PULL_BLACKLIST = 3;
    public static final int BTN_LOCK = 4;

    private static final int DATA_PUSH = 0;
    private static final int DATA_PULL = 1;
    private static final int DATA_SEND_BLACKLIST = 2;
    private static final int DATA_PULL_BLACKLIST = 3;
    private static final int DATA_LOCK = 4;

    private final BlockPos pos;
    private final Player player;
    @Nullable
    private final StorageBridgeBlockEntity bridge;
    private final ContainerData toggles = new SimpleContainerData(5);

    public StorageBridgeMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos());
    }

    public StorageBridgeMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        super(CESGMenus.STORAGE_BRIDGE.get(), containerId);
        this.pos = pos;
        this.player = playerInventory.player;
        this.bridge = player.level().getBlockEntity(pos) instanceof StorageBridgeBlockEntity be ? be : null;

        ItemStackHandler sendFilter = bridge != null ? bridge.getSendFilter() : new ItemStackHandler(FILTER_COLS);
        ItemStackHandler pullFilter = bridge != null ? bridge.getPullFilter() : new ItemStackHandler(FILTER_COLS);
        for (int col = 0; col < FILTER_COLS; col++)
            addSlot(new GhostSlot(sendFilter, col, SEND_X + 1 + col * SLOT, SEND_Y + 1));
        for (int col = 0; col < FILTER_COLS; col++)
            addSlot(new GhostSlot(pullFilter, col, PULL_X + 1 + col * SLOT, PULL_Y + 1));

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        SEND_X + col * SLOT, PLAYER_INV_Y + row * SLOT));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(playerInventory, col, SEND_X + col * SLOT, HOTBAR_Y));

        addDataSlots(toggles);
        syncTogglesFromBridge(); // correct initial state on open, before the first broadcast
    }

    private void syncTogglesFromBridge() {
        if (bridge == null)
            return;
        toggles.set(DATA_PUSH, bridge.isPushEnabled() ? 1 : 0);
        toggles.set(DATA_PULL, bridge.isPullEnabled() ? 1 : 0);
        toggles.set(DATA_SEND_BLACKLIST, bridge.isSendBlacklist() ? 1 : 0);
        toggles.set(DATA_PULL_BLACKLIST, bridge.isPullBlacklist() ? 1 : 0);
        toggles.set(DATA_LOCK, bridge.isLocked() ? 1 : 0);
    }

    public boolean isPushEnabled() {
        return toggles.get(DATA_PUSH) != 0;
    }

    public boolean isPullEnabled() {
        return toggles.get(DATA_PULL) != 0;
    }

    public boolean isSendBlacklist() {
        return toggles.get(DATA_SEND_BLACKLIST) != 0;
    }

    public boolean isPullBlacklist() {
        return toggles.get(DATA_PULL_BLACKLIST) != 0;
    }

    public boolean isLocked() {
        return toggles.get(DATA_LOCK) != 0;
    }

    @Override
    public void broadcastChanges() {
        syncTogglesFromBridge();
        super.broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (bridge == null)
            return false;
        switch (id) {
            case BTN_PUSH -> bridge.togglePushEnabled();
            case BTN_PULL -> bridge.togglePullEnabled();
            // Route mode ignores the send filter, so its whitelist/blacklist mode is inert too.
            case BTN_SEND_BLACKLIST -> {
                if (isRouteMode())
                    return false;
                bridge.toggleSendBlacklist();
            }
            case BTN_PULL_BLACKLIST -> bridge.togglePullBlacklist();
            case BTN_LOCK -> bridge.toggleLocked();
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // Ghost slots hold a type-only COPY of whatever is on the cursor; nothing is consumed.
        if (slotId >= 0 && slotId < GHOST_END && slots.get(slotId) instanceof GhostSlot ghost) {
            // In route mode the send filter is bypassed entirely (the Core's channel filters are the
            // only gate), so refuse edits rather than letting the player configure a dead control.
            if (slotId < PULL_SLOT_START && isRouteMode())
                return;
            ghost.setFilter(getCarried());
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Nullable
    private CrossDimensionalGatewayCoreBlockEntity core() {
        return GatewayFuelHandler.findCore(player.level(), pos);
    }

    /**
     * Whether the Core this Bridge hangs off is fanning items out by channel filter. The Core syncs
     * {@code RouteMode} and its bindings to the client, so this reads live on both sides rather than
     * needing its own container slot.
     */
    public boolean isRouteMode() {
        CrossDimensionalGatewayCoreBlockEntity core = core();
        return core != null && core.isRouteMode();
    }

    /** The active channel's destination name, for labelling the rows with where items actually go. */
    public Component destinationLabel() {
        CrossDimensionalGatewayCoreBlockEntity core = core();
        if (core != null) {
            com.cesg.gateways.teleport.GatewayPartner partner = core.getPartner();
            if (partner.isBound()) {
                String name = partner.displayName(player.level()); // live, so a rename shows through
                if (!name.isBlank())
                    return Component.literal(name);
            }
        }
        return Component.translatable("cesg.bridge.partner");
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // no real inventory to shuttle items into
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockState(pos).is(CESGRegistration.STORAGE_BRIDGE.get())
                && player.canInteractWithBlock(pos, 8);
    }

    /** Ghost slot: renders its configured item but never moves a real one — set via click interception. */
    private static class GhostSlot extends SlotItemHandler {
        GhostSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

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
            ((IItemHandlerModifiable) getItemHandler()).setStackInSlot(getSlotIndex(), filter);
        }
    }
}
