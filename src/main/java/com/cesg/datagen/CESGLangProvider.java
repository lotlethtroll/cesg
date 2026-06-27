package com.cesg.datagen;

import com.cesg.CESG;

import net.minecraft.data.PackOutput;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class CESGLangProvider extends LanguageProvider {
    public CESGLangProvider(PackOutput output) {
        super(output, CESG.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup.cesg.main", "Create: End Storage & Gateways");

        add("block.cesg.shulker_loader", "Shulker Loader");
        add("block.cesg.shulker_unloader", "Shulker Unloader");
        add("block.cesg.shulker_belt_loader", "Shulker Belt Loader");
        add("block.cesg.shulker_belt_unloader", "Shulker Belt Unloader");
        add("block.cesg.shulker_cage", "Shulker Cage");
        add("block.cesg.shulker_cage.occupied", "Holds a captured Shulker");
        add("block.cesg.shulker_cage.tooltip.summary",
                "_Traps_ a live shulker for _End_ shell farming, like vanilla duplication.");
        add("block.cesg.shulker_cage.tooltip.condition1", "When trapping a shulker");
        add("block.cesg.shulker_cage.tooltip.behaviour1",
                "_Right-click a live shulker_ with the cage to place it and _capture_ the mob.");
        add("block.cesg.shulker_cage.tooltip.condition2", "When powered in the End");
        add("block.cesg.shulker_cage.tooltip.behaviour2",
                "A _redstone-powered_ cage fires on a nearby _occupied cage_, knocking a _Shulker Shell_ loose on a short cooldown.");
        add("block.cesg.shulker_cage.tooltip.condition3", "When releasing");
        add("block.cesg.shulker_cage.tooltip.behaviour3",
                "_Right-click_ an occupied cage with an _empty hand_ to free the shulker.");
        add("block.cesg.end_gateway", "Fabricated End Gateway");
        add("block.cesg.cross_dimensional_gateway_core", "Cross-Dimensional Gateway Core");

        add("block.cesg.enhanced_shulker_box", "Shulker Box");
        add("item.cesg.enhanced_shulker_box", "Shulker Box");
        add("cesg.enhanced_shulker.upgrades", "Upgrades");
        add("cesg.enhanced_shulker.tier", " - Tier %s");
        for (DyeColor color : DyeColor.values()) {
            String id = "enhanced_" + color.getName() + "_shulker_box";
            String name = capitalize(color.getName()) + " Shulker Box";
            add("block.cesg." + id, name);
            add("item.cesg." + id, name);
        }

        add("item.cesg.stack_depth_upgrade_t1", "Stack Depth Upgrade Mk I");
        add("item.cesg.stack_depth_upgrade_t2", "Stack Depth Upgrade Mk II");
        add("item.cesg.stack_depth_upgrade_t3", "Stack Depth Upgrade Mk III");
        add("item.cesg.filter_upgrade", "Filter Upgrade");
        add("item.cesg.compacting_upgrade", "Compacting Upgrade");
        add("item.cesg.stack_depth_upgrade_t1.tooltip.summary",
                "_Raises stack limits to 128_ per slot when installed in an _Enhanced Shulker_.");
        add("item.cesg.stack_depth_upgrade_t2.tooltip.summary",
                "_Raises stack limits to 256_ per slot when installed in an _Enhanced Shulker_.");
        add("item.cesg.stack_depth_upgrade_t3.tooltip.summary",
                "_Raises stack limits to 512_ per slot when installed in an _Enhanced Shulker_.");
        add("item.cesg.filter_upgrade.tooltip.summary",
                "_Restricts_ which items can enter storage when installed in an _Enhanced Shulker_.");
        add("item.cesg.compacting_upgrade.tooltip.summary",
                "_Combines_ partial stacks automatically when installed in an _Enhanced Shulker_.");
        add("item.cesg.stack_depth_upgrade_t1.tooltip.condition1", "When installed");
        add("item.cesg.stack_depth_upgrade_t1.tooltip.behaviour1",
                "Only modules in the _upgrade sidebar_ activate; spares kept in main storage are _inert_.");
        add("item.cesg.stack_depth_upgrade_t2.tooltip.condition1", "When installed");
        add("item.cesg.stack_depth_upgrade_t2.tooltip.behaviour1",
                "Only the _highest-tier_ stack depth module applies if several are installed.");
        add("item.cesg.stack_depth_upgrade_t3.tooltip.condition1", "When installed");
        add("item.cesg.stack_depth_upgrade_t3.tooltip.behaviour1",
                "Only the _highest-tier_ stack depth module applies if several are installed.");
        add("item.cesg.filter_upgrade.tooltip.condition1", "When installed");
        add("item.cesg.filter_upgrade.tooltip.behaviour1",
                "Configure the _filter slot_ below upgrade modules with any item or _Create filter_. Station filters still apply first.");
        add("item.cesg.compacting_upgrade.tooltip.condition1", "When installed");
        add("item.cesg.compacting_upgrade.tooltip.behaviour1",
                "_Merges partial stacks_ immediately in the GUI; automation merges up to _64 items_ per pass.");
        add("item.cesg.enhanced_shulker_box.tooltip.summary",
                "_Enhanced_ shulker storage with sidebar _upgrade modules_. _Tier 2_ has _54 slots_; craft _tier upgrades_ to reach _81_ and _108_ slots.");
        add("cesg.enhanced_shulker.tooltip.tier_slots", "Tier %s — %s storage slots, %s upgrade slot(s)");
        add("cesg.enhanced_shulker.tooltip.no_upgrades", "No upgrades installed");
        add("cesg.enhanced_shulker.tooltip.upgrades_header", "Installed upgrades:");

        add("cesg.recipe.enhanced_shulker_tier_2", "Upgrade Shulker to Enhanced Tier 2");
        add("cesg.recipe.enhanced_shulker_tier_3", "Upgrade Enhanced Shulker to Tier 3");
        add("cesg.recipe.enhanced_shulker_tier_4", "Upgrade Enhanced Shulker to Tier 4");
        add("cesg.enhanced_shulker.tooltip.upgrade_line", "• %s");
        add("cesg.goggles.enhanced_shulker.no_upgrades", "Upgrades: none installed");
        add("cesg.goggles.enhanced_shulker.tier_slots", "Tier %s — %s slots, %s upgrade slot(s)");
        add("cesg.goggles.enhanced_shulker.upgrade", "Upgrade: %s");
        add("cesg.goggles.enhanced_shulker.stack_limit", "Stack limit: %s per slot");
        add("cesg.goggles.enhanced_shulker.filter_all", "Shulker filter: accepts all items");
        add("cesg.goggles.enhanced_shulker.filter_set", "Shulker filter: %s");
        add("cesg.goggles.enhanced_shulker.filter_reject_hint",
                "Rejects items that do not match the configured filter on insert");
        add("cesg.goggles.enhanced_shulker.compacting",
                "Compacting: merges partial stacks (64 items/pass in automation)");
        add("cesg.goggles.enhanced_shulker.compacting_backlog",
                "Compacting backlog: partial stacks still merging");
        add("item.cesg.shulker_shell", "Processed Shulker Shell");
        add("item.cesg.shulker_shell.tooltip.summary",
                "_Refined_ shulker shell used in _Enhanced Shulker_ tier upgrades and Create machines.");
        add("item.cesg.shulker_shell.tooltip.condition1", "Obtaining");
        add("item.cesg.shulker_shell.tooltip.behaviour1",
                "_Refine_ a Shulker Shell — farm shells with a _Shulker Cage_ in the _End_, then _process_ them into this.");

        add("item.cesg.ender_pearl_dust", "Ender Pearl Dust");
        add("block.cesg.liquid_ender_pearl", "Liquid Ender Pearl");
        add("fluid_type.cesg.liquid_ender_pearl", "Liquid Ender Pearl");
        add("item.cesg.liquid_ender_pearl_bucket", "Bucket of Liquid Ender Pearl");
        add("item.cesg.teleport_essence_bucket", "Bucket of Teleport Essence");
        add("item.cesg.teleport_essence_bucket.tooltip.summary",
                "_Gateway fuel precursor_ — refined in Phase 4 gateway machines.");
        add("item.cesg.liquid_eye_of_ender_bucket", "Bucket of Liquid Eye of Ender");
        add("item.cesg.liquid_eye_of_ender_bucket.tooltip.summary",
                "_Advanced gateway fuel_ — powers cross-dimensional travel in Phase 4.");

        add("fluid.cesg.teleport_essence", "Teleport Essence");
        add("fluid.cesg.liquid_eye_of_ender", "Liquid Eye of Ender");

        add("cesg.station.retention.label", "Shulker Retention");
        add("cesg.station.retention.hold", "Hold");
        add("cesg.station.retention.auto_eject", "Auto Eject");
        add("cesg.station.fullness.label", "Eject When");
        add("cesg.station.fullness.all_slots.load", "All Slots Full");
        add("cesg.station.fullness.all_slots.unload", "All Slots Empty");
        add("cesg.station.fullness.slot_threshold.load", "Full Slot Threshold");
        add("cesg.station.fullness.slot_threshold.unload", "Empty Slot Threshold");
        add("cesg.station.threshold.label.load", "Full Slot Threshold");
        add("cesg.station.threshold.label.unload", "Remaining Slots");
        add("cesg.station.threshold.value.load", "%s full slots");
        add("cesg.station.threshold.value.unload", "%s slots remaining");
        add("cesg.station.config.title.loader", "Shulker Loader");
        add("cesg.station.config.title.belt_loader", "Shulker Belt Loader");
        add("cesg.station.config.title.unloader", "Shulker Unloader");
        add("cesg.station.config.title.belt_unloader", "Shulker Belt Unloader");
        add("cesg.station.name.label", "Name");
        add("cesg.station.name.hint", "Shulker name…");

        add("cesg.goggles.loader.station.empty", "No shulker docked");
        add("cesg.goggles.loader.station.dock_via_funnel", "Insert a shulker box from any funnel side to dock");
        add("cesg.goggles.loader.station.contents", "Docked: %s — %s/%s slots");
        add("cesg.goggles.loader.station.unpowered", "Unpowered — connect a spinning shaft to the back");
        add("cesg.goggles.loader.station.retention", "Retention: %s");
        add("cesg.goggles.loader.station.eject_when", "Eject when: %s");
        add("cesg.goggles.loader.station.threshold_load", "Eject after: %s full slot(s)");
        add("cesg.goggles.loader.station.funnel_connected", "Eject funnel: %s");
        add("cesg.goggles.loader.station.eject_funnel_hint", "Attach an extracting funnel to eject");
        add("cesg.goggles.loader.station.funnel_wrong_mode_hint",
                "Funnel is feeding the station; point the output belt away to eject");
        add("cesg.goggles.loader.station.ready_to_eject", "Ready for funnel extraction");
        add("cesg.goggles.loader.station.config_hint", "Sneak + use or use a wrench to configure");

        add("cesg.goggles.unloader.station.empty", "No shulker docked");
        add("cesg.goggles.unloader.station.dock_via_funnel", "Insert a shulker box from any funnel side to dock");
        add("cesg.goggles.unloader.station.contents", "Docked: %s — %s/%s slots");
        add("cesg.goggles.unloader.station.unpowered", "Unpowered — connect a spinning shaft to the back");
        add("cesg.goggles.unloader.station.retention", "Retention: %s");
        add("cesg.goggles.unloader.station.eject_when", "Eject when: %s");
        add("cesg.goggles.unloader.station.threshold_unload", "Eject with: %s slot(s) remaining");
        add("cesg.goggles.unloader.station.funnel_connected", "Eject funnel: %s");
        add("cesg.goggles.unloader.station.eject_funnel_hint", "Attach an extracting funnel to eject");
        add("cesg.goggles.unloader.station.funnel_wrong_mode_hint",
                "Funnel is feeding the station; point the output belt away to eject");
        add("cesg.goggles.unloader.station.ready_to_eject", "Ready for funnel extraction");
        add("cesg.goggles.unloader.station.config_hint", "Sneak + use or use a wrench to configure");

        add("cesg.goggles.station.filter_all", "Filter: accepts all items");
        add("cesg.goggles.station.filter_set", "Filter: %s");
        add("cesg.goggles.station.filter_hint",
                "Click any front or side filter slot with an item or Create filter");
        add("cesg.goggles.station.shulker_filter_all", "Docked shulker filter: accepts all items");
        add("cesg.goggles.station.shulker_filter_set", "Docked shulker filter: %s");
        add("cesg.goggles.station.filter_chain_insert",
                "Loaders require items to pass the station filter, then the shulker filter");
        add("cesg.goggles.station.filter_reject_insert_station",
                "Non-matching items are returned at the station filter");
        add("cesg.goggles.station.filter_reject_insert_shulker",
                "Non-matching items are returned at the docked shulker filter");
        add("cesg.goggles.station.filter_reject_extract",
                "Non-matching items stay in the shulker until the station filter allows them");
        add("cesg.goggles.station.filter_accepts_held", "Held item passes filters: %s");
        add("cesg.goggles.station.filter_rejects_held_station",
                "Held item blocked by station filter: %s");
        add("cesg.goggles.station.filter_rejects_held_shulker",
                "Held item blocked by docked shulker filter: %s");
        add("cesg.goggles.belt_loader.placement", "Place two blocks above a horizontal belt");
        add("cesg.goggles.belt_unloader.placement", "Place two blocks above a horizontal belt");

        add("cesg.goggles.station.names", "Stamps name: %s");

        add("cesg.goggles.shulker_cage.end_only", "End dimension required for shell drops");
        add("cesg.goggles.shulker_cage.empty", "No shulker trapped");
        add("cesg.goggles.shulker_cage.trap_hint", "Use the cage on a live shulker to trap it");
        add("cesg.goggles.shulker_cage.held", "Trapped: %s");
        add("cesg.goggles.shulker_cage.cooldown", "Cooldown: %ss");
        add("cesg.goggles.shulker_cage.ready", "Ready — power it near another cage to drop shells");

        add("item.cesg.gateway_binding_item", "Gateway Binding Crystal");
        add("item.cesg.emergency_eye_charge", "Emergency Eye Charge");
        add("cesg.goggles.gateway.fuel", "Gateway fluid: %s/%s mB");
        add("cesg.goggles.gateway.bound", "Bound to partner gateway");
        add("cesg.goggles.gateway.unbound", "Unbound — use a binding crystal");

        add("cesg.gateway.end_only", "Native gateway physics only function in the End");
        add("cesg.gateway.central_island", "Routed to central island");
        add("cesg.gateway.fallback", "Partner offline — emergency route to central island");
        add("cesg.gateway.unpowered", "Gateway requires power and fuel");
        add("cesg.gateway.denied", "Travel denied");
        add("cesg.gateway.invalid_partner", "Invalid partner gateway");
    }

    private static String capitalize(String value) {
        if (value.isEmpty())
            return value;
        String[] parts = value.split("_");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0)
                builder.append(' ');
            String part = parts[i];
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1)
                builder.append(part.substring(1));
        }
        return builder.toString();
    }
}
