package com.cesg.init;

import com.cesg.CESG;
import com.cesg.farming.ShulkerCageBlock;
import com.cesg.farming.ShulkerCageBlockItem;
import com.cesg.gateways.CrossDimensionalGatewayCoreBlock;
import com.cesg.gateways.EndGatewayBlock;
import com.cesg.gateways.GatewayFrameBlock;
import com.cesg.gateways.GatewayPortalBlock;
import com.cesg.storage.beltloader.ShulkerBeltLoaderBlock;
import com.cesg.storage.beltunloader.ShulkerBeltUnloaderBlock;
import com.cesg.storage.loader.ShulkerLoaderBlock;
import com.cesg.storage.unloader.ShulkerUnloaderBlock;
import com.cesg.upgrades.CompactingUpgradeItem;
import com.cesg.upgrades.EnhancedShulkerBoxes;
import com.cesg.upgrades.FilterUpgradeItem;
import com.cesg.upgrades.StackDepthUpgradeItem;
import com.cesg.farming.ShulkerShellItem;
import com.cesg.gateways.GatewayBindingItem;
import com.cesg.gateways.EmergencyEyeChargeItem;
import com.cesg.datagen.CESGPlaceholderModels;
import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.decoration.encasing.EncasedCTBehaviour;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.SharedProperties;
import com.simibubi.create.foundation.item.ItemDescription;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemEntry;

import net.neoforged.bus.api.IEventBus;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.MapColor;

public class CESGRegistration {
    /** Base Stress Units drawn per RPM. Stationary stations are MEDIUM impact, belt variants HIGH. */
    private static final double STATION_STRESS_IMPACT = 4.0;
    private static final double BELT_STATION_STRESS_IMPACT = 8.0;
    private static final double ENDER_INFUSER_STRESS_IMPACT = 8.0;

