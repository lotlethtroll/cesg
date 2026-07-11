package com.cesg.upgrades;



import com.cesg.init.CESGDataComponents;

import com.cesg.storage.ShulkerInventoryAccess;



import net.minecraft.core.NonNullList;

import net.minecraft.world.item.ItemStack;

import net.minecraft.world.level.Level;

import net.neoforged.neoforge.items.IItemHandler;

import net.neoforged.neoforge.items.IItemHandlerModifiable;

import net.neoforged.neoforge.items.ItemStackHandler;



import org.jetbrains.annotations.Nullable;



public class EnhancedShulkerItemStackHandler implements IItemHandlerModifiable {

    private final ItemStack parent;

    private final NonNullList<ItemStack> mainStacks;

    private final NonNullList<ItemStack> upgradeStacks;

    private final EnhancedShulkerContents contents;

    @Nullable

    private final Level level;

    private ItemStack filterStack;

    private Runnable onChanged = () -> {};

    private Runnable slotSyncListener = () -> {};

    private boolean reentrantSync;

    public void setChangeListener(Runnable onChanged) {
        this.onChanged = onChanged == null ? () -> {} : onChanged;
    }

    /** Notified when compaction or clamping changes slots other than the one the player interacted with. */
    public void setSlotSyncListener(Runnable slotSyncListener) {
        this.slotSyncListener = slotSyncListener == null ? () -> {} : slotSyncListener;
    }

    /** Full merge pass; use when the GUI opens or a compacting module is first installed. */
    public void compactInventoryFully() {
        maybeCompact(false);
        sync();
    }



    public EnhancedShulkerItemStackHandler(ItemStack parent, EnhancedShulkerContents contents) {

        this(parent, contents, null);

    }



    public EnhancedShulkerItemStackHandler(ItemStack parent, EnhancedShulkerContents contents, @Nullable Level level) {

        this.parent = parent;

        this.contents = contents;

        this.level = level;

        this.mainStacks = NonNullList.withSize(contents.slotCount(), ItemStack.EMPTY);

        contents.copyMainInto(mainStacks);

        this.upgradeStacks = NonNullList.withSize(contents.upgradeSlotCount(), ItemStack.EMPTY);

        contents.copyUpgradesInto(upgradeStacks);

        this.filterStack = contents.filterStack().copy();

    }



    @Override

    public void setStackInSlot(int slot, ItemStack stack) {

        validateSlot(slot);

        if (!stack.isEmpty() && !isItemValid(slot, stack))

            return;

        if (!stack.isEmpty()) {

            int limit = ShulkerUpgradeItems.effectiveSlotLimit(stack, getInstalledStackLimit());

            if (stack.getCount() > limit)

                stack.setCount(limit);

        }

        mainStacks.set(slot, stack);

        maybeCompact(false);

        sync();

    }



    @Override

    public int getSlots() {

        return mainStacks.size();

    }



    @Override

    public ItemStack getStackInSlot(int slot) {

        validateSlot(slot);

        return mainStacks.get(slot);

    }



    @Override

    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        validateSlot(slot);
        if (stack.isEmpty())
            return stack;

        // Smelting module: the box stores the furnace RESULT; any leftover stays raw at the source.
        ItemStack candidate = applySmeltingUpgrade(stack);
        boolean transformed = candidate != stack;
        if (!isItemValid(slot, candidate))
            return stack;

