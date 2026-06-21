package com.cesg.datagen;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;

public final class CESGPlaceholderModels {
    private CESGPlaceholderModels() {}

    private static ResourceLocation create(String path) {
        return ResourceLocation.fromNamespaceAndPath("create", path);
    }

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

    public static void shulkerDuplicationAid(DataGenContext<Block, ?> ctx, RegistrateBlockstateProvider prov) {
        prov.simpleBlock(ctx.getEntry(), prov.models().cubeAll(ctx.getName(), mc("block/end_stone")));
    }

    public static void endGateway(DataGenContext<Block, ?> ctx, RegistrateBlockstateProvider prov) {
        prov.simpleBlock(ctx.getEntry(), prov.models().cubeAll(ctx.getName(), mc("block/purpur_block")));
    }

    public static void crossDimensionalGatewayCore(DataGenContext<Block, ?> ctx, RegistrateBlockstateProvider prov) {
        prov.simpleBlock(ctx.getEntry(), prov.models().cubeAll(ctx.getName(), mc("block/obsidian")));
    }

    public static void enhancedShulker(DataGenContext<Item, ?> ctx, RegistrateItemModelProvider prov) {
        prov.withExistingParent(ctx.getName(), mc("item/purple_shulker_box"));
    }

    public static void stackDepthUpgrade(DataGenContext<Item, ?> ctx, RegistrateItemModelProvider prov) {
        prov.withExistingParent(ctx.getName(), create("item/cogwheel"));
    }

    public static void filterUpgrade(DataGenContext<Item, ?> ctx, RegistrateItemModelProvider prov) {
        prov.withExistingParent(ctx.getName(), create("item/filter"));
    }

    public static void compactingUpgrade(DataGenContext<Item, ?> ctx, RegistrateItemModelProvider prov) {
        prov.withExistingParent(ctx.getName(), create("item/iron_sheet"));
    }

    public static void shulkerShell(DataGenContext<Item, ?> ctx, RegistrateItemModelProvider prov) {
        prov.withExistingParent(ctx.getName(), mc("item/shulker_shell"));
    }

    public static void gatewayBindingItem(DataGenContext<Item, ?> ctx, RegistrateItemModelProvider prov) {
        prov.withExistingParent(ctx.getName(), mc("item/amethyst_shard"));
    }

    public static void emergencyEyeCharge(DataGenContext<Item, ?> ctx, RegistrateItemModelProvider prov) {
        prov.withExistingParent(ctx.getName(), mc("item/ender_eye"));
    }

    public static void teleportEssenceBucket(DataGenContext<Item, ?> ctx, RegistrateItemModelProvider prov) {
        prov.withExistingParent(ctx.getName(), mc("item/water_bucket"));
    }

    public static void liquidEyeOfEnderBucket(DataGenContext<Item, ?> ctx, RegistrateItemModelProvider prov) {
        prov.withExistingParent(ctx.getName(), mc("item/lava_bucket"));
    }
}
