package com.cesg.datagen;

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
        prov.simpleBlock(ctx.getEntry(), prov.models().cubeAll(ctx.getName(), mc("block/purpur_block")));
    }

    public static void crossDimensionalGatewayCore(DataGenContext<Block, ?> ctx, RegistrateBlockstateProvider prov) {
        prov.simpleBlock(ctx.getEntry(), prov.models().cubeAll(ctx.getName(), mc("block/obsidian")));
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
