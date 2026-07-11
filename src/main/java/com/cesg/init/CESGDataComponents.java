package com.cesg.init;

import com.cesg.CESG;
import com.cesg.upgrades.ConfiguredFilterStack;
import com.cesg.upgrades.EnhancedShulkerContents;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CESGDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, CESG.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EnhancedShulkerContents>> ENHANCED_SHULKER =
            DATA_COMPONENTS.register("enhanced_shulker",
                    () -> DataComponentType.<EnhancedShulkerContents>builder()
                            .persistent(EnhancedShulkerContents.CODEC)
                            .networkSynchronized(EnhancedShulkerContents.STREAM_CODEC)
                            .build());

    public static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> ENHANCED_SHULKER_ITEM =
            net.minecraft.tags.TagKey.create(Registries.ITEM, com.cesg.CESG.id("enhanced_shulker"));

    /** Void-list configured on an installed Void Upgrade module (independent of the storage filter). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ConfiguredFilterStack>> VOID_FILTER =
            DATA_COMPONENTS.register("void_filter",
                    () -> DataComponentType.<ConfiguredFilterStack>builder()
                            .persistent(ConfiguredFilterStack.CODEC)
                            .networkSynchronized(ConfiguredFilterStack.STREAM_CODEC)
                            .build());

    /** Twin id shared by both halves of an Ender Barrel pair; keys their shared inventory pool. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<java.util.UUID>> ENDER_BARREL_PAIR =
            DATA_COMPONENTS.register("ender_barrel_pair",
                    () -> DataComponentType.<java.util.UUID>builder()
                            .persistent(net.minecraft.core.UUIDUtil.CODEC)
                            .networkSynchronized(net.minecraft.core.UUIDUtil.STREAM_CODEC)
                            .build());

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
    }
}
