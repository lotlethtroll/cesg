package com.cesg;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-synced balance knobs (Phase 6C). Defaults match the previously hard-coded values, so an
 * untouched config changes nothing.
 */
public final class CESGConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.IntValue GATEWAY_TRAVEL_COST;
    private static final ModConfigSpec.IntValue GATEWAY_TELEPORT_COOLDOWN;
    private static final ModConfigSpec.BooleanValue GATEWAY_CHUNK_LOADING;
    private static final ModConfigSpec.IntValue SHULKER_CAGE_COOLDOWN;
    private static final ModConfigSpec.IntValue SHULKER_CAGE_FIRE_INTERVAL;
    private static final ModConfigSpec.IntValue GATEWAY_PORT_TRANSFER_COST;
    private static final ModConfigSpec.IntValue BATTERY_CAPACITY;
    private static final ModConfigSpec.IntValue BATTERY_MAX_DRAIN;
    private static final ModConfigSpec.IntValue BATTERY_RESERVE_FLOOR;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("gateway");
        GATEWAY_TRAVEL_COST = builder
                .comment("Fuel drained per gateway teleport, in mB of Teleport Essence / Liquid Eye of Ender.")
                .defineInRange("travelCostMb", 250, 0, 4000);
        GATEWAY_TELEPORT_COOLDOWN = builder
                .comment("Portal cooldown applied to entities after gateway travel, in ticks.")
                .defineInRange("teleportCooldownTicks", 60, 0, 1200);
        GATEWAY_CHUNK_LOADING = builder
                .comment("Allow the per-gateway \"keep destination loaded\" toggle (chunk loading).",
                        "Disable to remove the option entirely on this server.")
                .define("allowChunkLoading", true);
        GATEWAY_PORT_TRANSFER_COST = builder
                .comment("Fuel (mB) a Gateway Port spends per flush (every 10 ticks) when it moves items or",
                        "fluid. Default 0 = Ports are free (1.0 behaviour). Set >0 to make automated",
                        "transfer cost fuel; a Gateway Flux Battery on the ring then protects a travel",
                        "reserve (see battery.reserveFloorMb).")
                .defineInRange("portTransferCostMb", 0, 0, 4000);
        builder.pop();

        builder.push("shulkerCage");
        SHULKER_CAGE_COOLDOWN = builder
                .comment("Ticks between shell harvests from a caged shulker.")
                .defineInRange("harvestCooldownTicks", 100, 1, 24000);
        SHULKER_CAGE_FIRE_INTERVAL = builder
                .comment("Ticks between the caged shulker's bullet-fire attempts.")
                .defineInRange("fireAttemptIntervalTicks", 20, 1, 24000);
        builder.pop();

        builder.push("battery");
        BATTERY_CAPACITY = builder
                .comment("Fuel capacity per Gateway Flux Battery block, in mB. Batteries assemble into a",
                        "W×W×H array (up to 3×3×3) that holds W*W*H times this. Single-fuel: an array",
                        "locks to the first fuel piped in (Teleport Essence OR Liquid Eye of Ender).")
                .defineInRange("capacityMbPerBlock", 8000, 500, 1_000_000);
        BATTERY_MAX_DRAIN = builder
                .comment("Max fuel (mB) the battery array pushes into the connected gateway Core per tick.",
                        "Bounds how fast a battery can cover a burst.")
                .defineInRange("maxDrainMbPerTick", 500, 1, 100_000);
        BATTERY_RESERVE_FLOOR = builder
                .comment("Fuel (mB) a Gateway Flux Battery array keeps in reserve for player travel. While a",
                        "battery is on the ring, automated Port/Bridge transfers may only draw combined fuel",
                        "(Core + battery) down to this floor; player travel ignores it. Only has an effect",
                        "when gateway.portTransferCostMb > 0.")
                .defineInRange("reserveFloorMb", 1000, 0, 1_000_000);
        builder.pop();

        SPEC = builder.build();
    }

    private CESGConfig() {}

    public static int gatewayTravelCost() {
        return GATEWAY_TRAVEL_COST.get();
    }

    public static int gatewayTeleportCooldown() {
        return GATEWAY_TELEPORT_COOLDOWN.get();
    }

    public static boolean gatewayChunkLoadingAllowed() {
        return GATEWAY_CHUNK_LOADING.get();
    }

    public static int gatewayPortTransferCost() {
        return GATEWAY_PORT_TRANSFER_COST.get();
    }

    public static int shulkerCageCooldown() {
        return SHULKER_CAGE_COOLDOWN.get();
    }

    public static int shulkerCageFireInterval() {
        return SHULKER_CAGE_FIRE_INTERVAL.get();
    }

    public static int batteryCapacity() {
        return BATTERY_CAPACITY.get();
    }

    public static int batteryMaxDrainPerTick() {
        return BATTERY_MAX_DRAIN.get();
    }

    public static int batteryReserveFloor() {
        return BATTERY_RESERVE_FLOOR.get();
    }
}