    public static final BlockEntry<ShulkerLoaderBlock> SHULKER_LOADER = CESG.REGISTRATE.block("shulker_loader", ShulkerLoaderBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(p -> p.mapColor(MapColor.COLOR_PURPLE).noOcclusion())
            .blockstate(CESGPlaceholderModels::shulkerLoader)
            .onRegister(block -> BlockStressValues.IMPACTS.register(block, () -> STATION_STRESS_IMPACT))
            .onRegister(CreateRegistrate.connectedTextures(() -> new EncasedCTBehaviour(AllSpriteShifts.ANDESITE_CASING)))
            .onRegister(CreateRegistrate.casingConnectivity((block, cc) -> cc.make(block, AllSpriteShifts.ANDESITE_CASING,
                    (state, face) -> {
                        Direction facing = state.getValue(DirectionalKineticBlock.FACING);
                        return face != facing && face != facing.getOpposite();
                    })))
            .item()
            .model(CESGPlaceholderModels::shulkerLoaderItem)
            .build()
            .register();

    public static final BlockEntry<ShulkerUnloaderBlock> SHULKER_UNLOADER = CESG.REGISTRATE.block("shulker_unloader", ShulkerUnloaderBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(p -> p.mapColor(MapColor.COLOR_CYAN).noOcclusion())
            .blockstate(CESGPlaceholderModels::shulkerUnloader)
            .onRegister(block -> BlockStressValues.IMPACTS.register(block, () -> STATION_STRESS_IMPACT))
            .onRegister(CreateRegistrate.connectedTextures(() -> new EncasedCTBehaviour(AllSpriteShifts.ANDESITE_CASING)))
            .onRegister(CreateRegistrate.casingConnectivity((block, cc) -> cc.make(block, AllSpriteShifts.ANDESITE_CASING,
                    (state, face) -> {
                        Direction facing = state.getValue(DirectionalKineticBlock.FACING);
                        return face != facing && face != facing.getOpposite();
                    })))
            .item()
            .model(CESGPlaceholderModels::shulkerUnloaderItem)
            .build()
            .register();

    public static final BlockEntry<ShulkerBeltLoaderBlock> SHULKER_BELT_LOADER = CESG.REGISTRATE.block("shulker_belt_loader", ShulkerBeltLoaderBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(p -> p.mapColor(MapColor.GOLD).noOcclusion())
            .blockstate(CESGPlaceholderModels::shulkerBeltLoader)
            .onRegister(block -> BlockStressValues.IMPACTS.register(block, () -> BELT_STATION_STRESS_IMPACT))
            .onRegister(CreateRegistrate.connectedTextures(() -> new EncasedCTBehaviour(AllSpriteShifts.BRASS_CASING)))
            .onRegister(CreateRegistrate.casingConnectivity((block, cc) -> cc.make(block, AllSpriteShifts.BRASS_CASING,
                    (state, face) -> {
                        Direction facing = state.getValue(DirectionalKineticBlock.FACING);
                        return face != facing && face != facing.getOpposite();
                    })))
            .item(com.cesg.storage.station.BeltStationBlockItem::new)
            .model(CESGPlaceholderModels::shulkerBeltLoaderItem)
            .build()
            .register();

    public static final BlockEntry<ShulkerBeltUnloaderBlock> SHULKER_BELT_UNLOADER = CESG.REGISTRATE.block("shulker_belt_unloader", ShulkerBeltUnloaderBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(p -> p.mapColor(MapColor.GOLD).noOcclusion())
            .blockstate(CESGPlaceholderModels::shulkerBeltUnloader)
            .onRegister(block -> BlockStressValues.IMPACTS.register(block, () -> BELT_STATION_STRESS_IMPACT))
            .onRegister(CreateRegistrate.connectedTextures(() -> new EncasedCTBehaviour(AllSpriteShifts.BRASS_CASING)))
            .onRegister(CreateRegistrate.casingConnectivity((block, cc) -> cc.make(block, AllSpriteShifts.BRASS_CASING,
                    (state, face) -> {
                        Direction facing = state.getValue(DirectionalKineticBlock.FACING);
                        return face != facing && face != facing.getOpposite();
                    })))
            .item(com.cesg.storage.station.BeltStationBlockItem::new)
            .model(CESGPlaceholderModels::shulkerBeltUnloaderItem)
            .build()
            .register();

    public static final ItemEntry<StackDepthUpgradeItem> STACK_DEPTH_UPGRADE_T1 = CESG.REGISTRATE
            .item("stack_depth_upgrade_t1", props -> new StackDepthUpgradeItem(props, 1))
            .model((ctx, prov) -> CESGPlaceholderModels.stackDepthUpgrade(ctx, prov, 1))
            .register();

    public static final ItemEntry<StackDepthUpgradeItem> STACK_DEPTH_UPGRADE_T2 = CESG.REGISTRATE
            .item("stack_depth_upgrade_t2", props -> new StackDepthUpgradeItem(props, 2))
            .model((ctx, prov) -> CESGPlaceholderModels.stackDepthUpgrade(ctx, prov, 2))
            .register();

    public static final ItemEntry<StackDepthUpgradeItem> STACK_DEPTH_UPGRADE_T3 = CESG.REGISTRATE
            .item("stack_depth_upgrade_t3", props -> new StackDepthUpgradeItem(props, 3))
            .model((ctx, prov) -> CESGPlaceholderModels.stackDepthUpgrade(ctx, prov, 3))
            .register();

    public static final ItemEntry<FilterUpgradeItem> FILTER_UPGRADE = CESG.REGISTRATE.item("filter_upgrade",
                    FilterUpgradeItem::new)
            .model(CESGPlaceholderModels::filterUpgrade)
            .register();

    public static final ItemEntry<CompactingUpgradeItem> COMPACTING_UPGRADE = CESG.REGISTRATE.item("compacting_upgrade",
                    CompactingUpgradeItem::new)
            .model(CESGPlaceholderModels::compactingUpgrade)
            .register();

    public static final ItemEntry<com.cesg.upgrades.SmeltingUpgradeItem> SMELTING_UPGRADE = CESG.REGISTRATE
            .item("smelting_upgrade", com.cesg.upgrades.SmeltingUpgradeItem::new)
            .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), "minecraft:item/generated")
                    .texture("layer0", CESG.id("item/smelting_upgrade")))
            .register();

    public static final ItemEntry<com.cesg.upgrades.VoidUpgradeItem> VOID_UPGRADE = CESG.REGISTRATE
            .item("void_upgrade", com.cesg.upgrades.VoidUpgradeItem::new)
            .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), "minecraft:item/generated")
                    .texture("layer0", CESG.id("item/void_upgrade")))
            .register();

    public static final ItemEntry<com.cesg.upgrades.MagnetUpgradeItem> MAGNET_UPGRADE_T1 = magnetUpgrade(1);
    public static final ItemEntry<com.cesg.upgrades.MagnetUpgradeItem> MAGNET_UPGRADE_T2 = magnetUpgrade(2);
    public static final ItemEntry<com.cesg.upgrades.MagnetUpgradeItem> MAGNET_UPGRADE_T3 = magnetUpgrade(3);

    private static ItemEntry<com.cesg.upgrades.MagnetUpgradeItem> magnetUpgrade(int tier) {
        return CESG.REGISTRATE
                .item("magnet_upgrade_t" + tier, props -> new com.cesg.upgrades.MagnetUpgradeItem(props, tier))
                .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), "minecraft:item/generated")
                        .texture("layer0", CESG.id("item/magnet_upgrade_t" + tier)))
                .register();
    }

    public static final ItemEntry<ShulkerShellItem> SHULKER_SHELL = CESG.REGISTRATE.item("shulker_shell", ShulkerShellItem::new)
            .model(CESGPlaceholderModels::shulkerShell)
            .onRegisterAfter(Registries.ITEM, item -> ItemDescription.useKey(item, "item.cesg.shulker_shell"))
            .register();

    /** Crushing-wheel output; mixed with water in a heated basin to make Liquid Ender Pearl. */
    public static final ItemEntry<net.minecraft.world.item.Item> ENDER_PEARL_DUST = CESG.REGISTRATE
            .item("ender_pearl_dust", net.minecraft.world.item.Item::new)
            .model(CESGPlaceholderModels::enderPearlDust)
            .register();

    public static final BlockEntry<ShulkerCageBlock> SHULKER_CAGE =
            CESG.REGISTRATE.block("shulker_cage", ShulkerCageBlock::new)
                    .initialProperties(() -> Blocks.END_STONE)
                    .properties(p -> p.mapColor(MapColor.COLOR_PURPLE).noOcclusion()
                            .sound(net.minecraft.world.level.block.SoundType.METAL))
                    .tag(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE)
                    .blockstate(CESGPlaceholderModels::shulkerCage)
                    .item((block, props) -> new ShulkerCageBlockItem(block, props))
                    .onRegisterAfter(Registries.ITEM, item -> ItemDescription.useKey(item, "block.cesg.shulker_cage"))
                    .build()
                    .register();


    public static final BlockEntry<EndGatewayBlock> END_GATEWAY = CESG.REGISTRATE.block("end_gateway", EndGatewayBlock::new)
            .initialProperties(() -> Blocks.END_STONE)
            .properties(p -> p.mapColor(MapColor.COLOR_PURPLE).lightLevel(s -> 8).noOcclusion())
            .blockstate(CESGPlaceholderModels::endGateway)
            .item()
            .model(CESGPlaceholderModels::endGatewayItem)
            .onRegisterAfter(Registries.ITEM, item -> ItemDescription.useKey(item, "block.cesg.end_gateway"))
            .build()
            .register();

    public static final BlockEntry<CrossDimensionalGatewayCoreBlock> CROSS_DIMENSIONAL_GATEWAY_CORE =
            CESG.REGISTRATE.block("cross_dimensional_gateway_core", CrossDimensionalGatewayCoreBlock::new)
                    .initialProperties(SharedProperties::stone)
                    .properties(p -> p.mapColor(MapColor.COLOR_PURPLE).lightLevel(s -> 10).noOcclusion())
                    .blockstate(CESGPlaceholderModels::crossDimensionalGatewayCore)
                    .item()
                    .model(CESGPlaceholderModels::crossDimensionalGatewayCoreItem)
                    .onRegisterAfter(Registries.ITEM, item -> ItemDescription.useKey(item, "block.cesg.cross_dimensional_gateway_core"))
                    .build()
                    .register();

    public static final BlockEntry<GatewayFrameBlock> GATEWAY_FRAME = CESG.REGISTRATE.block("gateway_frame", GatewayFrameBlock::new)
            .initialProperties(() -> Blocks.END_STONE_BRICKS)
            .properties(p -> p.mapColor(MapColor.COLOR_PURPLE).noOcclusion()
                    .lightLevel(s -> s.getValue(GatewayFrameBlock.LIT) ? 7 : 0))
            .tag(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE)
            .blockstate(CESGPlaceholderModels::gatewayFrame)
            // Wave 2: frames merge into one brass-outlined window (lit + unlit sheets).
            .onRegister(CreateRegistrate.connectedTextures(() -> new com.cesg.client.GatewayFrameCT()))
            .item()
            .build()
            .register();

    public static final BlockEntry<com.cesg.gateways.GatewayPortBlock> GATEWAY_PORT =
            CESG.REGISTRATE.block("gateway_port", com.cesg.gateways.GatewayPortBlock::new)
                    .initialProperties(() -> Blocks.END_STONE_BRICKS)
                    .properties(p -> p.mapColor(MapColor.COLOR_PURPLE))
                    .tag(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE)
                    .blockstate(CESGPlaceholderModels::gatewayPort)
                    .item()
                    .onRegisterAfter(Registries.ITEM, item -> ItemDescription.useKey(item, "block.cesg.gateway_port"))
                    .build()
                    .register();

    public static final BlockEntry<com.cesg.gateways.StorageBridgeBlock> STORAGE_BRIDGE =
            CESG.REGISTRATE.block("storage_bridge", com.cesg.gateways.StorageBridgeBlock::new)
                    .initialProperties(() -> Blocks.END_STONE_BRICKS)
                    .properties(p -> p.mapColor(MapColor.COLOR_PURPLE))
                    .tag(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE)
                    .blockstate(CESGPlaceholderModels::storageBridge)
                    .item()
                    .onRegisterAfter(Registries.ITEM, item -> ItemDescription.useKey(item, "block.cesg.storage_bridge"))
                    .build()
                    .register();

    public static final BlockEntry<com.cesg.gateways.GatewayFluxBatteryBlock> GATEWAY_FLUX_BATTERY =
            CESG.REGISTRATE.block("gateway_flux_battery", com.cesg.gateways.GatewayFluxBatteryBlock::new)
                    .initialProperties(SharedProperties::stone)
                    .properties(p -> p.mapColor(MapColor.COLOR_PURPLE).noOcclusion())
                    .tag(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE)
                    .blockstate(CESGPlaceholderModels::gatewayFluxBattery)
                    .item(com.cesg.gateways.GatewayFluxBatteryItem::new)
                    .onRegisterAfter(Registries.ITEM, item -> ItemDescription.useKey(item, "block.cesg.gateway_flux_battery"))
                    .build()
                    .register();

    public static final BlockEntry<GatewayPortalBlock> GATEWAY_PORTAL = CESG.REGISTRATE.block("gateway_portal", GatewayPortalBlock::new)
            .initialProperties(() -> Blocks.NETHER_PORTAL)
            .properties(p -> p.mapColor(MapColor.COLOR_PURPLE).noOcclusion().lightLevel(s -> 11).noLootTable())
            .blockstate(CESGPlaceholderModels::gatewayPortal)
            .register();

    public static final ItemEntry<GatewayBindingItem> GATEWAY_BINDING_ITEM = CESG.REGISTRATE.item("gateway_binding_item",
                    GatewayBindingItem::new)
            .model(CESGPlaceholderModels::gatewayBindingItem)
            .onRegisterAfter(Registries.ITEM, item -> ItemDescription.useKey(item, "item.cesg.gateway_binding_item"))
            .register();

    public static final ItemEntry<EmergencyEyeChargeItem> EMERGENCY_EYE_CHARGE = CESG.REGISTRATE.item("emergency_eye_charge",
                    EmergencyEyeChargeItem::new)
            .model(CESGPlaceholderModels::emergencyEyeCharge)
            .onRegisterAfter(Registries.ITEM, item -> ItemDescription.useKey(item, "item.cesg.emergency_eye_charge"))
            .register();

    public static final BlockEntry<com.cesg.machine.EnderInfuserBlock> ENDER_INFUSER =
            CESG.REGISTRATE.block("ender_infuser", com.cesg.machine.EnderInfuserBlock::new)
                    .initialProperties(SharedProperties::stone)
                    .properties(p -> p.mapColor(MapColor.COLOR_PURPLE).noOcclusion().lightLevel(s -> 7))
                    .blockstate(CESGPlaceholderModels::enderInfuser)
                    .onRegister(block -> BlockStressValues.IMPACTS.register(block, () -> ENDER_INFUSER_STRESS_IMPACT))
                    .item()
                    .model(CESGPlaceholderModels::enderInfuserItem)
                    .onRegisterAfter(Registries.ITEM, item -> ItemDescription.useKey(item, "block.cesg.ender_infuser"))
                    .build()
                    .register();

    // Ender Barrel: crafted in twinned pairs sharing one 27-slot pool (works across dimensions).
    public static final BlockEntry<com.cesg.storage.enderbarrel.EnderBarrelBlock> ENDER_BARREL =
            CESG.REGISTRATE.block("ender_barrel", com.cesg.storage.enderbarrel.EnderBarrelBlock::new)
                    .initialProperties(() -> Blocks.BARREL)
                    .properties(p -> p.mapColor(MapColor.COLOR_PURPLE).noLootTable())
                    .tag(net.minecraft.tags.BlockTags.MINEABLE_WITH_AXE)
                    .blockstate(com.cesg.datagen.CESGPlaceholderModels::enderBarrel)
                    .item(com.cesg.storage.enderbarrel.EnderBarrelBlockItem::new)
                    .onRegisterAfter(Registries.ITEM,
                            item -> ItemDescription.useKey(item, "block.cesg.ender_barrel"))
                    .build()
                    .register();

    // Phase 6D storage network: controller anchors the block-adjacency cluster; terminal is the UI.
    public static final BlockEntry<com.cesg.storage.network.StorageNetworkControllerBlock> STORAGE_NETWORK_CONTROLLER =
            CESG.REGISTRATE.block("storage_network_controller", com.cesg.storage.network.StorageNetworkControllerBlock::new)
                    .initialProperties(SharedProperties::stone)
                    .properties(p -> p.mapColor(MapColor.COLOR_PURPLE).noOcclusion().lightLevel(s -> 7))
                    .tag(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE)
                    // Hand-authored frame-and-core model (src/main/resources), brass casing via Create.
                    .blockstate((ctx, prov) -> prov.simpleBlock(ctx.getEntry(),
                            prov.models().getExistingFile(net.minecraft.resources.ResourceLocation
                                    .fromNamespaceAndPath(CESG.MOD_ID, "block/storage_network_controller"))))
                    // Real Create casing connectivity: bordered alone, merging into banks side-by-side.
                    // EncasedCTBehaviour only connects blocks that BOTH have a CasingConnectivity
                    // entry, so the casingConnectivity registration below is load-bearing.
                    .onRegister(CreateRegistrate.connectedTextures(() -> new EncasedCTBehaviour(AllSpriteShifts.BRASS_CASING)))
                    .onRegister(CreateRegistrate.casingConnectivity((block, cc) -> cc.makeCasing(block, AllSpriteShifts.BRASS_CASING)))
                    .item()
                    .onRegisterAfter(Registries.ITEM,
                            item -> ItemDescription.useKey(item, "block.cesg.storage_network_controller"))
                    .build()
                    .register();

    public static final BlockEntry<com.cesg.storage.network.StorageTerminalBlock> STORAGE_TERMINAL =
            CESG.REGISTRATE.block("storage_terminal", com.cesg.storage.network.StorageTerminalBlock::new)
                    .initialProperties(SharedProperties::stone)
                    .properties(p -> p.mapColor(MapColor.COLOR_PURPLE).noOcclusion().lightLevel(s -> 5))
                    .tag(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE)
                    // Hand-authored console model, rotated to the FACING chosen at placement.
                    .blockstate((ctx, prov) -> prov.horizontalBlock(ctx.getEntry(),
                            prov.models().getExistingFile(net.minecraft.resources.ResourceLocation
                                    .fromNamespaceAndPath(CESG.MOD_ID, "block/storage_terminal"))))
                    .item()
                    .onRegisterAfter(Registries.ITEM,
                            item -> ItemDescription.useKey(item, "block.cesg.storage_terminal"))
                    .build()
                    .register();

    public static void register(IEventBus modEventBus) {
        CESGCreativeTabs.TABS.register(modEventBus);
        EnhancedShulkerBoxes.register();
        com.cesg.decoration.CESGDecoratives.register();
        CESGBlockEntities.register(modEventBus);
        CESGFluids.register();
        CESGMenus.register(modEventBus);
    }
}
