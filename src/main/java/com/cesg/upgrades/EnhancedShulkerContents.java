package com.cesg.upgrades;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record EnhancedShulkerContents(
        int tier,
        int slotCount,
        int upgradeSlotCount,
        List<StoredSlot> mainSlots,
        List<StoredSlot> upgradeSlots,
        ItemStack filterStack
) {
    /**
     * Per-slot record holding an item with an unbounded count. Vanilla {@link ItemStack#CODEC} and
     * {@link ItemContainerContents#fromItems} clamp counts to each item's max stack size (and 99 for
     * codec), which would truncate stack-depth upgrades (e.g. 32 ender pearls back to 16). This codec
     * stores the raw count so raised stacks persist correctly.
     */
    public record StoredSlot(int index, ItemStack item) {}

    private static final Codec<ItemStack> BIG_STACK_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.holderByNameCodec().fieldOf("id").forGetter(ItemStack::getItemHolder),
            Codec.INT.fieldOf("count").orElse(1).forGetter(ItemStack::getCount),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY)
                    .forGetter(ItemStack::getComponentsPatch)
    ).apply(instance, EnhancedShulkerContents::buildStack));

    private static final Codec<StoredSlot> STORED_SLOT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("slot").forGetter(StoredSlot::index),
            BIG_STACK_CODEC.fieldOf("item").forGetter(StoredSlot::item)
    ).apply(instance, StoredSlot::new));

    private static final Codec<List<StoredSlot>> STORED_SLOTS_CODEC = STORED_SLOT_CODEC.listOf();

    private static final StreamCodec<RegistryFriendlyByteBuf, ItemStack> BIG_STACK_STREAM =
            ByteBufCodecs.fromCodecWithRegistries(BIG_STACK_CODEC);

    private static final StreamCodec<RegistryFriendlyByteBuf, StoredSlot> STORED_SLOT_STREAM =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, StoredSlot::index,
                    BIG_STACK_STREAM, StoredSlot::item,
                    StoredSlot::new);

    private static final StreamCodec<RegistryFriendlyByteBuf, List<StoredSlot>> STORED_SLOTS_STREAM =
            STORED_SLOT_STREAM.apply(ByteBufCodecs.list());

    public static final Codec<EnhancedShulkerContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("tier").forGetter(EnhancedShulkerContents::tier),
            Codec.INT.fieldOf("slot_count").forGetter(EnhancedShulkerContents::slotCount),
            Codec.INT.fieldOf("upgrade_slot_count").forGetter(EnhancedShulkerContents::upgradeSlotCount),
            STORED_SLOTS_CODEC.optionalFieldOf("main", List.of()).forGetter(EnhancedShulkerContents::mainSlots),
            STORED_SLOTS_CODEC.optionalFieldOf("upgrades", List.of()).forGetter(EnhancedShulkerContents::upgradeSlots),
            BIG_STACK_CODEC.optionalFieldOf("filter")
                    .forGetter(c -> c.filterStack().isEmpty() ? Optional.empty() : Optional.of(c.filterStack()))
    ).apply(instance, (tier, slotCount, upgradeCount, mainSlots, upgradeSlots, filter) ->
            new EnhancedShulkerContents(tier, slotCount, upgradeCount, mainSlots, upgradeSlots,
                    filter.orElse(ItemStack.EMPTY))));

    public static final StreamCodec<RegistryFriendlyByteBuf, EnhancedShulkerContents> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, EnhancedShulkerContents::tier,
                    ByteBufCodecs.VAR_INT, EnhancedShulkerContents::slotCount,
                    ByteBufCodecs.VAR_INT, EnhancedShulkerContents::upgradeSlotCount,
                    STORED_SLOTS_STREAM, EnhancedShulkerContents::mainSlots,
                    STORED_SLOTS_STREAM, EnhancedShulkerContents::upgradeSlots,
                    ItemStack.OPTIONAL_STREAM_CODEC, EnhancedShulkerContents::filterStack,
                    EnhancedShulkerContents::new);

    private static ItemStack buildStack(Holder<Item> item, int count, DataComponentPatch patch) {
        ItemStack stack = new ItemStack(item, count);
        if (!patch.isEmpty())
            stack.applyComponents(patch);
        return stack;
    }

    /** Copies main storage into {@code list}, preserving per-slot counts beyond vanilla stack limits. */
    public void copyMainInto(NonNullList<ItemStack> list) {
        copySlotsInto(mainSlots, list);
    }

    /** Copies sidebar upgrade modules into {@code list}. */
    public void copyUpgradesInto(NonNullList<ItemStack> list) {
        copySlotsInto(upgradeSlots, list);
    }

    private static void copySlotsInto(List<StoredSlot> slots, NonNullList<ItemStack> list) {
        for (int i = 0; i < list.size(); i++)
            list.set(i, ItemStack.EMPTY);
        for (StoredSlot slot : slots) {
            if (slot.index() >= 0 && slot.index() < list.size())
                list.set(slot.index(), slot.item());
        }
    }

    /** Snapshots a live handler inventory without clamping stack counts. */
    public static List<StoredSlot> snapshotStacks(NonNullList<ItemStack> stacks) {
        List<StoredSlot> out = new ArrayList<>();
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (!stack.isEmpty())
                out.add(new StoredSlot(i, stack.copy()));
        }
        return out;
    }

    public static EnhancedShulkerContents forTier(int tier) {
        int slots = switch (tier) {
            case 2 -> 54;
            case 3 -> 81;
            case 4 -> 108;
            default -> 27;
        };
        int upgradeSlots = Math.max(0, tier - 1);
        return new EnhancedShulkerContents(tier, slots, upgradeSlots, List.of(), List.of(), ItemStack.EMPTY);
    }

    public static EnhancedShulkerContents empty(int tier) {
        return forTier(tier);
    }

    /**
     * Raises tier while preserving stored items, installed upgrade modules, and filter configuration.
     * Slot indices remain valid because higher tiers only add extra rows and upgrade slots.
     */
    public static EnhancedShulkerContents upgradeTier(EnhancedShulkerContents existing, int targetTier) {
        int newTier = Math.max(existing.tier(), Math.min(targetTier, MAX_TIER));
        if (newTier == existing.tier())
            return existing;
        EnhancedShulkerContents template = forTier(newTier);
        return new EnhancedShulkerContents(newTier, template.slotCount(), template.upgradeSlotCount(),
                existing.mainSlots(), existing.upgradeSlots(), existing.filterStack());
    }

    public static final int MAX_TIER = 4;

    public static EnhancedShulkerContents migrateFromVanilla(int targetTier, ItemContainerContents vanillaContents) {
        EnhancedShulkerContents base = forTier(targetTier);
        NonNullList<ItemStack> list = NonNullList.withSize(base.slotCount(), ItemStack.EMPTY);
        vanillaContents.copyInto(list);
        return new EnhancedShulkerContents(base.tier(), base.slotCount(), base.upgradeSlotCount(),
                snapshotStacks(list), List.of(), ItemStack.EMPTY);
    }
}
