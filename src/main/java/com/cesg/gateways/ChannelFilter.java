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
    }

    public ItemStackHandler items() {
        return items;
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
        if (blacklist)
            return true;
        for (int slot = 0; slot < items.getSlots(); slot++)
            if (!items.getStackInSlot(slot).isEmpty())
                return true;
        return false;
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("Items", items.serializeNBT(registries));
        tag.putBoolean("Blacklist", blacklist);
        return tag;
    }

    public void load(HolderLookup.Provider registries, CompoundTag tag) {
        items.deserializeNBT(registries, tag.getCompound("Items"));
        blacklist = tag.getBoolean("Blacklist");
    }
}
