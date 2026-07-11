package com.cesg.storage.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.cesg.init.CESGMenus;
import com.cesg.init.CESGRegistration;
import com.cesg.network.TerminalContentPacket;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Terminal container (Phase 6D): virtual network browser, 3×3 crafting grid, and player inventory.
 * Shift-click player or crafting slots deposits into the network; shift-click the result batch-crafts
 * into the player inventory.
 */
public class StorageTerminalMenu extends AbstractContainerMenu {
    private static final int REFRESH_INTERVAL = 10;

    public static final int MAIN_PANEL_WIDTH = 176;
    public static final int CRAFT_GRID_X = MAIN_PANEL_WIDTH + 8;
    public static final int CRAFT_GRID_Y = 30;
    public static final int RESULT_X = CRAFT_GRID_X + 58;
    public static final int RESULT_Y = CRAFT_GRID_Y + 18;
    /** Height of the right-side crafting extension (grid + result + padding). */
    public static final int CRAFT_NOTCH_HEIGHT = CRAFT_GRID_Y + 3 * 18 + 8;
    public static final int IMAGE_WIDTH = RESULT_X + 26;
    /** Small clear button above the left edge of the 3×3 crafting grid. */
    public static final int CLEAR_CRAFT_X = CRAFT_GRID_X;
    public static final int CLEAR_CRAFT_Y = 18;
    public static final int CLEAR_CRAFT_SIZE = 10;

    public static final int PLAYER_INV_X = 8;
    public static final int PLAYER_INV_Y = 152;
    public static final int HOTBAR_Y = 210;

    public static final int RESULT_SLOT = 0;
    public static final int CRAFT_SLOT_START = 1;
    public static final int CRAFT_SLOT_END = 10;
    private static final int PLAYER_SLOT_START = 10;
    private static final int PLAYER_SLOT_END = 46;

    private final CraftingContainer craftSlots = new TransientCraftingContainer(this, 3, 3);
    private final ResultContainer resultSlots = new ResultContainer();
    private final BlockPos terminalPos;
    private final Player player;
    private int refreshCountdown;
    private List<TerminalContentPacket.Entry> lastSent = List.of();

    public StorageTerminalMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos());
    }

    public StorageTerminalMenu(int containerId, Inventory playerInventory, BlockPos terminalPos) {
        super(CESGMenus.STORAGE_TERMINAL.get(), containerId);
        this.terminalPos = terminalPos;
        this.player = playerInventory.player;

        addSlot(new ResultSlot(playerInventory.player, craftSlots, resultSlots, 0, RESULT_X, RESULT_Y));
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 3; col++)
                addSlot(new Slot(craftSlots, col + row * 3,
                        CRAFT_GRID_X + col * 18, CRAFT_GRID_Y + row * 18));

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(playerInventory, col, PLAYER_INV_X + col * 18, HOTBAR_Y));
    }

    public BlockPos getTerminalPos() {
        return terminalPos;
    }

    public void requestRefresh() {
        refreshCountdown = 0;
    }

    @Override
    public void slotsChanged(Container inventory) {
        super.slotsChanged(inventory);
        if (inventory == craftSlots)
            updateCraftingResult();
    }

    private void updateCraftingResult() {
        Level level = player.level();
        if (level.isClientSide)
            return;
        CraftingInput input = craftSlots.asCraftInput();
        ItemStack result = ItemStack.EMPTY;
        Optional<RecipeHolder<CraftingRecipe>> recipe = level.getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, level);
        if (recipe.isPresent()) {
            ItemStack assembled = recipe.get().value().assemble(input, level.registryAccess());
            if (resultSlots.canPlaceItem(0, assembled)) {
                result = assembled;
                resultSlots.setRecipeUsed(recipe.get());
            } else {
                resultSlots.setRecipeUsed(null);
            }
        } else {
            resultSlots.setRecipeUsed(null);
        }
        resultSlots.setItem(0, result);
    }

    /** Server-side recipe preview refresh (also used mid-batch by {@link TerminalBatchCrafting}). */
    void updateCraftingResultServer() {
        updateCraftingResult();
    }

    CraftingContainer craftSlots() {
        return craftSlots;
    }

    ResultContainer resultSlots() {
        return resultSlots;
    }

    Player player() {
        return player;
    }

    /** @return true when the full result stack was merged into the player inventory */
    boolean moveCraftResultToPlayer(ItemStack stack) {
        return moveItemStackTo(stack, PLAYER_SLOT_START, PLAYER_SLOT_END, true);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockState(terminalPos).is(CESGRegistration.STORAGE_TERMINAL.get())
                && player.canInteractWithBlock(terminalPos, 8);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem())
            return ItemStack.EMPTY;

        ItemStack original = slot.getItem();

        if (index == RESULT_SLOT) {
            if (player.level().isClientSide())
                return ItemStack.EMPTY;
            return TerminalBatchCrafting.shiftCraftAll(this, player);
        }

        if (index >= CRAFT_SLOT_START && index < CRAFT_SLOT_END
                || index >= PLAYER_SLOT_START && index < PLAYER_SLOT_END) {
            if (player.level().isClientSide())
                return ItemStack.EMPTY;
            return depositSlotToNetwork(slot, original);
        }

        return ItemStack.EMPTY;
    }

    private ItemStack depositSlotToNetwork(Slot slot, ItemStack original) {
        ItemStack remainder = StorageNetwork.insert(player.level(), terminalPos, original.copy());
        if (remainder.getCount() == original.getCount())
            return ItemStack.EMPTY;
        slot.set(remainder);
        slot.setChanged();
        requestRefresh();
        return original;
    }

    /** Returns crafting-grid contents to the network and clears the matrix. */
    public void clearCraftingGrid() {
        if (player.level().isClientSide)
            return;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = craftSlots.getItem(i);
            if (stack.isEmpty())
                continue;
            ItemStack remainder = StorageNetwork.insert(player.level(), terminalPos, stack.copy());
            craftSlots.setItem(i, remainder);
        }
        craftSlots.setChanged();
        updateCraftingResult();
        requestRefresh();
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (!(player instanceof ServerPlayer serverPlayer))
            return;
        if (refreshCountdown-- > 0)
            return;
        refreshCountdown = REFRESH_INTERVAL;

        Object2IntMap<ItemStack> totals = StorageNetwork.aggregate(
                StorageNetwork.scan(serverPlayer.level(), terminalPos));
        List<TerminalContentPacket.Entry> entries = new ArrayList<>(totals.size());
        for (Object2IntMap.Entry<ItemStack> entry : totals.object2IntEntrySet())
            entries.add(new TerminalContentPacket.Entry(entry.getKey(), entry.getIntValue()));
        if (TerminalContentPacket.sameEntries(entries, lastSent))
            return;
        lastSent = entries;
        PacketDistributor.sendToPlayer(serverPlayer, new TerminalContentPacket(containerId, entries));
    }
}
