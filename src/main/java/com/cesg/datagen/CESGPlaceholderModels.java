package com.cesg.datagen;

import com.cesg.storage.enderbarrel.EnderBarrelBlock;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;

import org.jetbrains.annotations.Nullable;

public final class CESGPlaceholderModels {
    private CESGPlaceholderModels() {}

    private static ResourceLocation mc(String path) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", path);
    }

    private static ResourceLocation cesg(String path) {
        return ResourceLocation.fromNamespaceAndPath("cesg", path);
    }

    public static void shulkerLoader(DataGenContext<Block, ?> ctx, RegistrateBlockstateProvider prov) {
        shulkerStation(ctx, prov, "shulker_loader");
    }

    /** Same facing rotations as vanilla {@link net.minecraft.world.level.block.BarrelBlock}. */
    public static void enderBarrel(DataGenContext<Block, ?> ctx, RegistrateBlockstateProvider prov) {
        ModelFile model = prov.models().cubeBottomTop(ctx.getName(),
                cesg("block/ender_barrel_side"),
                cesg("block/ender_barrel_bottom"),
                cesg("block/ender_barrel_top"));
        prov.getVariantBuilder(ctx.getEntry()).forAllStates(state -> {
            Direction facing = state.getValue(EnderBarrelBlock.FACING);
            int rotationX = 0;
            int rotationY = 0;
            switch (facing) {
                case DOWN -> rotationX = 180;
                case EAST -> {
                    rotationX = 90;
                    rotationY = 90;
                }
                case NORTH -> rotationX = 90;
                case SOUTH -> {
                    rotationX = 90;
                    rotationY = 180;
                }
                case WEST -> {
                    rotationX = 90;
                    rotationY = 270;
                }
                default -> {
                }
            }
            return ConfiguredModel.builder()
                    .modelFile(model)
                    .rotationX(rotationX)
                    .rotationY(rotationY)
                    .build();
        });
    }

    public static void shulkerLoaderItem(DataGenContext<Item, ?> ctx, RegistrateItemModelProvider prov) {
        prov.withExistingParent(ctx.getName(), cesg("block/shulker_loader/item"));
    }

    public static void shulkerUnloader(DataGenContext<Block, ?> ctx, RegistrateBlockstateProvider prov) {
        shulkerStation(ctx, prov, "shulker_unloader");
    }

    public static void shulkerUnloaderItem(DataGenContext<Item, ?> ctx, RegistrateItemModelProvider prov) {
        prov.withExistingParent(ctx.getName(), cesg("block/shulker_unloader/item"));
    }

    private static void shulkerStation(DataGenContext<Block, ?> ctx, RegistrateBlockstateProvider prov, String name) {
        ModelFile model = prov.models().getExistingFile(cesg("block/" + name + "/block"));
        prov.getVariantBuilder(ctx.getEntry()).forAllStates(state -> {
            Direction facing = state.getValue(DirectionalKineticBlock.FACING);
            if (facing == null) {
                throw new IllegalStateException("Missing FACING property on " + state);
            }
            int[] rotations = switch (facing) {
                case UP -> new int[]{270, 0};
                case DOWN -> new int[]{90, 0};
                case EAST -> new int[]{0, 90};
                case SOUTH -> new int[]{0, 180};
                case WEST -> new int[]{0, 270};
                case NORTH -> new int[]{0, 0};
            };
            return ConfiguredModel.builder()
                    .modelFile(model)
                    .rotationX(rotations[0])
                    .rotationY(rotations[1])
                    .uvLock(true)
                    .build();
        });
    }

    public static void shulkerBeltLoader(DataGenContext<Block, ?> ctx, RegistrateBlockstateProvider prov) {
        shulkerStation(ctx, prov, "shulker_belt_loader");
    }

    public static void shulkerBeltLoaderItem(DataGenContext<Item, ?> ctx, RegistrateItemModelProvider prov) {
        prov.withExistingParent(ctx.getName(), cesg("block/shulker_belt_loader/item"));
    }

    public static void shulkerBeltUnloader(DataGenContext<Block, ?> ctx, RegistrateBlockstateProvider prov) {
        shulkerStation(ctx, prov, "shulker_belt_unloader");
    }

    public static void shulkerBeltUnloaderItem(DataGenContext<Item, ?> ctx, RegistrateItemModelProvider prov) {
        prov.withExistingParent(ctx.getName(), cesg("block/shulker_belt_unloader/item"));
    }

    public static void shulkerCage(DataGenContext<Block, ?> ctx, RegistrateBlockstateProvider prov) {
        // Parent supplies the geometry (outer cube + inner faces so the bars show from both sides);
        // here we just bind the texture and cutout render type.
        prov.simpleBlock(ctx.getEntry(),
                prov.models().withExistingParent(ctx.getName(), cesg("block/shulker_cage_frame"))
                        .renderType("minecraft:cutout")
                        .texture("all", cesg("block/shulker_cage"))
                        .texture("particle", cesg("block/shulker_cage")));
    }

    public static void endGateway(DataGenContext<Block, ?> ctx, RegistrateBlockstateProvider prov) {
        ModelFile model = prov.models().getExistingFile(cesg("block/end_gateway/block"));
        prov.getVariantBuilder(ctx.getEntry()).forAllStates(state -> {
            Direction facing = state.getValue(DirectionalKineticBlock.FACING);
            int x = 0;
            int y = 0;
            switch (facing) {
                case UP -> x = 270;
                case DOWN -> x = 90;
                case SOUTH -> y = 180;
                case EAST -> y = 90;
                case WEST -> y = 270;
                default -> { }
            }
            return ConfiguredModel.builder().modelFile(model).rotationX(x).rotationY(y).uvLock(true).build();
        });
    }

    public static void endGatewayItem(DataGenContext<Item, ?> ctx, RegistrateItemModelProvider prov) {
        prov.withExistingParent(ctx.getName(), cesg("block/end_gateway/block"));
    }

    public static void enderInfuser(DataGenContext<Block, ?> ctx, RegistrateBlockstateProvider prov) {
        ModelFile model = prov.models().getExistingFile(cesg("block/ender_infuser"));
        prov.getVariantBuilder(ctx.getEntry()).forAllStates(state -> {
            Direction facing = state.getValue(DirectionalKineticBlock.FACING);
            int x = 0;
            int y = 0;
            switch (facing) {
                case UP -> x = 270;
                case DOWN -> x = 90;
                case SOUTH -> y = 180;
                case EAST -> y = 90;
                case WEST -> y = 270;
                default -> { }
            }
            return ConfiguredModel.builder().modelFile(model).rotationX(x).rotationY(y).uvLock(true).build();
        });
    }

    public static void enderInfuserItem(DataGenContext<Item, ?> ctx, RegistrateItemModelProvider prov) {
        prov.withExistingParent(ctx.getName(), cesg("block/ender_infuser"));
    }

    public static void crossDimensionalGatewayCore(DataGenContext<Block, ?> ctx, RegistrateBlockstateProvider prov) {
        ModelFile unlit = prov.models().getExistingFile(cesg("block/cross_dimensional_gateway_core/block"));
        var fuelTextures = java.util.Map.of(
                com.cesg.gateways.GatewayFrameBlock.FrameFuel.ESSENCE,
                new String[] { "cross_dimensional_gateway_core_lit_essence", "cross_dimensional_gateway_core_side_lit_essence" },
                com.cesg.gateways.GatewayFrameBlock.FrameFuel.EYE,
                new String[] { "cross_dimensional_gateway_core_lit_eye", "cross_dimensional_gateway_core_side_lit_eye" });
        java.util.Map<com.cesg.gateways.GatewayFrameBlock.FrameFuel, ModelFile> litModels = new java.util.EnumMap<>(
                com.cesg.gateways.GatewayFrameBlock.FrameFuel.class);
        for (var entry : fuelTextures.entrySet()) {
            litModels.put(entry.getKey(), prov.models()
                    .withExistingParent(ctx.getName() + "_lit_" + entry.getKey().getSerializedName(),
                            cesg("block/cross_dimensional_gateway_core/block"))
                    .renderType("translucent")
                    .texture("front", cesg("block/" + entry.getValue()[0]))
                    .texture("side", cesg("block/" + entry.getValue()[1]))
                    .texture("particle", cesg("block/" + entry.getValue()[1])));
        }
        prov.getVariantBuilder(ctx.getEntry()).forAllStates(state -> {
            Direction facing = state.getValue(DirectionalKineticBlock.FACING);
            boolean isLit = state.getValue(com.cesg.gateways.CrossDimensionalGatewayCoreBlock.LIT);
            com.cesg.gateways.GatewayFrameBlock.FrameFuel fuel =
                    state.getValue(com.cesg.gateways.CrossDimensionalGatewayCoreBlock.FUEL);
            int x = 0;
            int y = 0;
            switch (facing) {
                case UP -> x = 270;
                case DOWN -> x = 90;
                case SOUTH -> y = 180;
                case EAST -> y = 90;
                case WEST -> y = 270;
                default -> { }
            }
            ModelFile model = !isLit || fuel == com.cesg.gateways.GatewayFrameBlock.FrameFuel.NONE
                    ? unlit
                    : litModels.get(fuel);
            return ConfiguredModel.builder().modelFile(model)
                    .rotationX(x).rotationY(y).uvLock(true).build();
        });
    }

    public static void crossDimensionalGatewayCoreItem(DataGenContext<Item, ?> ctx, RegistrateItemModelProvider prov) {
        prov.withExistingParent(ctx.getName(), cesg("block/cross_dimensional_gateway_core/block"));
    }

    public static void gatewayFrame(DataGenContext<Block, ?> ctx, RegistrateBlockstateProvider prov) {
        ModelFile glassOff = prov.models().withExistingParent(ctx.getName(), cesg("block/gateway_frame_pane"))
                .renderType("translucent")
                .texture("all", cesg("block/gateway_frame"))
                .texture("particle", cesg("block/gateway_frame"));
        ModelFile glassOn = prov.models().withExistingParent(ctx.getName() + "_lit", cesg("block/gateway_frame_pane"))
                .renderType("translucent")
                .texture("all", cesg("block/gateway_frame_lit"))
                .texture("particle", cesg("block/gateway_frame_lit"));

        var fuels = java.util.Map.of(
                com.cesg.gateways.GatewayFrameBlock.FrameFuel.ESSENCE, cesg("block/teleport_essence_still"),
                com.cesg.gateways.GatewayFrameBlock.FrameFuel.EYE, cesg("block/liquid_eye_of_ender_still"));

        var builder = prov.getMultipartBuilder(ctx.getEntry());
        builder.part().modelFile(glassOff).addModel()
                .condition(com.cesg.gateways.GatewayFrameBlock.LIT, false).end();
        builder.part().modelFile(glassOn).addModel()
                .condition(com.cesg.gateways.GatewayFrameBlock.LIT, true).end();

        // Conduit: hub always (when fueled) + a beam toward each connected ring neighbor.
        // Beam model points NORTH; other directions via rotation.
        int[][] rot = { {0, 0}, {0, 180}, {0, 90}, {0, 270}, {270, 0}, {90, 0} }; // N S E W U D
        net.minecraft.core.Direction[] dirs = {
                net.minecraft.core.Direction.NORTH, net.minecraft.core.Direction.SOUTH,
                net.minecraft.core.Direction.EAST, net.minecraft.core.Direction.WEST,
                net.minecraft.core.Direction.UP, net.minecraft.core.Direction.DOWN };
        for (var fuel : fuels.entrySet()) {
            String suffix = "_" + fuel.getKey().getSerializedName();
            ModelFile hub = prov.models().withExistingParent("gateway_conduit_hub" + suffix, cesg("block/gateway_conduit_hub"))
                    .renderType("translucent")
                    .texture("fluid", fuel.getValue())
                    .texture("particle", fuel.getValue());
            ModelFile beam = prov.models().withExistingParent("gateway_conduit_beam" + suffix, cesg("block/gateway_conduit_beam"))
                    .renderType("translucent")
                    .texture("fluid", fuel.getValue())
                    .texture("particle", fuel.getValue());
            builder.part().modelFile(hub).addModel()
                    .condition(com.cesg.gateways.GatewayFrameBlock.FUEL, fuel.getKey()).end();
            for (int i = 0; i < dirs.length; i++) {
                builder.part().modelFile(beam).rotationX(rot[i][0]).rotationY(rot[i][1]).addModel()
                        .condition(com.cesg.gateways.GatewayFrameBlock.FUEL, fuel.getKey())
                        .condition(com.cesg.gateways.GatewayFrameBlock.CONNECTIONS.get(dirs[i]),
                                com.cesg.gateways.GatewayFrameBlock.ConduitLink.FRAME,
                                com.cesg.gateways.GatewayFrameBlock.ConduitLink.CORE).end();
            }
        }

        // Metal docking socket toward the core: fuel-agnostic, textured like the core itself.
        ModelFile collar = prov.models().withExistingParent("gateway_conduit_collar_core", cesg("block/gateway_conduit_collar"))
                .renderType("translucent")
                .texture("collar", cesg("block/cross_dimensional_gateway_core_side"))
                .texture("particle", cesg("block/cross_dimensional_gateway_core_side"));
        for (int i = 0; i < dirs.length; i++) {
            builder.part().modelFile(collar).rotationX(rot[i][0]).rotationY(rot[i][1]).addModel()
                    .condition(com.cesg.gateways.GatewayFrameBlock.FUEL,
                            com.cesg.gateways.GatewayFrameBlock.FrameFuel.ESSENCE,
                            com.cesg.gateways.GatewayFrameBlock.FrameFuel.EYE)
                    .condition(com.cesg.gateways.GatewayFrameBlock.CONNECTIONS.get(dirs[i]),
                            com.cesg.gateways.GatewayFrameBlock.ConduitLink.CORE).end();
        }
    }

    /** Gateway Port (Phase 6A logistics endpoint): plain cube with the custom port texture. */
    public static void gatewayPort(DataGenContext<Block, ?> ctx, RegistrateBlockstateProvider prov) {
        prov.simpleBlock(ctx.getEntry(), prov.models().cubeAll(ctx.getName(), cesg("block/gateway_port")));
    }

    /**
     * Cross-Dimensional Storage Bridge (Phase 7H): brass casing with a teal conduit band and a gateway-violet
     * collar. The band's centre pip is driven by {@link com.cesg.gateways.StorageBridgeBlock#STATUS}, so
     * OFFLINE / LIVE / FAULT are legible on the placed block. The block attaches to a ring from any of its
     * six sides, so every side face is identical — no facing property to contradict.
     *
     * <p>Geometry comes from the hand-authored {@code block/storage_bridge/body}, which sinks the gauge 1px
     * into all four sides and the top and closes each recess with a flush pane of glass, so the indicator
     * reads as a built-in lens rather than a painted-on square. OFFLINE uses that model as-is; LIVE and
     * FAULT swap to {@code body_lit}, whose gauge faces are full-bright — the same geometry, backlit.
     */
    public static void storageBridge(DataGenContext<Block, ?> ctx, RegistrateBlockstateProvider prov) {
        prov.getVariantBuilder(ctx.getEntry()).forAllStates(state -> {
            boolean offline = state.getValue(com.cesg.gateways.StorageBridgeBlock.STATUS)
                    == com.cesg.gateways.StorageBridgeBlockEntity.RemoteStatus.OFFLINE;
            String suffix = switch (state.getValue(com.cesg.gateways.StorageBridgeBlock.STATUS)) {
                case OFFLINE -> "";
                case LIVE -> "_live";
                // UNLINKED reuses the fault art deliberately: it is a wiring problem the player must fix,
                // and a fourth texture set would reopen the 7H art pass for no extra information.
                case FAULT, UNLINKED -> "_fault";
            };
            return ConfiguredModel.builder()
                    .modelFile(prov.models()
                            .withExistingParent(ctx.getName() + suffix,
                                    cesg("block/storage_bridge/body" + (offline ? "" : "_lit")))
                            .renderType("translucent")
                            .texture("side", cesg("block/storage_bridge" + suffix))
                            .texture("top", cesg("block/storage_bridge_top" + suffix))
                            .texture("bottom", cesg("block/storage_bridge_bottom")))
                    .build();
        });
    }

    /**
     * Gateway Flux Battery (Phase 7E / 7H): Create fluid-tank style TOP/BOTTOM/SHAPE variants pointing at
     * hand-authored models under {@code models/block/gateway_flux_battery/}.
     */
    public static void gatewayFluxBattery(DataGenContext<Block, ?> ctx, RegistrateBlockstateProvider prov) {
        prov.getVariantBuilder(ctx.getEntry()).forAllStates(state -> {
            boolean top = state.getValue(com.cesg.gateways.GatewayFluxBatteryBlock.TOP);
            boolean bottom = state.getValue(com.cesg.gateways.GatewayFluxBatteryBlock.BOTTOM);
            com.cesg.gateways.GatewayFluxBatteryBlock.Shape shape =
                    state.getValue(com.cesg.gateways.GatewayFluxBatteryBlock.SHAPE);

            String shapeName = "middle";
            if (top && bottom)
                shapeName = "single";
            else if (top)
                shapeName = "top";
            else if (bottom)
                shapeName = "bottom";

            String modelName = shapeName
                    + (shape == com.cesg.gateways.GatewayFluxBatteryBlock.Shape.PLAIN
                            ? ""
                            : "_" + shape.getSerializedName());
            ModelFile model = prov.models()
                    .getExistingFile(cesg("block/gateway_flux_battery/block_" + modelName));
            return ConfiguredModel.builder().modelFile(model).build();
        });
    }

    /** Inventory model parents the windowed single-block tank. */
    public static void gatewayFluxBatteryItem(DataGenContext<Item, ?> ctx, RegistrateItemModelProvider prov) {
        prov.withExistingParent(ctx.getName(), cesg("block/gateway_flux_battery/block_single_window"));
    }

    /** Animated ender teal-purple portal plane (custom models referencing cesg:block/gateway_portal). */
    public static void gatewayPortal(DataGenContext<Block, ?> ctx, RegistrateBlockstateProvider prov) {
        ModelFile ns = prov.models().getExistingFile(cesg("block/gateway_portal_ns"));
        ModelFile ew = prov.models().getExistingFile(cesg("block/gateway_portal_ew"));
        prov.getVariantBuilder(ctx.getEntry()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(com.cesg.gateways.GatewayPortalBlock.AXIS) == Direction.Axis.Z ? ew : ns)
                .build());
    }

    public static void enhancedShulkerBox(DataGenContext<Block, ?> ctx, RegistrateBlockstateProvider prov,
            @Nullable DyeColor color) {
        ResourceLocation model = vanillaShulkerBlockModel(color);
        prov.getVariantBuilder(ctx.getEntry()).forAllStates(state -> {
            Direction facing = state.getValue(ShulkerBoxBlock.FACING);
            int x = 0;
            int y = 0;
            switch (facing) {
                case DOWN -> x = 180;
                case UP -> x = 0;
                case NORTH -> y = 0;
                case SOUTH -> y = 180;
                case WEST -> y = 270;
                case EAST -> y = 90;
            }
            return ConfiguredModel.builder()
                    .modelFile(prov.models().getExistingFile(model))
                    .rotationX(x)
                    .rotationY(y)
                    .build();
        });
    }

    public static void enhancedShulkerBoxItem(DataGenContext<Item, ?> ctx, RegistrateItemModelProvider prov,
            @Nullable DyeColor color) {
        prov.withExistingParent(ctx.getName(), vanillaShulkerItemModel(color));
    }

    private static ResourceLocation vanillaShulkerBlockModel(@Nullable DyeColor color) {
        if (color == null)
            return mc("block/shulker_box");
        return mc("block/" + color.getName() + "_shulker_box");
    }

    private static ResourceLocation vanillaShulkerItemModel(@Nullable DyeColor color) {
        if (color == null)
            return mc("item/shulker_box");
        return mc("item/" + color.getName() + "_shulker_box");
    }

    public static void stackDepthUpgrade(DataGenContext<Item, ?> ctx, RegistrateItemModelProvider prov, int tier) {
        prov.withExistingParent(ctx.getName(), mc("item/generated"))
                .texture("layer0", cesg("item/stack_depth_upgrade_t" + tier));
    }

    public static void filterUpgrade(DataGenContext<Item, ?> ctx, RegistrateItemModelProvider prov) {
        prov.withExistingParent(ctx.getName(), mc("item/generated"))
                .texture("layer0", cesg("item/filter_upgrade"));
    }

    public static void compactingUpgrade(DataGenContext<Item, ?> ctx, RegistrateItemModelProvider prov) {
        prov.withExistingParent(ctx.getName(), mc("item/generated"))
                .texture("layer0", cesg("item/compacting_upgrade"));
    }

    public static void shulkerShell(DataGenContext<Item, ?> ctx, RegistrateItemModelProvider prov) {
        prov.withExistingParent(ctx.getName(), mc("item/generated"))
                .texture("layer0", cesg("item/shulker_shell"));
    }

    public static void enderPearlDust(DataGenContext<Item, ?> ctx, RegistrateItemModelProvider prov) {
        prov.withExistingParent(ctx.getName(), mc("item/generated"))
                .texture("layer0", cesg("item/ender_pearl_dust"));
    }

    public static void gatewayBindingItem(DataGenContext<Item, ?> ctx, RegistrateItemModelProvider prov) {
        prov.withExistingParent(ctx.getName(), mc("item/generated"))
                .texture("layer0", cesg("item/gateway_binding_item"));
    }

    public static void emergencyEyeCharge(DataGenContext<Item, ?> ctx, RegistrateItemModelProvider prov) {
        prov.withExistingParent(ctx.getName(), mc("item/generated"))
                .texture("layer0", cesg("item/emergency_eye_charge"));
    }

    public static void teleportEssenceBucket(DataGenContext<Item, ?> ctx, RegistrateItemModelProvider prov) {
        prov.withExistingParent(ctx.getName(), mc("item/generated"))
                .texture("layer0", cesg("item/teleport_essence_bucket"));
    }

    public static void liquidEyeOfEnderBucket(DataGenContext<Item, ?> ctx, RegistrateItemModelProvider prov) {
        prov.withExistingParent(ctx.getName(), mc("item/generated"))
                .texture("layer0", cesg("item/liquid_eye_of_ender_bucket"));
    }
}
