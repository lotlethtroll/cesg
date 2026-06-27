package com.cesg.init;

import com.cesg.CESG;
import com.cesg.farming.ShulkerCageBlock;
import com.cesg.farming.ShulkerCageBlockItem;
import com.cesg.gateways.CrossDimensionalGatewayCoreBlock;
import com.cesg.gateways.EndGatewayBlock;
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
            .item()
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
            .item()
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

    public static final ItemEntry<net.minecraft.world.item.Item> TELEPORT_ESSENCE_BUCKET = CESGFluids.TELEPORT_ESSENCE_BUCKET;
    public static final ItemEntry<net.minecraft.world.item.Item> LIQUID_EYE_OF_ENDER_BUCKET = CESGFluids.LIQUID_EYE_OF_ENDER_BUCKET;

    public static final BlockEntry<EndGatewayBlock> END_GATEWAY = CESG.REGISTRATE.block("end_gateway", EndGatewayBlock::new)
            .initialProperties(() -> Blocks.END_STONE)
            .properties(p -> p.mapColor(MapColor.COLOR_PURPLE).lightLevel(s -> 8))
            .blockstate(CESGPlaceholderModels::endGateway)
            .simpleItem()
            .register();

    public static final BlockEntry<CrossDimensionalGatewayCoreBlock> CROSS_DIMENSIONAL_GATEWAY_CORE =
            CESG.REGISTRATE.block("cross_dimensional_gateway_core", CrossDimensionalGatewayCoreBlock::new)
                    .initialProperties(SharedProperties::stone)
                    .properties(p -> p.mapColor(MapColor.COLOR_PURPLE).lightLevel(s -> 10).noOcclusion())
                    .blockstate(CESGPlaceholderModels::crossDimensionalGatewayCore)
                    .simpleItem()
                    .register();

    public static final ItemEntry<GatewayBindingItem> GATEWAY_BINDING_ITEM = CESG.REGISTRATE.item("gateway_binding_item",
                    GatewayBindingItem::new)
            .model(CESGPlaceholderModels::gatewayBindingItem)
            .register();

    public static final ItemEntry<EmergencyEyeChargeItem> EMERGENCY_EYE_CHARGE = CESG.REGISTRATE.item("emergency_eye_charge",
                    EmergencyEyeChargeItem::new)
            .model(CESGPlaceholderModels::emergencyEyeCharge)
            .register();

    public static void register(IEventBus modEventBus) {
        CESGCreativeTabs.TABS.register(modEventBus);
        EnhancedShulkerBoxes.register();
        CESGBlockEntities.register(modEventBus);
        CESGFluids.register();
        CESGMenus.register(modEventBus);
    }
}
