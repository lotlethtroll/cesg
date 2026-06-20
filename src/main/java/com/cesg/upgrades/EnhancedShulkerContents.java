package com.cesg.upgrades;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record EnhancedShulkerContents(
        int tier,
        int slotCount,
        int upgradeSlotCount,
        ItemContainerContents mainInventory,
        ItemContainerContents upgradeInventory
) {
    public static final Codec<EnhancedShulkerContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("tier").forGetter(EnhancedShulkerContents::tier),
            Codec.INT.fieldOf("slot_count").forGetter(EnhancedShulkerContents::slotCount),
            Codec.INT.fieldOf("upgrade_slot_count").forGetter(EnhancedShulkerContents::upgradeSlotCount),
            ItemContainerContents.CODEC.fieldOf("main").forGetter(EnhancedShulkerContents::mainInventory),
            ItemContainerContents.CODEC.fieldOf("upgrades").forGetter(EnhancedShulkerContents::upgradeInventory)
    ).apply(instance, EnhancedShulkerContents::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, EnhancedShulkerContents> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, EnhancedShulkerContents::tier,
                    ByteBufCodecs.VAR_INT, EnhancedShulkerContents::slotCount,
                    ByteBufCodecs.VAR_INT, EnhancedShulkerContents::upgradeSlotCount,
                    ItemContainerContents.STREAM_CODEC, EnhancedShulkerContents::mainInventory,
                    ItemContainerContents.STREAM_CODEC, EnhancedShulkerContents::upgradeInventory,
                    EnhancedShulkerContents::new);

    public static EnhancedShulkerContents forTier(int tier) {
        int slots = switch (tier) {
            case 2 -> 54;
            case 3 -> 81;
            case 4 -> 108;
            default -> 27;
        };
        int upgradeSlots = Math.max(0, tier - 1);
        return new EnhancedShulkerContents(tier, slots, upgradeSlots,
                ItemContainerContents.EMPTY,
                ItemContainerContents.fromItems(NonNullList.withSize(upgradeSlots, ItemStack.EMPTY)));
    }

    public static EnhancedShulkerContents empty(int tier) {
        return forTier(tier);
    }

    public static EnhancedShulkerContents migrateFromVanilla(int targetTier, ItemContainerContents vanillaContents) {
        EnhancedShulkerContents base = forTier(targetTier);
        NonNullList<ItemStack> list = NonNullList.withSize(base.slotCount(), ItemStack.EMPTY);
        vanillaContents.copyInto(list);
        return new EnhancedShulkerContents(base.tier(), base.slotCount(), base.upgradeSlotCount(),
                ItemContainerContents.fromItems(list), base.upgradeInventory());
    }
}
