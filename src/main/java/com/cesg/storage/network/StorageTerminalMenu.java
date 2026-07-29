package com.cesg.storage.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.cesg.gateways.StorageBridgeBlockEntity;
import com.cesg.gateways.StorageBridgeBlockEntity.RemoteStatus;
import com.cesg.init.CESGMenus;
import com.cesg.init.CESGRegistration;
import com.cesg.network.TerminalContentPacket;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
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
    private List<TerminalContentPacket.Entry> lastRemoteSent = List.of();
    private int lastRemoteStatus = TerminalContentPacket.REMOTE_NONE;

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

    /**
     * Recipe-viewer "+" transfer: return the current grid to the network, then place one of each of the
     * recipe's ingredients into the 3×3 grid — preferring the player inventory, falling back to the
     * storage network. Shaped recipes keep their shape (top-left); shapeless fill sequentially. The
     * player then shift-clicks the result to batch-craft, which restocks from the network.
     */
    public void fillFromRecipe(CraftingRecipe recipe) {
        if (player.level().isClientSide)
            return;
        Ingredient[] grid = gridLayout(recipe);
        clearCraftingGrid(); // return whatever is on the grid to the network first
        for (int slot = 0; slot < 9; slot++) {
            Ingredient ingredient = grid[slot];
            if (ingredient == null || ingredient.isEmpty())
                continue;
            ItemStack chosen = takeIngredient(ingredient);
            if (!chosen.isEmpty())
                craftSlots.setItem(slot, chosen);
        }
        craftSlots.setChanged();
        updateCraftingResult();
        requestRefresh();
    }

    /** Maps a crafting recipe's ingredients onto the 3×3 grid (nulls = empty). */
    private static Ingredient[] gridLayout(CraftingRecipe recipe) {
        Ingredient[] grid = new Ingredient[9];
        if (recipe instanceof ShapedRecipe shaped) {
            int width = shaped.getWidth();
            int height = shaped.getHeight();
            NonNullList<Ingredient> ingredients = shaped.getIngredients();
            for (int row = 0; row < height && row < 3; row++)
                for (int col = 0; col < width && col < 3; col++) {
                    int index = row * width + col;
                    if (index < ingredients.size() && !ingredients.get(index).isEmpty())
                        grid[row * 3 + col] = ingredients.get(index);
                }
        } else {
            int slot = 0;
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (slot >= 9)
                    break;
                if (!ingredient.isEmpty())
                    grid[slot++] = ingredient;
            }
        }
        return grid;
    }

    /** One item accepted by {@code ingredient}, taken from the player inventory or, failing that, the network. */
    private ItemStack takeIngredient(Ingredient ingredient) {
        ItemStack[] options = ingredient.getItems();
        for (ItemStack option : options) {
            ItemStack fromPlayer = takeFromPlayer(option);
            if (!fromPlayer.isEmpty())
                return fromPlayer;
        }
        for (ItemStack option : options) {
            ItemStack pulled = StorageNetwork.extract(player.level(), terminalPos, option, 1);
            if (!pulled.isEmpty())
                return pulled;
        }
        return ItemStack.EMPTY;
    }

    private ItemStack takeFromPlayer(ItemStack sample) {
        if (sample.isEmpty())
            return ItemStack.EMPTY;
        NonNullList<ItemStack> items = player.getInventory().items; // 36 main slots only
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, sample)) {
                ItemStack taken = stack.copyWithCount(1);
                stack.shrink(1);
                return taken;
            }
        }
        return ItemStack.EMPTY;
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

        RemoteSection remote = gatherRemoteSection();
        if (remote.status() == lastRemoteStatus
                && TerminalContentPacket.sameEntries(entries, lastSent)
                && TerminalContentPacket.sameEntries(remote.entries(), lastRemoteSent))
            return;
        lastSent = entries;
        lastRemoteSent = remote.entries();
        lastRemoteStatus = remote.status();
        PacketDistributor.sendToPlayer(serverPlayer,
                new TerminalContentPacket(containerId, entries, remote.entries(), remote.status()));
    }

    private record RemoteSection(List<TerminalContentPacket.Entry> entries, int status) {}

    /**
     * The Bridge that drives this terminal's Partner section: the first LIVE one on the network, or
     * (failing that) the first Bridge at all so its OFFLINE/FAULT status is still surfaced. Multiple
     * Bridges on one ring share a partner, so a single primary avoids double-counting that partner.
     */
    public StorageBridgeBlockEntity primaryBridge() {
        if (!(player.level() instanceof ServerLevel level))
            return null;
        StorageBridgeBlockEntity fallback = null;
        for (BlockPos pos : StorageNetwork.memberPositions(level, terminalPos)) {
            if (!(level.getBlockEntity(pos) instanceof StorageBridgeBlockEntity bridge))
                continue;
            bridge.remoteSnapshot(); // refresh liveness (TTL-cached)
            if (bridge.remoteStatus() == RemoteStatus.LIVE)
                return bridge;
            if (fallback == null)
                fallback = bridge;
        }
        return fallback;
    }

    private RemoteSection gatherRemoteSection() {
        StorageBridgeBlockEntity bridge = primaryBridge();
        if (bridge == null)
            return new RemoteSection(List.of(), TerminalContentPacket.REMOTE_NONE);
        return new RemoteSection(bridge.remoteSnapshot(), statusCode(bridge.remoteStatus()));
    }

    private static int statusCode(RemoteStatus status) {
        return switch (status) {
            case LIVE -> TerminalContentPacket.REMOTE_LIVE;
            case FAULT -> TerminalContentPacket.REMOTE_FAULT;
            case OFFLINE -> TerminalContentPacket.REMOTE_OFFLINE;
            case UNLINKED -> TerminalContentPacket.REMOTE_UNLINKED;
        };
    }
}
