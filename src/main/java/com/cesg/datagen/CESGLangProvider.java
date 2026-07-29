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
        add("block.cesg.shulker_loader.tooltip.summary",
                "Docks a _shulker box_ and loads it from adjacent item automation.");
        add("block.cesg.shulker_unloader.tooltip.summary",
                "Docks a _shulker box_ and extracts its contents into adjacent item automation.");
        add("block.cesg.shulker_belt_loader.tooltip.summary",
                "Extends a hose to _load a docked shulker_ from a belt two blocks below.");
        add("block.cesg.shulker_belt_unloader.tooltip.summary",
                "Extends a hose to _unload a docked shulker_ onto a belt two blocks below.");
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
        add("block.cesg.shulker_cage.tooltip.condition3", "When struck by a shulker bullet");
        add("block.cesg.shulker_cage.tooltip.behaviour3",
                "Any real _shulker bullet_ hitting an occupied cage in the _End_ knocks a _shell_ loose (same cooldown) — wild shulker crossfire is free shells.");
        add("block.cesg.shulker_cage.tooltip.condition4", "When releasing");
        add("block.cesg.shulker_cage.tooltip.behaviour4",
                "_Right-click_ an occupied cage with an _empty hand_ to free the shulker.");
        add("block.cesg.end_gateway", "Fabricated End Gateway");
        add("block.cesg.end_gateway.tooltip.summary",
                "_End-only gateway_ — right-click to return to the _central island_.");
        add("block.cesg.cross_dimensional_gateway_core", "Cross-Dimensional Gateway Core");
        add("block.cesg.cross_dimensional_gateway_core.tooltip.summary",
                "Controls a _Gateway Frame_ ring. _Power_, _fuel_, and _bind_ it, then _walk through_ the portal to travel.");
        add("block.cesg.gateway_port", "Gateway Port");
        add("block.cesg.gateway_port.tooltip.summary",
                "Cross-gateway logistics endpoint. Place beside a _Gateway Frame_ or _Core_; piped-in items and fluids emerge from a Gateway Port on the bound partner ring while the gateway is powered and fueled.");
        add("cesg.goggles.port.title", "Gateway Port");
        add("cesg.goggles.port.outgoing", "Outgoing: %s items, %s mB");
        add("cesg.goggles.port.incoming", "Incoming: %s items, %s mB");
        add("block.cesg.gateway_flux_battery", "Gateway Flux Battery");
        add("block.cesg.gateway_flux_battery.tooltip.summary",
                "Buffers _gateway fuel_ beside a _Gateway ring_ and keeps the _Core_ topped up so bursty travel and Port transfers never run the gateway dry. Single-fuel like a _fluid tank_ (locks to the first fuel piped in); _stack them into a 2×2×2 or 3×3×3 array_ for more capacity.");
        add("cesg.goggles.battery.title", "Gateway Flux Battery");
        add("cesg.goggles.battery.empty", "Empty: 0/%s mB");
        add("cesg.goggles.battery.stored", "%s: %s/%s mB");
        add("cesg.goggles.battery.array", "Array: %sx%sx%s");
        add("cesg.goggles.battery.reserve", "Travel reserve: %s mB (automation paused below this)");
        add("cesg.battery.fuel_added", "Added %s mB to the Flux Battery");
        add("cesg.battery.fuel_drained", "Drained %s mB from the Flux Battery");
        add("cesg.battery.need_bucket_space",
                "Need %s mB free for a full bucket (only %s mB free)");
        add("cesg.battery.need_bucket_fuel",
                "Need %s mB stored to fill a bucket (only %s mB)");
        add("cesg.battery.wrong_fuel", "Battery is locked to %s — empty it first");
        add("cesg.battery.gauge_moved", "Charge gauge moved to the %s face");
        add("cesg.direction.north", "north");
        add("cesg.direction.south", "south");
        add("cesg.direction.east", "east");
        add("cesg.direction.west", "west");
        add("block.cesg.storage_bridge", "Cross-Dimensional Storage Bridge");
        add("block.cesg.storage_bridge.tooltip.summary",
                "Links two _Storage Networks_ through a bound gateway. Place beside a _Gateway Frame_ or _Core_ that is _adjacent to a Storage Network_; a matching Bridge on the partner ring surfaces that network's items on this side's _Storage Terminal_ as a separate section. Moves items _both ways_ with per-side filters while the gateway is powered and fueled.");
        add("cesg.goggles.bridge.title", "Cross-Dimensional Storage Bridge");
        add("cesg.goggles.bridge.status.live", "Partner network: linked");
        add("cesg.goggles.bridge.status.offline", "Partner network: offline (unbound or unloaded)");
        add("cesg.goggles.bridge.status.fault", "Partner network: no Bridge or controller found");
        add("cesg.goggles.bridge.transit", "In transit: %s out, %s in");
        add("cesg.bridge.gui.title", "Storage Bridge");
        add("cesg.bridge.push", "Push → Partner");
        add("cesg.bridge.pull", "Pull ← Partner");
        add("cesg.bridge.on", "On");
        add("cesg.bridge.off", "Off");
        add("cesg.bridge.whitelist_short", "WL");
        add("cesg.bridge.blacklist_short", "BL");
        add("cesg.bridge.enable.tip", "Toggle unattended auto-transfer in this direction. Idle = no fuel cost.");
        add("cesg.bridge.whitelist.tip", "Whitelist: only the filtered items move (empty = nothing).");
        add("cesg.bridge.blacklist.tip", "Blacklist: every item moves except the filtered ones (empty = everything).");
        add("cesg.bridge.filter.hint", "Click with an item to filter this direction (the item is not consumed).");
        add("block.cesg.gateway_frame", "Gateway Frame");
        add("block.cesg.gateway_frame.tooltip.summary",
                "Build a _vertical ring_ of these around one _Gateway Core_ to form a walk-through portal. "
                        + "Leave _a block of space_ between neighbouring gateways — rings whose frames touch "
                        + "break each other.");
        add("block.cesg.gateway_portal", "Gateway Portal");

        add("block.cesg.enhanced_shulker_box", "Shulker Box");
        add("item.cesg.enhanced_shulker_box", "Shulker Box");
        add("cesg.enhanced_shulker.upgrades", "Upgrades");
        add("cesg.enhanced_shulker.tier_name.2", "Enhanced");
        add("cesg.enhanced_shulker.tier_name.3", "Reinforced");
        add("cesg.enhanced_shulker.tier_name.4", "Ultimate");
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
        add("item.cesg.smelting_upgrade", "Smelting Upgrade");
        add("item.cesg.void_upgrade", "Void Upgrade");
        add("item.cesg.magnet_upgrade_t1", "Magnet Upgrade Mk I");
        add("item.cesg.magnet_upgrade_t2", "Magnet Upgrade Mk II");
        add("item.cesg.magnet_upgrade_t3", "Magnet Upgrade Mk III");
        add("item.cesg.crushing_upgrade_t1", "Crushing Upgrade Mk I");
        add("item.cesg.crushing_upgrade_t2", "Crushing Upgrade Mk II");
        add("item.cesg.crushing_upgrade_t3", "Crushing Upgrade Mk III");
        add("item.cesg.washing_upgrade_t1", "Washing Upgrade Mk I");
        add("item.cesg.washing_upgrade_t2", "Washing Upgrade Mk II");
        add("item.cesg.washing_upgrade_t3", "Washing Upgrade Mk III");
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
        add("item.cesg.smelting_upgrade.tooltip.summary",
                "Items inserted into the box are stored as their _furnace result_ — ores arrive as ingots, food arrives cooked. Items without a smelting recipe store unchanged.");
        add("item.cesg.magnet_upgrade_t1.tooltip.summary",
                "A PLACED box pulls in dropped items it can accept within _4 blocks_. Only the highest-tier magnet module applies.");
        add("item.cesg.magnet_upgrade_t2.tooltip.summary",
                "A PLACED box pulls in dropped items it can accept within _7 blocks_, faster. Only the highest-tier magnet module applies.");
        add("item.cesg.magnet_upgrade_t3.tooltip.summary",
                "A PLACED box pulls in dropped items it can accept within _10 blocks_, fastest. Only the highest-tier magnet module applies.");
        add("item.cesg.void_upgrade.tooltip.summary",
                "Overflow of item types the box already stores is _destroyed_ instead of rejected. Types the box does not store are still rejected normally — a full box is not a trash can. Click the module's _crimson config slot_ with an item to void ONLY matching overflow (independent of the storage filter).");
        add("item.cesg.crushing_upgrade_t1.tooltip.summary",
                "A PLACED box _crushes and mills_ its contents using Create recipes (ores → crushed ore, cobble → gravel → sand), chaining to the final form. _1 conversion/sec_. Only the highest-tier crushing module applies.");
        add("item.cesg.crushing_upgrade_t2.tooltip.summary",
                "A PLACED box _crushes and mills_ its contents, chaining to the final form, at _2 conversions/sec_. Only the highest-tier crushing module applies.");
        add("item.cesg.crushing_upgrade_t3.tooltip.summary",
                "A PLACED box _crushes and mills_ its contents, chaining to the final form, at _4 conversions/sec_. Only the highest-tier crushing module applies.");
        add("item.cesg.washing_upgrade_t1.tooltip.summary",
                "A PLACED box _washes_ its contents using Create splashing recipes (gravel → flint, sand → clay, crushed ores → nuggets), chaining to the final form. _1 conversion/sec_. Only the highest-tier washing module applies.");
        add("item.cesg.washing_upgrade_t2.tooltip.summary",
                "A PLACED box _washes_ its contents, chaining to the final form, at _2 conversions/sec_. Only the highest-tier washing module applies.");
        add("item.cesg.washing_upgrade_t3.tooltip.summary",
                "A PLACED box _washes_ its contents, chaining to the final form, at _4 conversions/sec_. Only the highest-tier washing module applies.");
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
                "Click the _filter slot_ with any item or _Create filter_ to set it as configuration — the item is _not consumed_. Empty-hand click clears. Station filters still apply first.");
        add("item.cesg.compacting_upgrade.tooltip.condition1", "When installed");
        add("item.cesg.compacting_upgrade.tooltip.behaviour1",
                "_Merges partial stacks_ immediately in the GUI; automation merges up to _64 items_ per pass.");
        add("item.cesg.enhanced_shulker_box.tooltip.summary",
                "_Enhanced_ shulker storage with sidebar _upgrade modules_. _Tier 2_ has _54 slots_; craft _tier upgrades_ to reach _81_ and _108_ slots.");
        add("cesg.enhanced_shulker.tooltip.tier_slots", "Tier %s — %s storage slots, %s upgrade slot(s)");
        add("cesg.enhanced_shulker.tooltip.no_upgrades", "No upgrades installed");
        add("cesg.enhanced_shulker.tooltip.upgrades_header", "Installed upgrades:");

        add("cesg.recipe.enhanced_shulker_tier_2", "Upgrade to Enhanced Shulker Box");
        add("cesg.recipe.enhanced_shulker_tier_3", "Upgrade to Reinforced Shulker Box");
        add("cesg.recipe.enhanced_shulker_tier_4", "Upgrade to Ultimate Shulker Box");
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
        add("item.cesg.ender_pearl_dust.tooltip.summary",
                "Finely _crushed Ender Pearl_ used to begin CESG's liquid teleportation chain.");
        add("block.cesg.liquid_ender_pearl", "Liquid Ender Pearl");
        add("fluid_type.cesg.liquid_ender_pearl", "Liquid Ender Pearl");
        add("fluid.cesg.liquid_ender_pearl", "Liquid Ender Pearl");
        add("fluid.cesg.teleport_essence", "Teleport Essence");
        add("fluid.cesg.liquid_eye_of_ender", "Liquid Eye of Ender");
        add("item.cesg.liquid_ender_pearl_bucket", "Bucket of Liquid Ender Pearl");
        add("item.cesg.liquid_ender_pearl_bucket.tooltip.summary",
                "A volatile _teleporting liquid_ and precursor for _Teleport Essence_.");

        add("block.cesg.teleport_essence", "Teleport Essence");
        add("fluid_type.cesg.teleport_essence", "Teleport Essence");
        add("item.cesg.teleport_essence_bucket", "Bucket of Teleport Essence");
        add("item.cesg.teleport_essence_bucket.tooltip.summary",
                "_Same-dimension gateway fuel_ — mix _Liquid Ender Pearl_ + _Chorus Fruit_ in a heated basin.");
        add("block.cesg.liquid_eye_of_ender", "Liquid Eye of Ender");
        add("fluid_type.cesg.liquid_eye_of_ender", "Liquid Eye of Ender");
        add("item.cesg.liquid_eye_of_ender_bucket", "Bucket of Liquid Eye of Ender");
        add("item.cesg.liquid_eye_of_ender_bucket.tooltip.summary",
                "_Cross-dimensional gateway fuel_ — mix _Teleport Essence_ + _Blaze Powder_ in a heated basin.");

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
        add("cesg.goggles.loader.station.eject_funnel_hint",
                "Finished box can be pulled from any side (funnel, chute, hopper)");
        add("cesg.goggles.loader.station.ready_to_eject", "Ready to eject — extract from any side");
        add("cesg.goggles.loader.station.config_hint", "Sneak + use or use a wrench to configure");

        add("cesg.goggles.unloader.station.empty", "No shulker docked");
        add("cesg.goggles.unloader.station.dock_via_funnel", "Insert a shulker box from any funnel side to dock");
        add("cesg.goggles.unloader.station.contents", "Docked: %s — %s/%s slots");
        add("cesg.goggles.unloader.station.unpowered", "Unpowered — connect a spinning shaft to the back");
        add("cesg.goggles.unloader.station.retention", "Retention: %s");
        add("cesg.goggles.unloader.station.eject_when", "Eject when: %s");
        add("cesg.goggles.unloader.station.threshold_unload", "Eject with: %s slot(s) remaining");
        add("cesg.goggles.unloader.station.eject_funnel_hint",
                "Finished box can be pulled from any side (funnel, chute, hopper)");
        add("cesg.goggles.unloader.station.ready_to_eject", "Ready to eject — extract from any side");
        add("cesg.goggles.unloader.station.config_hint", "Sneak + use or use a wrench to configure");

        add("cesg.goggles.station.sneak_hint", "Hold [Sneak] for settings & filters");
        add("cesg.station.needs_belt",
                "Belt stations sit two blocks above a Create belt — aim at the belt to place");
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
        add("item.cesg.gateway_binding_item.tooltip.summary",
                "Links two _Cross-Dimensional Gateway Cores_ across dimensions.");
        add("item.cesg.gateway_binding_item.tooltip.condition1", "When imprinting");
        add("item.cesg.gateway_binding_item.tooltip.behaviour1",
                "_Sneak + use_ on a gateway core to store its coordinates on the crystal.");
        add("item.cesg.gateway_binding_item.tooltip.condition2", "When binding");
        add("item.cesg.gateway_binding_item.tooltip.behaviour2",
                "_Use_ an imprinted crystal on a second gateway to bind them together.");
        add("item.cesg.emergency_eye_charge", "Emergency Eye Charge");
        add("item.cesg.emergency_eye_charge.tooltip.summary",
                "_Consumable escape_ — teleports you to the _End central island_ from any dimension.");
        add("cesg.goggles.end_gateway.summary", "End-only return to the central island");
        add("cesg.goggles.gateway.essence", "Teleport Essence: %s/%s mB");
        add("cesg.goggles.gateway.eye", "Liquid Eye of Ender: %s/%s mB");
        add("cesg.goggles.gateway.same_dimension", "Bound (same dimension — uses Teleport Essence)");
        add("cesg.goggles.gateway.cross_dimension", "Bound (cross-dimensional — uses Liquid Eye of Ender)");
        add("cesg.goggles.gateway.unbound", "Unbound — imprint and apply a binding crystal");
        add("cesg.goggles.gateway.unpowered", "Unpowered — connect a spinning shaft");
        add("cesg.goggles.gateway.frame.no_ring", "Build a Gateway Frame ring around this Core");
        add("cesg.goggles.gateway.frame.gap", "Ring has a gap — only Frames and the Core count");
        add("cesg.goggles.gateway.frame.extra", "Extra Frames touching the ring — keep gateways apart");
        add("cesg.goggles.gateway.frame.blocked", "Clear the opening inside the ring");
        add("cesg.goggles.gateway.frame.size", "Opening must be 1-8 wide and 2-8 tall");
        add("cesg.goggles.gateway.frame.too_big", "Ring is too large");
        add("cesg.goggles.gateway.travel_cost", "Travel cost: %s mB per entity");

        add("cesg.gateway.end_only", "Native gateway physics only function in the End");
        add("cesg.gateway.central_island", "Routed to central island");
        add("cesg.gateway.fallback", "Partner offline — emergency route to central island");
        add("cesg.gateway.unpowered", "Gateway requires kinetic power");
        add("cesg.gateway.unbound", "Gateway is not bound to a partner");
        add("cesg.gateway.need_fuel", "Gateway requires fuel");
        add("cesg.gateway.no_frame", "No gateway frame — build a complete Gateway Frame rectangle (corners included) with the Core as one of its blocks");
        add("block.cesg.ender_infuser", "Ender Infuser");
        add("block.cesg.ender_infuser.tooltip.summary",
                "Uses _rotational force_ and catalysts to convert teleportation fluids between recipe stages.");
        add("cesg.goggles.ender_infuser.input", "Input: %s (%s/%s mB)");
        add("cesg.goggles.ender_infuser.output", "Output: %s (%s/%s mB)");
        add("cesg.goggles.ender_infuser.catalyst", "Catalyst: %sx %s");
        add("cesg.goggles.ender_infuser.producing", "Producing: %s");
        add("cesg.goggles.ender_infuser.no_recipe", "No matching recipe — check fluid + catalyst");
        add("cesg.goggles.ender_infuser.unpowered", "Requires kinetic power");
        add("cesg.goggles.ender_infuser.empty", "empty");

        add("cesg.gateway.ready", "Gateway ready — walk through the portal to travel");
        add("cesg.gateway.teleport_resistant", "Warp Resistance blocks all teleportation");
        add("subtitles.cesg.portal_open", "Gateway opens");
        add("subtitles.cesg.portal_close", "Gateway closes");
        add("subtitles.cesg.teleport", "Gateway transports");
        add("subtitles.cesg.machine_process", "Ender Infuser processes");
        add("subtitles.cesg.link_live", "Dimensional link established");
        add("subtitles.cesg.link_fault", "Dimensional link faults");
        add("subtitles.cesg.transfer", "Items cross dimensions");
        add("cesg.gateway.denied", "Travel denied");

        // Teleport potions (6F)
        add("effect.cesg.teleport", "Teleportation");
        add("effect.cesg.teleport_resistance", "Warp Resistance");
        add("cesg.tooltip.warp_resistance",
                "Resists teleportation — ender pearls, blinks, enderman grabs, and gateway travel. Walking through Nether/End portals still works.");
        add("item.minecraft.potion.effect.teleport", "Potion of Teleportation");
        add("item.minecraft.splash_potion.effect.teleport", "Splash Potion of Teleportation");
        add("item.minecraft.lingering_potion.effect.teleport", "Lingering Potion of Teleportation");
        add("item.minecraft.tipped_arrow.effect.teleport", "Arrow of Teleportation");
        add("item.minecraft.potion.effect.teleport_resistance", "Potion of Warp Resistance");
        add("item.minecraft.splash_potion.effect.teleport_resistance", "Splash Potion of Warp Resistance");
        add("item.minecraft.lingering_potion.effect.teleport_resistance", "Lingering Potion of Warp Resistance");
        add("item.minecraft.tipped_arrow.effect.teleport_resistance", "Arrow of Warp Resistance");

        // Ponder scenes (6C) — header = scene title, text_N matches showText() order in CESGPonderScenes
        add("cesg.ponder.tag.end_storage", "End Storage & Gateways");
        add("cesg.ponder.tag.end_storage.description",
                "Shulker automation, ender processing, and the cross-dimensional Gateway network");
        add("cesg.ponder.shulker_loader.header", "Shulker Loader");
        add("cesg.ponder.shulker_loader.text_1",
                "An empty shulker box drops in from above and docks in the Shulker Loader");
        add("cesg.ponder.shulker_loader.text_2",
                "Items ride in on a belt and the funnel loads them into the docked box");
        add("cesg.ponder.shulker_loader.text_3", "When full, the loaded shulker ejects onto the output belt");
        add("cesg.ponder.shulker_unloader.header", "Shulker Unloader");
        add("cesg.ponder.shulker_unloader.text_1", "A full shulker box docks in the Shulker Unloader");
        add("cesg.ponder.shulker_unloader.text_2", "Its contents are pushed out through the funnel onto a belt");
        add("cesg.ponder.shulker_unloader.text_3", "The empty shulker then drops down into storage below");
        add("cesg.ponder.ender_infuser.header", "Ender Infuser");
        add("cesg.ponder.ender_infuser.text_1", "Supply rotational force and pipe a recipe fluid into the input");
        add("cesg.ponder.ender_infuser.text_2", "Insert the required catalysts into the Infuser");
        add("cesg.ponder.ender_infuser.text_3", "Completed fluid leaves through the output connection");
        add("cesg.ponder.storage_bridge.header", "Cross-Dimensional Storage Bridge");
        add("cesg.ponder.storage_bridge.text_1",
                "Attach a Storage Bridge to a powered, bound Gateway ring");
        add("cesg.ponder.storage_bridge.text_2",
                "Its gauge reads OFFLINE until a matching Bridge on the far ring answers");
        add("cesg.ponder.storage_bridge.text_3",
                "A green LIVE gauge means the pair is up — filtered items cross the Gateway");
        add("cesg.ponder.storage_bridge.text_4",
                "If the partner Bridge is missing or unloaded the gauge turns amber and reads FAULT");
        add("cesg.ponder.storage_network_controller.header", "Storage Network Controller");
        add("cesg.ponder.storage_network_controller.text_1",
                "The Storage Network Controller anchors a cluster of adjacent storage blocks");
        add("cesg.ponder.storage_network_controller.text_2",
                "Place it touching the Bridge to expose that storage to the Gateway");
        add("cesg.ponder.storage_network_controller.text_3",
                "Right-click the Controller to read back the network's size and contents");
        add("cesg.ponder.storage_terminal.header", "Storage Terminal");
        add("cesg.ponder.storage_terminal.text_1",
                "The Storage Terminal is the console for a Controller's network");
        add("cesg.ponder.storage_terminal.text_2",
                "It must touch a cluster that contains a Storage Network Controller");
        add("cesg.ponder.storage_terminal.text_3",
                "Open it to browse local stock and, across a Bridge, the remote network's contents");
        add("cesg.ponder.gateway_flux_battery.header", "Gateway Flux Battery");
        add("cesg.ponder.gateway_flux_battery.text_1",
                "A Gateway Flux Battery stores gateway fuel beside the ring");
        add("cesg.ponder.gateway_flux_battery.text_2",
                "Pump Liquid Eye of Ender or Teleport Essence in — the window and gauge fill as it charges");
        add("cesg.ponder.gateway_flux_battery.text_3",
                "While it holds fuel, the battery keeps the Gateway Core topped up");
        add("cesg.ponder.gateway_flux_battery_array.header", "Flux Battery Arrays");
        add("cesg.ponder.gateway_flux_battery_array.text_1",
                "A lone Gateway Flux Battery is a 1x1x1 tank holding one block of fuel");
        add("cesg.ponder.gateway_flux_battery_array.text_2",
                "A 2x2 footprint stacks two high: eight blocks merged into one 2x2x2 tank");
        add("cesg.ponder.gateway_flux_battery_array.text_3",
                "A 3x3 footprint stacks three high — the largest array, holding twenty-seven blocks");
        add("cesg.ponder.gateway_flux_battery_array.text_4",
                "Any face of the array accepts fuel, and one face carries the charge gauge");
        add("cesg.ponder.shulker_belt_loader.header", "Shulker Belt Loader");
        add("cesg.ponder.shulker_belt_loader.text_1", "The Shulker Belt Loader sits two blocks above a belt");
        add("cesg.ponder.shulker_belt_loader.text_2", "Its hose extends down to draw items off the belt into a shulker");
        add("cesg.ponder.shulker_belt_loader.text_3", "The filled shulker then drops from the funnel onto an item drain");
        add("cesg.ponder.shulker_belt_unloader.header", "Shulker Belt Unloader");
        add("cesg.ponder.shulker_belt_unloader.text_1", "A full shulker box is fed into the Belt Unloader from above");
        add("cesg.ponder.shulker_belt_unloader.text_2", "Its hose extends down toward the belt below");
        add("cesg.ponder.shulker_belt_unloader.text_3", "It drops the shulker's contents onto the belt to be carried away");
        add("cesg.ponder.cross_dimensional_gateway_core.header", "Cross-Dimensional Gateway Core");
        add("cesg.ponder.cross_dimensional_gateway_core.text_1", "Build a vertical Gateway Frame ring with the Core as one of its blocks");
        add("cesg.ponder.cross_dimensional_gateway_core.text_2", "Pump Liquid Eye of Ender into any frame to fuel and light the ring");
        add("cesg.ponder.cross_dimensional_gateway_core.text_3", "The Core's glass eye fills with the fuel it is holding");
        add("cesg.ponder.cross_dimensional_gateway_core.text_4", "Powered and bound, the interior opens into a portal");
        add("cesg.gateway.invalid_partner", "Invalid partner gateway");
        add("cesg.gateway.crystal_imprinted", "Binding crystal imprinted with this gateway");
        add("cesg.gateway.crystal_empty", "Binding crystal is empty — sneak + use on a gateway to imprint");
        add("cesg.gateway.crystal_self", "Cannot bind a gateway to itself");
        add("cesg.gateway.fuel_added", "Added %s mB of gateway fuel");
        add("cesg.gateway.fuel_full", "Gateway fuel tank is full");
        add("cesg.gateway.fuel_drained", "Drained %s mB of gateway fuel");
        add("cesg.gateway.tank_empty", "Not enough fuel to fill a bucket");

        // Phase 6A: channels + destination picker.
        add("cesg.gateway.bound_success_channel", "Gateway bound on channel %s (partner channel %s)");
        add("cesg.gateway.bound_success_channel_replaced",
                "Gateway bound on channel %s (partner channel %s) — previous binding replaced");
        add("cesg.gateway.channel_screen", "Gateway Destination");
        add("cesg.gateway.channel_current", "Active channel: %s");
        add("cesg.gateway.channel_unbound", "Unbound channel");
        add("cesg.gateway.channel_bound", "Bound: %s (%s, %s, %s)");
        add("cesg.gateway.channel_bound_named", "Bound: %s — %s (%s, %s, %s)");
        add("cesg.gateway.name_hint", "Name this gateway...");
        add("cesg.goggles.gateway.destination", "Destination: %s");
        add("cesg.crystal.empty", "Not imprinted — sneak + use on a Gateway Core");
        add("cesg.crystal.target_named", "Imprinted: %s");
        add("cesg.crystal.location", "Location: %s (%s, %s, %s)");
        add("cesg.crystal.channel", "Return channel: %s");
        add("cesg.goggles.gateway.channel", "Active channel: %s");
        add("cesg.goggles.gateway.chunkloading", "Keeping this gateway and its destination loaded");
        add("cesg.gateway.chunkload_on", "Load Destination: ON");
        add("cesg.gateway.chunkload_off", "Load Destination: OFF");
        add("cesg.gateway.chunkload_tooltip",
                "While ON, this gateway keeps its own chunk and the bound destination's chunk loaded and ticking — Gateway Port transfers keep flowing with nobody on either side. Follows the active channel: switching destinations moves the loading with it.");
        add("cesg.gateway.route_on", "Route: ON");
        add("cesg.gateway.route_off", "Route: OFF");
        add("cesg.gateway.route_tooltip",
                "Route mode: a Gateway Port or Storage Bridge fans each item out to whichever bound channel's filter accepts it, instead of only the active channel. Right-click a channel to edit its filter. Fluids and manual/terminal moves still use the active channel.");
        add("cesg.gateway.filter.title", "Channel %s Filter");
        add("cesg.gateway.filter.whitelist", "Whitelist");
        add("cesg.gateway.filter.blacklist", "Blacklist");
        add("cesg.gateway.filter.whitelist.tip", "Whitelist: only these items route to this channel (empty = none).");
        add("cesg.gateway.filter.blacklist.tip", "Blacklist: every item routes to this channel except these (empty = all).");
        add("cesg.gateway.filter.hint", "Click with an item to filter this channel (the item is not consumed).");
        add("cesg.goggles.gateway.partner_live", "Partner gateway is powered and fueled");
        add("cesg.goggles.gateway.partner_offline", "Partner gateway is unpowered or out of fuel");

        // EMI recipe-viewer category title.
        add("emi.category.cesg.ender_infusing", "Ender Infusing");
        // JEI recipe-viewer category title + shared fluid-amount tooltip line.
        add("cesg.recipe.category.ender_infusing", "Ender Infusing");
        add("cesg.recipe.ender_infusing.amount", "%s mB");

        // Ender Barrel.
        add("block.cesg.ender_barrel", "Ender Barrel");
        add("block.cesg.ender_barrel.tooltip.summary",
                "Crafted as twins: both barrels of a pair share the SAME 27 slots, across any distance or dimension. Hoppers and pipes work at either end, and barrels join _Storage Networks_ (each pair counted once). Unlike an Ender Chest, each pair is its own private pool — craft more pairs for more channels.");
        add("cesg.barrel.pair", "Pair: #%s");
        add("cesg.barrel.unpaired", "Unpaired — placing one tags the rest of the stack as its twins");

        // Storage network (6D).
        add("block.cesg.storage_network_controller", "Storage Network Controller");
        add("block.cesg.storage_network_controller.tooltip.summary",
                "Anchors a storage network. _Enhanced Shulker Boxes_, _Shulker Stations_, and _Storage Terminals_ touching the controller (or each other) join the network automatically.");
        add("block.cesg.storage_terminal", "Storage Terminal");
        add("block.cesg.storage_terminal.tooltip.summary",
                "Searchable window into a storage network. Click items to withdraw; shift-click your inventory to deposit. Must touch a network that contains a _Storage Network Controller_.");
        add("cesg.network.status", "Network: %s member blocks, %s inventories, %s item types");
        add("cesg.network.no_controller", "No Storage Network Controller connected");
        add("cesg.network.terminal_title", "Storage Terminal");
        add("cesg.network.search", "Search");
        add("cesg.network.crafting", "Crafting");
        add("cesg.network.clear_craft", "Clear crafting grid (return to storage)");
        add("cesg.network.count", "%s in network");
        add("cesg.network.box_locked", "This box is part of a storage network — use a Storage Terminal, or pick the box up");
        add("cesg.network.tab.local", "Local");
        add("cesg.network.tab.partner", "Partner");
        add("cesg.network.remote.offline", "Partner network offline");
        add("cesg.network.remote.fault", "No partner network found");
        add("cesg.network.remote.empty", "Partner network is empty");
        add("cesg.network.no_results", "No results");
        add("cesg.network.remote.unfuelled", "Not enough gateway fuel — a Flux Battery is holding back the travel reserve");

        // Advancements (6C).
        add("advancement.cesg.root", "Create: End Storage & Gateways");
        add("advancement.cesg.root.desc", "Shulker logistics and cross-dimensional travel, the Create way");
        add("advancement.cesg.first_station", "Boxing Day");
        add("advancement.cesg.first_station.desc", "Craft any shulker station");
        add("advancement.cesg.enhanced_shulker", "Bigger on the Inside");
        add("advancement.cesg.enhanced_shulker.desc", "Craft an Enhanced Shulker Box");
        add("advancement.cesg.tier_upgrade", "Deep Storage");
        add("advancement.cesg.tier_upgrade.desc", "Craft a Stack Depth Upgrade");
        add("advancement.cesg.gateway_built", "Gateway Engineer");
        add("advancement.cesg.gateway_built.desc", "Craft a Cross-Dimensional Gateway Core");
        add("advancement.cesg.gateway_travel", "Interdimensional Commuter");
        add("advancement.cesg.gateway_travel.desc", "Travel to another dimension through a bound gateway");
        // Name our item tag so EMI doesn't warn about an untranslated tag.
        add("tag.item.cesg.enhanced_shulker", "Enhanced Shulker Boxes");

        decorativeNames();
    }

    /** Display names for the Phase 6E smooth/polished end stone & purpur families. */
    private void decorativeNames() {
        decorativeFamily("smooth_end_stone", "Smooth End Stone", "Smooth End Stone");
        decorativeFamily("polished_end_stone", "Polished End Stone", "Polished End Stone");
        decorativeFamily("smooth_purpur", "Smooth Purpur Block", "Smooth Purpur");
        decorativeFamily("polished_purpur", "Polished Purpur Block", "Polished Purpur");
    }

    private void decorativeFamily(String id, String baseName, String variantPrefix) {
        add("block.cesg." + id, baseName);
        add("block.cesg." + id + "_stairs", variantPrefix + " Stairs");
        add("block.cesg." + id + "_slab", variantPrefix + " Slab");
        add("block.cesg." + id + "_wall", variantPrefix + " Wall");
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