        ItemStack existing = mainStacks.get(slot);
        if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, candidate))
            return maybeVoidOverflow(stack, candidate);

        ItemStack reference = existing.isEmpty() ? candidate : existing;
        int max = ShulkerUpgradeItems.effectiveSlotLimit(reference, getInstalledStackLimit());
        int space = max - existing.getCount();
        if (space <= 0)
            return maybeVoidOverflow(stack, candidate);

        int toInsert = Math.min(space, candidate.getCount());
        if (!simulate) {
            ItemStack copy = candidate.copy();
            copy.setCount(existing.getCount() + toInsert);
            mainStacks.set(slot, copy);
            maybeCompact(true);
            sync();
        }
        if (transformed) {
            int remaining = stack.getCount() - toInsert;
            return remaining <= 0 ? ItemStack.EMPTY
                    : maybeVoidOverflow(stack.copyWithCount(remaining), candidate);
        }
        stack.shrink(toInsert);
        return maybeVoidOverflow(stack, candidate);
    }

    /** Player-driven direct sets (hotbar swap) run the same transform as inserts. */
    ItemStack transformForStorage(ItemStack stack) {
        return applySmeltingUpgrade(stack);
    }

    /**
     * Smelting module hook: swap the incoming stack for its furnace result (1:1 recipes only),
     * CHAINED to its fixed point — cobblestone stores as smooth stone in one step, so the stored
     * form never drifts one recipe further on every reopen. Cycle-guarded against datapack loops.
     */
    /** Items known to have no further smelting step — makes repeated passes near-free. */
    private final java.util.Set<net.minecraft.world.item.Item> smeltTerminalItems = new java.util.HashSet<>();

    private ItemStack applySmeltingUpgrade(ItemStack stack) {
        if (stack.isEmpty() || level == null || !hasSmeltingUpgrade())
            return stack;
        if (smeltTerminalItems.contains(stack.getItem()))
            return stack;
        ItemStack current = stack;
        java.util.Set<net.minecraft.world.item.Item> seen = new java.util.HashSet<>();
        for (int step = 0; step < 8; step++) {
            if (!seen.add(current.getItem()))
                break; // recipe cycle (a -> b -> a): stop at the last stable form
            var recipe = level.getRecipeManager().getRecipeFor(
                    net.minecraft.world.item.crafting.RecipeType.SMELTING,
                    new net.minecraft.world.item.crafting.SingleRecipeInput(current), level);
            if (recipe.isEmpty())
                break;
            ItemStack result = recipe.get().value().getResultItem(level.registryAccess());
            // Only 1:1 recipes keep the raw<->smelted leftover mapping trivial; stop the chain here.
            if (result.isEmpty() || result.getCount() != 1)
                break;
            current = result.copyWithCount(current.getCount());
        }
        smeltTerminalItems.add(current.getItem());
        return current;
    }

    /**
     * Void module hook: overflow of a type this box already stores is destroyed instead of
     * returned. Never voids while any slot could still take the item, and never voids types the
     * box does not store — a full box is not a universal trash can.
     */
    private ItemStack maybeVoidOverflow(ItemStack leftoverRaw, ItemStack storedForm) {
        if (leftoverRaw.isEmpty() || !hasVoidUpgrade())
            return leftoverRaw;
        // Independent void-list on the module: configured = only matching overflow voids.
        ItemStack voidFilter = voidFilterStack();
        if (!voidFilter.isEmpty()
                && !ShulkerStorageUpgrades.testFilterStack(level, voidFilter, storedForm))
            return leftoverRaw;
        boolean storesType = false;
        for (int i = 0; i < mainStacks.size(); i++) {
            ItemStack inSlot = mainStacks.get(i);
            if (inSlot.isEmpty())
                return leftoverRaw; // a free slot remains: not overflow yet
            if (ItemStack.isSameItemSameComponents(inSlot, storedForm)) {
                storesType = true;
                int limit = ShulkerUpgradeItems.effectiveSlotLimit(inSlot, getInstalledStackLimit());
                if (inSlot.getCount() < limit)
                    return leftoverRaw; // room remains in an existing stack: not overflow yet
            }
        }
        return storesType ? ItemStack.EMPTY : leftoverRaw;
    }



    @Override

    public ItemStack extractItem(int slot, int amount, boolean simulate) {

        validateSlot(slot);

        ItemStack existing = mainStacks.get(slot);

        if (existing.isEmpty())

            return ItemStack.EMPTY;

        int toExtract = Math.min(amount, existing.getCount());

        ItemStack result = existing.copyWithCount(toExtract);

        if (!simulate) {

            existing.shrink(toExtract);

            if (existing.isEmpty())

                mainStacks.set(slot, ItemStack.EMPTY);

            sync();

        }

        return result;

    }



    @Override

    public int getSlotLimit(int slot) {

        return effectiveSlotLimitFor(mainStacks.get(slot));

    }



    public int effectiveSlotLimitFor(ItemStack stack) {

        return ShulkerUpgradeItems.effectiveSlotLimit(stack, getInstalledStackLimit());

    }



    @Override

    public boolean isItemValid(int slot, ItemStack stack) {

        if (stack.isEmpty())

            return true;

        if (ShulkerInventoryAccess.isShulkerBox(stack))

            return false;

        return ShulkerStorageUpgrades.passesStorageFilter(level, upgradeStacks, filterStack, stack);

    }



    public NonNullList<ItemStack> getUpgradeStacks() {

        return upgradeStacks;

    }



    public boolean hasFilterUpgrade() {

        return ShulkerStorageUpgrades.hasFilterUpgrade(upgradeStacks);

    }



    public boolean hasCompactingUpgrade() {

        return ShulkerStorageUpgrades.hasCompactingUpgrade(upgradeStacks);

    }

    public boolean hasSmeltingUpgrade() {
        return ShulkerStorageUpgrades.hasSmeltingUpgrade(upgradeStacks);
    }

    public boolean hasVoidUpgrade() {
        return ShulkerStorageUpgrades.hasVoidUpgrade(upgradeStacks);
    }

    /** The void-list configured on the installed Void module ({@code EMPTY} = void any stored type). */
    public ItemStack voidFilterStack() {
        for (ItemStack module : upgradeStacks)
            if (module.getItem() instanceof VoidUpgradeItem)
                return module.getOrDefault(com.cesg.init.CESGDataComponents.VOID_FILTER.get(),
                        ConfiguredFilterStack.EMPTY).stack();
        return ItemStack.EMPTY;
    }

    /** Stores the void-list ON the installed module item, so the config travels with the module. */
    public void setVoidFilter(ItemStack config) {
        for (ItemStack module : upgradeStacks) {
            if (module.getItem() instanceof VoidUpgradeItem) {
                if (config.isEmpty())
                    module.remove(com.cesg.init.CESGDataComponents.VOID_FILTER.get());
                else
                    module.set(com.cesg.init.CESGDataComponents.VOID_FILTER.get(), ConfiguredFilterStack.of(config));
                sync();
                return;
            }
        }
    }

    /** Ghost-slot backing for the void-list (mirrors {@link #getFilterStackHandler()}). */
    public IItemHandler getVoidFilterHandler() {
        return new ItemStackHandler(NonNullList.withSize(1, voidFilterStack())) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return hasVoidUpgrade() && ShulkerStorageUpgrades.isValidFilterConfiguration(stack);
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return hasVoidUpgrade() ? voidFilterStack() : ItemStack.EMPTY;
            }

            @Override
            public void setStackInSlot(int slot, ItemStack stack) {
                if (!isItemValid(slot, stack) && !stack.isEmpty())
                    return;
                setVoidFilter(stack);
            }
        };
    }

    /**
     * Converts EXISTING contents to their furnace results — run when a smelting module is installed
     * (and on menu open), so a box full of raw ore doesn't stay raw until touched. A slot is only
     * converted when the result still fits its slot limit (guards rare recipes whose output stacks
     * smaller than the input, which clamping could otherwise destroy).
     */
    public void smeltInventoryFully() {
        if (!hasSmeltingUpgrade())
            return;
        boolean changed = false;
        for (int i = 0; i < mainStacks.size(); i++) {
            ItemStack current = mainStacks.get(i);
            if (current.isEmpty())
                continue;
            ItemStack smelted = transformForStorage(current);
            if (smelted != current
                    && smelted.getCount() <= ShulkerUpgradeItems.effectiveSlotLimit(smelted, getInstalledStackLimit())) {
                mainStacks.set(i, smelted);
                changed = true;
            }
        }
        if (changed) {
            maybeCompact(false);
            sync();
        }
    }



    public ItemStack getFilterStack() {

        return filterStack;

    }



    public IItemHandler getUpgradeItemHandler() {

        return new ItemStackHandler(upgradeStacks) {

            @Override

            public boolean isItemValid(int slot, ItemStack stack) {

                return ShulkerUpgradeItems.isValidForUpgradeSlot(stack);

            }



            @Override

            public int getSlotLimit(int slot) {

                return 1;

            }



            @Override

            public void setStackInSlot(int slot, ItemStack stack) {

                if (!isItemValid(slot, stack))

                    return;

                super.setStackInSlot(slot, stack);

                if (!hasFilterUpgrade())

                    filterStack = ItemStack.EMPTY;

                if (hasCompactingUpgrade())

                    maybeCompact(false);
                if (hasSmeltingUpgrade())
                    smeltInventoryFully();

                sync();

            }



            @Override

            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {

                if (!isItemValid(slot, stack))

                    return stack;

                ItemStack result = super.insertItem(slot, stack, simulate);

                if (!simulate && !hasFilterUpgrade())

                    filterStack = ItemStack.EMPTY;

                if (!simulate && hasCompactingUpgrade())

                    maybeCompact(false);
                if (!simulate && hasSmeltingUpgrade())
                    smeltInventoryFully();

                if (!simulate)

                    sync();

                return result;

            }



            @Override

            public ItemStack extractItem(int slot, int amount, boolean simulate) {

                ItemStack result = super.extractItem(slot, amount, simulate);

                if (!simulate) {

                    if (!hasFilterUpgrade())

                        filterStack = ItemStack.EMPTY;

                    sync();

                }

                return result;

            }

        };

    }



    public IItemHandler getFilterStackHandler() {

        return new ItemStackHandler(NonNullList.withSize(1, filterStack)) {

            @Override

            public boolean isItemValid(int slot, ItemStack stack) {

                return hasFilterUpgrade() && ShulkerStorageUpgrades.isValidFilterConfiguration(stack);

            }



            @Override

            public int getSlotLimit(int slot) {

                return 1;

            }



            @Override

            public ItemStack getStackInSlot(int slot) {

                return hasFilterUpgrade() ? filterStack : ItemStack.EMPTY;

            }



            @Override

            public void setStackInSlot(int slot, ItemStack stack) {

                if (!isItemValid(slot, stack) && !stack.isEmpty())

                    return;

                filterStack = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);

                sync();

            }



            @Override

            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {

                if (!hasFilterUpgrade() || !ShulkerStorageUpgrades.isValidFilterConfiguration(stack))

                    return stack;

                if (!filterStack.isEmpty())

                    return stack;

                if (!simulate) {

                    filterStack = stack.copyWithCount(1);

                    sync();

                }

                return stack.getCount() <= 1 ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - 1);

            }



            @Override

            public ItemStack extractItem(int slot, int amount, boolean simulate) {

                if (!hasFilterUpgrade() || filterStack.isEmpty())

                    return ItemStack.EMPTY;

                ItemStack result = filterStack.copy();

                if (!simulate) {

                    filterStack = ItemStack.EMPTY;

                    sync();

                }

                return result;

            }

        };

    }



    /** Active only when installed in a sidebar upgrade slot, not when stored in main inventory. */

    public boolean hasInstalledStackDepthUpgrade() {

        return ShulkerUpgradeItems.highestInstalledStackDepthTier(upgradeStacks) > 0;

    }



    public int getInstalledStackLimit() {

        return ShulkerUpgradeItems.installedStackLimit(upgradeStacks);

    }



    private void maybeCompact(boolean rateLimited) {
        if (!ShulkerStorageUpgrades.hasCompactingUpgrade(upgradeStacks))
            return;
        ShulkerStorageUpgrades.CompactResult result = rateLimited
                ? ShulkerStorageUpgrades.compactStacks(mainStacks, getInstalledStackLimit(),
                        ShulkerStorageUpgrades.AUTOMATION_COMPACT_ITEM_BUDGET)
                : ShulkerStorageUpgrades.compactStacks(mainStacks, getInstalledStackLimit(), Integer.MAX_VALUE);
        if (result.itemsMoved() > 0)
            slotSyncListener.run();
    }

    /**
     * Rate-limited merge pass for automation (inserts and docked-station ticks).
     * @return {@code true} when mergeable partial stacks remain
     */
    public boolean compactAutomationPass() {
        if (!ShulkerStorageUpgrades.hasCompactingUpgrade(upgradeStacks))
            return false;
        if (!ShulkerStorageUpgrades.hasMergeablePartialStacks(mainStacks, getInstalledStackLimit()))
            return false;
        ShulkerStorageUpgrades.CompactResult result = ShulkerStorageUpgrades.compactStacks(mainStacks,
                getInstalledStackLimit(), ShulkerStorageUpgrades.AUTOMATION_COMPACT_ITEM_BUDGET);
        if (result.itemsMoved() > 0)
            sync();
        return !result.complete();
    }



    /**
     * When stack-depth modules are removed, spill excess into other slots rather than deleting items.
     * Any remainder that cannot fit stays over-limit until the player extracts it.
     */
    private boolean clampMainStacksToLimits() {

        int installed = getInstalledStackLimit();

        boolean changed = false;

        for (int i = 0; i < mainStacks.size(); i++) {

            ItemStack stack = mainStacks.get(i);

            if (stack.isEmpty())

                continue;

            int limit = ShulkerUpgradeItems.effectiveSlotLimit(stack, installed);

            while (stack.getCount() > limit) {

                int excess = stack.getCount() - limit;

                ItemStack spill = stack.copyWithCount(excess);

                stack.setCount(limit);

                boolean placed = false;

                for (int j = 0; j < mainStacks.size(); j++) {

                    if (j == i)

                        continue;

                    spill = insertItem(j, spill, false);

                    if (spill.isEmpty()) {

                        placed = true;

                        break;

                    }

                }

                if (!placed) {

                    stack.grow(spill.getCount());

                    break;

                }

                changed = true;

            }

        }

        return changed;

    }



    /**
     * Persists the current inventory to the backing item component. Call this after mutations that
     * bypass the handler's own insert/extract methods — notably vanilla {@code moveItemStackTo} stack
     * merges, which mutate the slot stack in place and only notify via {@code Container.setChanged()}.
     */
    public void persist() {
        sync();
    }

    private void sync() {

        if (reentrantSync)

            return;

        reentrantSync = true;

        try {

            if (clampMainStacksToLimits())

                maybeCompact(false);

            ItemStack savedFilter = hasFilterUpgrade() ? filterStack : ItemStack.EMPTY;

            if (!hasFilterUpgrade())

                filterStack = ItemStack.EMPTY;



            EnhancedShulkerContents updated = new EnhancedShulkerContents(

                    contents.tier(), contents.slotCount(), contents.upgradeSlotCount(),

                    EnhancedShulkerContents.snapshotStacks(mainStacks),

                    EnhancedShulkerContents.snapshotStacks(upgradeStacks),

                    savedFilter);

            parent.set(CESGDataComponents.ENHANCED_SHULKER.get(), updated);

            onChanged.run();

        } finally {

            reentrantSync = false;

        }

    }



    private void validateSlot(int slot) {

        if (slot < 0 || slot >= mainStacks.size())

            throw new RuntimeException("Slot " + slot + " not in valid range [0," + mainStacks.size() + ")");

    }

}

