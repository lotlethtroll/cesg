package com.cesg.decoration;

import java.util.ArrayList;
import java.util.List;

import com.cesg.CESG;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;

/**
 * Phase 6E decorative variants: smooth & polished end stone and purpur, each as a full family
 * (base block + stairs + slab + wall). Registrate handles default drop-self loot, item models and
 * auto-lang; slabs get the double-drop loot table and walls get an inventory item model.
 */
public final class CESGDecoratives {

    public record Family(String name, BlockEntry<Block> base, BlockEntry<StairBlock> stairs,
                         BlockEntry<SlabBlock> slab, BlockEntry<WallBlock> wall) {}

    private static final List<Family> FAMILIES = new ArrayList<>();

    private CESGDecoratives() {}

    public static List<Family> families() {
        return FAMILIES;
    }

    public static void register() {
        family("smooth_end_stone", Blocks.END_STONE);
        family("polished_end_stone", Blocks.END_STONE);
        family("smooth_purpur", Blocks.PURPUR_BLOCK);
        family("polished_purpur", Blocks.PURPUR_BLOCK);
    }

    private static void family(String name, Block source) {
        ResourceLocation tex = CESG.id("block/" + name);

        BlockEntry<Block> base = CESG.REGISTRATE.block(name, Block::new)
                .initialProperties(() -> source)
                .tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .blockstate((c, p) -> p.simpleBlock(c.get(), p.models().cubeAll(name, tex)))
                .item().build()
                .register();

        BlockEntry<StairBlock> stairs = CESG.REGISTRATE.block(name + "_stairs",
                        p -> new StairBlock(base.get().defaultBlockState(), p))
                .initialProperties(() -> source)
                .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.STAIRS)
                .blockstate((c, p) -> p.stairsBlock(c.get(), tex))
                .item().build()
                .register();

        BlockEntry<SlabBlock> slab = CESG.REGISTRATE.block(name + "_slab", SlabBlock::new)
                .initialProperties(() -> source)
                .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.SLABS)
                .blockstate((c, p) -> p.slabBlock(c.get(), tex, tex))
                .loot((tables, block) -> tables.add(block, tables.createSlabItemTable(block)))
                .item().build()
                .register();

        BlockEntry<WallBlock> wall = CESG.REGISTRATE.block(name + "_wall", WallBlock::new)
                .initialProperties(() -> source)
                .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.WALLS)
                .blockstate((c, p) -> p.wallBlock(c.get(), tex))
                .item().model((c, p) -> p.wallInventory(c.getName(), tex)).build()
                .register();

        FAMILIES.add(new Family(name, base, stairs, slab, wall));
    }
}
