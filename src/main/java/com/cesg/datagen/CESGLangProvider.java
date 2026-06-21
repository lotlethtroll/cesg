package com.cesg.datagen;

import com.cesg.CESG;

import net.minecraft.data.PackOutput;
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
        add("block.cesg.shulker_duplication_aid", "Shulker Duplication Aid");
        add("block.cesg.end_gateway", "Fabricated End Gateway");
        add("block.cesg.cross_dimensional_gateway_core", "Cross-Dimensional Gateway Core");

        add("item.cesg.enhanced_shulker_t2", "Enhanced Shulker (Tier 2)");
        add("item.cesg.stack_depth_upgrade", "Stack Depth Upgrade");
        add("item.cesg.filter_upgrade", "Filter Upgrade");
        add("item.cesg.compacting_upgrade", "Compacting Upgrade");
        add("item.cesg.shulker_shell", "Processed Shulker Shell");
        add("item.cesg.gateway_binding_item", "Gateway Binding Crystal");
        add("item.cesg.emergency_eye_charge", "Emergency Eye Charge");
        add("item.cesg.teleport_essence_bucket", "Bucket of Teleport Essence");
        add("item.cesg.liquid_eye_of_ender_bucket", "Bucket of Liquid Eye of Ender");

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

        add("cesg.goggles.belt_loader.placement", "Place two blocks above a horizontal belt");
        add("cesg.goggles.belt_loader.filter_all", "Filter: accepts all items");
        add("cesg.goggles.belt_loader.filter_set", "Filter: %s");
        add("cesg.goggles.belt_loader.filter_hint", "Click the front filter slot with an item or Create filter");

        add("cesg.goggles.belt_unloader.placement", "Place two blocks above a horizontal belt");
        add("cesg.goggles.belt_unloader.filter_all", "Filter: unloads all items");
        add("cesg.goggles.belt_unloader.filter_set", "Filter: %s");
        add("cesg.goggles.belt_unloader.filter_hint", "Click the front filter slot with an item or Create filter");

        add("cesg.goggles.station.names", "Stamps name: %s");

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
}
