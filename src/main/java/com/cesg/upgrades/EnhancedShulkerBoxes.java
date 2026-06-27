package com.cesg.upgrades;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.cesg.CESG;
import com.cesg.datagen.CESGPlaceholderModels;
import com.cesg.init.CESGDataComponents;
import com.simibubi.create.foundation.item.ItemDescription;

import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.core.registries.Registries;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;

import org.jetbrains.annotations.Nullable;

public final class EnhancedShulkerBoxes {
    public static final int TIER = 2;

    private static final List<BlockEntry<EnhancedShulkerBoxBlock>> ALL = new ArrayList<>();
    private static final Map<DyeColor, BlockEntry<EnhancedShulkerBoxBlock>> BY_COLOR = new EnumMap<>(DyeColor.class);

    public static BlockEntry<EnhancedShulkerBoxBlock> DEFAULT;

    private EnhancedShulkerBoxes() {}

    public static void register() {
        DEFAULT = registerBox(null);
        for (DyeColor color : DyeColor.values())
            BY_COLOR.put(color, registerBox(color));
    }

    private static BlockEntry<EnhancedShulkerBoxBlock> registerBox(@Nullable DyeColor color) {
        String name = color == null ? "enhanced_shulker_box" : "enhanced_" + color.getName() + "_shulker_box";
        Block vanilla = vanillaCounterpart(color);
        BlockEntry<EnhancedShulkerBoxBlock> entry = CESG.REGISTRATE.block(name,
                        props -> new EnhancedShulkerBoxBlock(color, props))
                .initialProperties(() -> vanilla)
                .blockstate((ctx, prov) -> CESGPlaceholderModels.enhancedShulkerBox(ctx, prov, color))
                .item((block, props) -> new EnhancedShulkerBoxItem(block, props.stacksTo(1), TIER))
                .onRegisterAfter(Registries.ITEM, item -> ItemDescription.useKey(item, "item.cesg.enhanced_shulker_box"))
                .tag(CESGDataComponents.ENHANCED_SHULKER_ITEM)
                .model((ctx, prov) -> CESGPlaceholderModels.enhancedShulkerBoxItem(ctx, prov, color))
                .build()
                .register();
        ALL.add(entry);
        return entry;
    }

    private static Block vanillaCounterpart(@Nullable DyeColor color) {
        return color == null ? Blocks.SHULKER_BOX : ShulkerBoxBlock.getBlockByColor(color);
    }

    public static Block[] allBlocks() {
        return ALL.stream().map(BlockEntry::get).toArray(Block[]::new);
    }

    public static List<BlockEntry<EnhancedShulkerBoxBlock>> allEntries() {
        return Collections.unmodifiableList(ALL);
    }

    public static BlockEntry<EnhancedShulkerBoxBlock> byColor(@Nullable DyeColor color) {
        return color == null ? DEFAULT : BY_COLOR.get(color);
    }

    public static boolean isEnhancedShulker(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof EnhancedShulkerBoxItem;
    }

    public static boolean isEnhancedShulker(Item item) {
        return item instanceof EnhancedShulkerBoxItem;
    }

    /** Preview / creative helper: enhanced shulker at the requested tier and dye color. */
    public static ItemStack stackWithTier(int tier, @Nullable DyeColor color) {
        ItemStack stack = new ItemStack(byColor(color).get());
        stack.set(CESGDataComponents.ENHANCED_SHULKER.get(), EnhancedShulkerContents.forTier(tier));
        return stack;
    }

    public static ItemStack stackWithTier(int tier) {
        return stackWithTier(tier, null);
    }
}
