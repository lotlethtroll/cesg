package com.cesg.storage.enderbarrel;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * World-level inventory pools for Ender Barrel pairs: one 27-slot container per pair id, shared by
 * every placed barrel carrying that id. Stored on the overworld so pairs work across dimensions.
 * Both twins (their menus AND their item-handler caps) operate on the same live container, exactly
 * like two players viewing one chest.
 */
public class EnderBarrelSharedStorage extends SavedData {
    public static final int SLOTS = 27;
    private static final String STORAGE_KEY = "cesg_ender_barrels";

    private final Map<UUID, SimpleContainer> pools = new HashMap<>();

    public static EnderBarrelSharedStorage get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(EnderBarrelSharedStorage::new, EnderBarrelSharedStorage::load, null),
                STORAGE_KEY);
    }

    public SimpleContainer pool(UUID pairId) {
        return pools.computeIfAbsent(pairId, id -> newPool());
    }

    private SimpleContainer newPool() {
        SimpleContainer container = new SimpleContainer(SLOTS);
        container.addListener(changed -> setDirty());
        return container;
    }

    private static EnderBarrelSharedStorage load(CompoundTag tag, HolderLookup.Provider registries) {
        EnderBarrelSharedStorage storage = new EnderBarrelSharedStorage();
        for (Tag raw : tag.getList("Pools", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) raw;
            NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(entry, items, registries);
            SimpleContainer container = storage.newPool();
            for (int slot = 0; slot < SLOTS; slot++)
                container.setItem(slot, items.get(slot));
            storage.pools.put(entry.getUUID("Id"), container);
        }
        return storage;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        pools.forEach((id, container) -> {
            if (container.isEmpty())
                return; // empty pools rebuild identically on demand
            NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
            for (int slot = 0; slot < SLOTS; slot++)
                items.set(slot, container.getItem(slot));
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Id", id);
            ContainerHelper.saveAllItems(entry, items, registries);
            list.add(entry);
        });
        tag.put("Pools", list);
        return tag;
    }
}
