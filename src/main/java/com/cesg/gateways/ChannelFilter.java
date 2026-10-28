package com.cesg.gateways;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * A single gateway channel's routing filter (Phase 7B). Nine ghost slots plus a whitelist/blacklist
 * flag decide whether an item is allowed to route to this channel's partner. Semantics match the
 * Storage Bridge's per-direction filters: an empty whitelist accepts nothing, an empty blacklist
 * accepts everything — so leaving one channel as an empty blacklist makes it a catch-all.
 */
public class ChannelFilter {
    public static final int SLOTS = 9;

    private final ItemStackHandler items;
    private boolean blacklist;
    private final ItemStackHandler fluidItems;
    private boolean fluidBlacklist;

    public ChannelFilter(Runnable onChange) {
        this.items = new ItemStackHandler(SLOTS) {
            @Override
            protected void onContentsChanged(int slot) {
                onChange.run();
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1; // filters are type-only
            }
        };
        this.fluidItems = new ItemStackHandler(SLOTS) {
            @Override
            protected void onContentsChanged(int slot) {
                onChange.run();
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1; // filters are type-only
            }
        };
    }

    public ItemStackHandler items() {
        return items;
    }

    public ItemStackHandler fluidItems() { return fluidItems; }
    public boolean isFluidBlacklist() { return fluidBlacklist; }
    public void setFluidBlacklist(boolean value) { fluidBlacklist = value; }

    public boolean acceptsFluid(net.neoforged.neoforge.fluids.FluidStack sample) {
        if (sample.isEmpty()) return false;
        boolean listed = false;
        for (int slot = 0; slot < fluidItems.getSlots(); slot++) {
            var selected = net.neoforged.neoforge.fluids.FluidUtil.getFluidContained(fluidItems.getStackInSlot(slot));
            if (selected.isPresent() && net.neoforged.neoforge.fluids.FluidStack.isSameFluidSameComponents(selected.get(), sample)) {
                listed = true;
                break;
            }
        }
        return fluidBlacklist ? !listed : listed;
    }

    public boolean isBlacklist() {
        return blacklist;
    }

    public void setBlacklist(boolean value) {
        blacklist = value;
    }

    /** True when this filter would let {@code sample} route to its channel. */
    public boolean accepts(ItemStack sample) {
        boolean listed = false;
        boolean anyFilter = false;
        for (int slot = 0; slot < items.getSlots(); slot++) {
            ItemStack f = items.getStackInSlot(slot);
            if (f.isEmpty())
                continue;
            anyFilter = true;
            if (ItemStack.isSameItemSameComponents(f, sample)) {
                listed = true;
                break;
            }
        }
        if (blacklist)
            return !listed; // empty blacklist -> everything passes
        return anyFilter && listed; // empty whitelist -> nothing passes
    }

    /** Whether this filter differs from a fresh default (empty whitelist) and is worth persisting. */
    public boolean hasContent() {
        if (blacklist || fluidBlacklist)
            return true;
        for (int slot = 0; slot < items.getSlots(); slot++)
            if (!items.getStackInSlot(slot).isEmpty() || !fluidItems.getStackInSlot(slot).isEmpty())
                return true;
        return false;
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("Items", items.serializeNBT(registries));
        tag.putBoolean("Blacklist", blacklist);
        tag.put("FluidItems", fluidItems.serializeNBT(registries));
        tag.putBoolean("FluidBlacklist", fluidBlacklist);
        return tag;
    }

    public void load(HolderLookup.Provider registries, CompoundTag tag) {
        items.deserializeNBT(registries, tag.getCompound("Items"));
        blacklist = tag.getBoolean("Blacklist");
        if (tag.contains("FluidItems"))
            fluidItems.deserializeNBT(registries, tag.getCompound("FluidItems"));
        fluidBlacklist = tag.getBoolean("FluidBlacklist");
    }
}
