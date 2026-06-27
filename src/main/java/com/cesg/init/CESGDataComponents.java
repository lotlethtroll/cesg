package com.cesg.init;

import com.cesg.CESG;
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

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
    }
}
