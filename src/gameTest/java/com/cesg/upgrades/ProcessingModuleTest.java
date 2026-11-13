package com.cesg.upgrades;

import java.util.ArrayList;
import java.util.List;

import com.cesg.init.CESGDataComponents;
import com.cesg.init.CESGRegistration;
import com.cesg.upgrades.EnhancedShulkerContents.StoredSlot;
import com.simibubi.create.AllRecipeTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/** Haunting/Blasting modules, their priority against Crushing/Washing, and the Smelting module fixes. */
@GameTestHolder("cesg")
@PrefixGameTestTemplate(false)
public class ProcessingModuleTest {
    private static final BlockPos BOX = new BlockPos(1, 2, 1);

    @GameTest(template = "empty")
    public void hauntingLookupUsesCesgEndStone(GameTestHelper helper) {
        ResourceLocation id = AllRecipeTypes.HAUNTING
                .find(new SingleRecipeInput(new ItemStack(Items.SANDSTONE)), helper.getLevel())
                .map(RecipeHolder::id).orElse(null);
        helper.assertTrue(ResourceLocation.fromNamespaceAndPath("cesg", "end_stone_from_haunting").equals(id),
                "Haunting sandstone resolved to " + id);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void hauntingTurnsSandstoneIntoEndStone(GameTestHelper helper) {
        var box = placedBox(helper, List.of(new ItemStack(Items.SANDSTONE, 2)),
                new ItemStack(CESGRegistration.HAUNTING_UPGRADE_T2.get()));
        box.runProcessingModules();
        helper.assertTrue(count(box, Items.SANDSTONE) == 0 && count(box, Items.END_STONE) == 2,
                "Haunting Mk II should turn 2 sandstone into end stone: " + describe(box));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void blastingOnlyMeltsMetals(GameTestHelper helper) {
        var box = placedBox(helper, List.of(new ItemStack(Items.RAW_IRON, 2), new ItemStack(Items.SAND),
                new ItemStack(Items.COBBLESTONE), new ItemStack(Items.BEEF), new ItemStack(Items.IRON_SWORD)),
                new ItemStack(CESGRegistration.BLASTING_UPGRADE_T3.get()));
        box.runProcessingModules();
        box.runProcessingModules();
        helper.assertTrue(count(box, Items.RAW_IRON) == 0 && count(box, Items.IRON_INGOT) == 2,
                "Raw iron should blast into ingots: " + describe(box));
        helper.assertTrue(count(box, Items.SAND) == 1 && count(box, Items.COBBLESTONE) == 1
                && count(box, Items.BEEF) == 1 && count(box, Items.IRON_SWORD) == 1
                && count(box, Items.IRON_NUGGET) == 0, "Blasting touched a non-metal or a tool: " + describe(box));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void crushingTakesOreBeforeBlasting(GameTestHelper helper) {
        var box = placedBox(helper, List.of(new ItemStack(Items.IRON_ORE, 2)),
                new ItemStack(CESGRegistration.CRUSHING_UPGRADE_T1.get()),
                new ItemStack(CESGRegistration.BLASTING_UPGRADE_T1.get()));
        box.runProcessingModules();
        // Crushing Mk I takes one ore per pass; Blasting must leave the second ore for it.
        helper.assertTrue(count(box, Items.IRON_ORE) == 1, "Blasting took iron ore from Crushing: " + describe(box));
        box.runProcessingModules();
        helper.assertTrue(count(box, Items.IRON_ORE) == 0, "Crushing should take the second ore: " + describe(box));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void washingTakesSandBeforeHaunting(GameTestHelper helper) {
        var box = placedBox(helper, List.of(new ItemStack(Items.SAND, 2)),
                new ItemStack(CESGRegistration.HAUNTING_UPGRADE_T1.get()),
                new ItemStack(CESGRegistration.WASHING_UPGRADE_T1.get()));
        box.runProcessingModules();
        helper.assertTrue(count(box, Items.SAND) == 1 && count(box, Items.SOUL_SAND) == 0,
                "Haunting took sand from Washing: " + describe(box));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void fullBoxKeepsInputAndLosesNothing(GameTestHelper helper) {
        List<ItemStack> contents = new ArrayList<>();
        contents.add(new ItemStack(Items.SANDSTONE, 64));
        contents.add(new ItemStack(Items.RAW_IRON, 64));
        while (contents.size() < EnhancedShulkerContents.forTier(3).slotCount())
            contents.add(new ItemStack(Items.DIRT, 64));
        var box = placedBox(helper, contents, new ItemStack(CESGRegistration.HAUNTING_UPGRADE_T3.get()),
                new ItemStack(CESGRegistration.BLASTING_UPGRADE_T3.get()));
        for (int pass = 0; pass < 3; pass++)
            box.runProcessingModules();
        helper.assertTrue(count(box, Items.SANDSTONE) == 64 && count(box, Items.RAW_IRON) == 64
                && count(box, Items.DIRT) == 79 * 64 && count(box, Items.END_STONE) == 0
                && count(box, Items.IRON_INGOT) == 0, "Full box changed: " + describe(box));
        BlockPos pos = helper.absolutePos(BOX);
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3)).isEmpty(),
                "Full box dropped items");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public void placedHauntingBoxConvertsWithinTwoSeconds(GameTestHelper helper) {
        var box = placedBox(helper, List.of(new ItemStack(Items.SANDSTONE)),
                new ItemStack(CESGRegistration.HAUNTING_UPGRADE_T1.get()));
        helper.succeedWhen(() -> helper.assertTrue(count(box, Items.END_STONE) == 1,
                "Placed box has not haunted sandstone yet: " + describe(box)));
    }

    @GameTest(template = "empty")
    public void smeltingSkipsToolsAndLeavesOreForCrushing(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var smeltOnly = handler(level, new ItemStack(CESGRegistration.SMELTING_UPGRADE.get()));
        ItemHandlerHelper.insertItemStacked(smeltOnly, new ItemStack(Items.RAW_IRON), false);
        ItemHandlerHelper.insertItemStacked(smeltOnly, new ItemStack(Items.IRON_SWORD), false);
        helper.assertTrue(count(smeltOnly, Items.IRON_INGOT) == 1 && count(smeltOnly, Items.IRON_SWORD) == 1
                && count(smeltOnly, Items.IRON_NUGGET) == 0, "Smelting alone: " + describe(smeltOnly));

        var withCrushing = handler(level, new ItemStack(CESGRegistration.SMELTING_UPGRADE.get()),
                new ItemStack(CESGRegistration.CRUSHING_UPGRADE_T1.get()));
        ItemHandlerHelper.insertItemStacked(withCrushing, new ItemStack(Items.RAW_IRON), false);
        ItemHandlerHelper.insertItemStacked(withCrushing, new ItemStack(Items.SAND), false);
        helper.assertTrue(count(withCrushing, Items.RAW_IRON) == 1 && count(withCrushing, Items.IRON_INGOT) == 0,
                "Smelting should leave raw iron for Crushing: " + describe(withCrushing));
        helper.assertTrue(count(withCrushing, Items.GLASS) == 1, "Smelting should still smelt sand: " + describe(withCrushing));
        helper.succeed();
    }

    private static EnhancedShulkerBoxBlockEntity placedBox(GameTestHelper helper, List<ItemStack> contents,
            ItemStack... modules) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BOX);
        level.setBlock(pos, EnhancedShulkerBoxes.DEFAULT.get().defaultBlockState(), 3);
        ItemStack item = new ItemStack(EnhancedShulkerBoxes.DEFAULT.get());
        item.set(CESGDataComponents.ENHANCED_SHULKER.get(), contents(contents, modules));
        var box = (EnhancedShulkerBoxBlockEntity) level.getBlockEntity(pos);
        box.loadFromItem(item);
        return box;
    }

    private static EnhancedShulkerItemStackHandler handler(ServerLevel level, ItemStack... modules) {
        ItemStack item = new ItemStack(EnhancedShulkerBoxes.DEFAULT.get());
        EnhancedShulkerContents contents = contents(List.of(), modules);
        item.set(CESGDataComponents.ENHANCED_SHULKER.get(), contents);
        return new EnhancedShulkerItemStackHandler(item, contents, level);
    }

    private static EnhancedShulkerContents contents(List<ItemStack> stacks, ItemStack... modules) {
        var base = EnhancedShulkerContents.forTier(3);
        List<StoredSlot> main = new ArrayList<>();
        for (int i = 0; i < stacks.size(); i++)
            main.add(new StoredSlot(i, stacks.get(i)));
        List<StoredSlot> upgrades = new ArrayList<>();
        for (int i = 0; i < modules.length; i++)
            upgrades.add(new StoredSlot(i, modules[i]));
        return new EnhancedShulkerContents(3, base.slotCount(), base.upgradeSlotCount(), main, upgrades,
                ItemStack.EMPTY);
    }

    private static int count(EnhancedShulkerBoxBlockEntity box, Item item) {
        var contents = box.getShulkerStack().get(CESGDataComponents.ENHANCED_SHULKER.get());
        return contents.mainSlots().stream().filter(s -> s.item().is(item)).mapToInt(s -> s.item().getCount()).sum();
    }

    private static int count(EnhancedShulkerItemStackHandler handler, Item item) {
        int total = 0;
        for (int i = 0; i < handler.getSlots(); i++)
            if (handler.getStackInSlot(i).is(item))
                total += handler.getStackInSlot(i).getCount();
        return total;
    }

    private static String describe(EnhancedShulkerBoxBlockEntity box) {
        var contents = box.getShulkerStack().get(CESGDataComponents.ENHANCED_SHULKER.get());
        return contents.mainSlots().stream().map(s -> s.item().getCount() + "x" + s.item().getItem()).distinct()
                .limit(12).toList().toString();
    }

    private static String describe(EnhancedShulkerItemStackHandler handler) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < handler.getSlots(); i++)
            if (!handler.getStackInSlot(i).isEmpty())
                out.add(handler.getStackInSlot(i).toString());
        return out.toString();
    }
}
