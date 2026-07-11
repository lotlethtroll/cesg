package com.cesg.datagen;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.cesg.CESG;
import com.cesg.init.CESGRegistration;
import com.cesg.upgrades.EnhancedShulkerBoxes;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Phase 6C advancement tree: root -> first station / enhanced shulker -> tier upgrade,
 * root -> gateway built -> first cross-dimensional travel (awarded in code by TeleportResolver,
 * since gateway travel never fires a vanilla trigger).
 */
public class CESGAdvancementProvider extends AdvancementProvider {
    public CESGAdvancementProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries,
            ExistingFileHelper existingFileHelper) {
        super(output, registries, existingFileHelper, List.of(CESGAdvancementProvider::generate));
    }

    private static void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> saver,
            ExistingFileHelper files) {
        AdvancementHolder root = Advancement.Builder.advancement()
                .display(EnhancedShulkerBoxes.DEFAULT.get(),
                        title("root"), description("root"),
                        ResourceLocation.withDefaultNamespace("textures/block/end_stone_bricks.png"),
                        AdvancementType.TASK, false, false, false)
                .addCriterion("has_shulker_box", InventoryChangeTrigger.TriggerInstance.hasItems(
                        anyOf(net.minecraft.world.item.Items.SHULKER_BOX, EnhancedShulkerBoxes.DEFAULT.get(),
                                CESGRegistration.SHULKER_SHELL.get())))
                .save(saver, CESG.id("root"), files);

        AdvancementHolder station = Advancement.Builder.advancement()
                .parent(root)
                .display(CESGRegistration.SHULKER_LOADER.get(),
                        title("first_station"), description("first_station"),
                        null, AdvancementType.TASK, true, true, false)
                .addCriterion("has_station", InventoryChangeTrigger.TriggerInstance.hasItems(
                        anyOf(CESGRegistration.SHULKER_LOADER.get(), CESGRegistration.SHULKER_UNLOADER.get(),
                                CESGRegistration.SHULKER_BELT_LOADER.get(),
                                CESGRegistration.SHULKER_BELT_UNLOADER.get())))
                .save(saver, CESG.id("first_station"), files);

        AdvancementHolder enhanced = Advancement.Builder.advancement()
                .parent(root)
                .display(EnhancedShulkerBoxes.DEFAULT.get(),
                        title("enhanced_shulker"), description("enhanced_shulker"),
                        null, AdvancementType.TASK, true, true, false)
                .addCriterion("has_enhanced_shulker", InventoryChangeTrigger.TriggerInstance.hasItems(
                        anyOf(EnhancedShulkerBoxes.allEntries().stream()
                                .map(entry -> (ItemLike) entry.get())
                                .toArray(ItemLike[]::new))))
                .save(saver, CESG.id("enhanced_shulker"), files);

        Advancement.Builder.advancement()
                .parent(enhanced)
                .display(CESGRegistration.STACK_DEPTH_UPGRADE_T1.get(),
                        title("tier_upgrade"), description("tier_upgrade"),
                        null, AdvancementType.GOAL, true, true, false)
                .addCriterion("has_tier_upgrade", InventoryChangeTrigger.TriggerInstance.hasItems(
                        anyOf(CESGRegistration.STACK_DEPTH_UPGRADE_T1.get(),
                                CESGRegistration.STACK_DEPTH_UPGRADE_T2.get(),
                                CESGRegistration.STACK_DEPTH_UPGRADE_T3.get())))
                .save(saver, CESG.id("tier_upgrade"), files);

        AdvancementHolder gateway = Advancement.Builder.advancement()
                .parent(root)
                .display(CESGRegistration.CROSS_DIMENSIONAL_GATEWAY_CORE.get(),
                        title("gateway_built"), description("gateway_built"),
                        null, AdvancementType.GOAL, true, true, false)
                .addCriterion("has_gateway_core", InventoryChangeTrigger.TriggerInstance.hasItems(
                        CESGRegistration.CROSS_DIMENSIONAL_GATEWAY_CORE.get()))
                .save(saver, CESG.id("gateway_built"), files);

        Advancement.Builder.advancement()
                .parent(gateway)
                .display(CESGRegistration.GATEWAY_BINDING_ITEM.get(),
                        title("gateway_travel"), description("gateway_travel"),
                        null, AdvancementType.CHALLENGE, true, true, false)
                .addCriterion(com.cesg.CESGIds.GATEWAY_TRAVEL_CRITERION,
                        CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()))
                .save(saver, com.cesg.CESGIds.GATEWAY_TRAVEL_ADVANCEMENT, files);
    }

    private static ItemPredicate anyOf(ItemLike... items) {
        return ItemPredicate.Builder.item().of(items).build();
    }

    private static Component title(String id) {
        return Component.translatable("advancement.cesg." + id);
    }

    private static Component description(String id) {
        return Component.translatable("advancement.cesg." + id + ".desc");
    }
}
