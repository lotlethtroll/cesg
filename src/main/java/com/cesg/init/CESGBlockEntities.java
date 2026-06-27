package com.cesg.init;

import com.cesg.CESG;
import com.cesg.farming.ShulkerCageBlockEntity;
import com.cesg.gateways.CrossDimensionalGatewayCoreBlockEntity;
import com.cesg.gateways.EndGatewayBlockEntity;
import com.cesg.storage.beltloader.ShulkerBeltLoaderBlockEntity;
import com.cesg.storage.beltunloader.ShulkerBeltUnloaderBlockEntity;
import com.cesg.storage.loader.ShulkerLoaderBlockEntity;
import com.cesg.storage.unloader.ShulkerUnloaderBlockEntity;
import com.cesg.upgrades.EnhancedShulkerBoxBlockEntity;
import com.cesg.upgrades.EnhancedShulkerBoxes;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CESGBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CESG.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShulkerLoaderBlockEntity>> SHULKER_LOADER =
            BLOCK_ENTITIES.register("shulker_loader",
                    () -> BlockEntityType.Builder.of(ShulkerLoaderBlockEntity::new, CESGRegistration.SHULKER_LOADER.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShulkerUnloaderBlockEntity>> SHULKER_UNLOADER =
            BLOCK_ENTITIES.register("shulker_unloader",
                    () -> BlockEntityType.Builder.of(ShulkerUnloaderBlockEntity::new, CESGRegistration.SHULKER_UNLOADER.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShulkerBeltLoaderBlockEntity>> SHULKER_BELT_LOADER =
            BLOCK_ENTITIES.register("shulker_belt_loader",
                    () -> BlockEntityType.Builder.of(ShulkerBeltLoaderBlockEntity::new, CESGRegistration.SHULKER_BELT_LOADER.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShulkerBeltUnloaderBlockEntity>> SHULKER_BELT_UNLOADER =
            BLOCK_ENTITIES.register("shulker_belt_unloader",
                    () -> BlockEntityType.Builder.of(ShulkerBeltUnloaderBlockEntity::new, CESGRegistration.SHULKER_BELT_UNLOADER.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShulkerCageBlockEntity>> SHULKER_CAGE =
            BLOCK_ENTITIES.register("shulker_cage",
                    () -> BlockEntityType.Builder.of(ShulkerCageBlockEntity::new,
                                    CESGRegistration.SHULKER_CAGE.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EndGatewayBlockEntity>> END_GATEWAY =
            BLOCK_ENTITIES.register("end_gateway",
                    () -> BlockEntityType.Builder.of(EndGatewayBlockEntity::new, CESGRegistration.END_GATEWAY.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrossDimensionalGatewayCoreBlockEntity>> CROSS_DIMENSIONAL_GATEWAY_CORE =
            BLOCK_ENTITIES.register("cross_dimensional_gateway_core",
                    () -> BlockEntityType.Builder.of(CrossDimensionalGatewayCoreBlockEntity::new,
                                    CESGRegistration.CROSS_DIMENSIONAL_GATEWAY_CORE.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnhancedShulkerBoxBlockEntity>> ENHANCED_SHULKER_BOX =
            BLOCK_ENTITIES.register("enhanced_shulker_box",
                    () -> BlockEntityType.Builder.of(EnhancedShulkerBoxBlockEntity::new, EnhancedShulkerBoxes.allBlocks())
                            .build(null));

    static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}
