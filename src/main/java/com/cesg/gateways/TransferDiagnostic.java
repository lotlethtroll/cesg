package com.cesg.gateways;

/** Stable menu wire IDs; status describes a direction, not overall partner liveness. */
public enum TransferDiagnostic {
    READY, DISABLED, IDLE, NETWORK_UNAVAILABLE, SCAN_LIMIT, GATEWAY_INACTIVE,
    NO_ROUTE, PARTNER_OFFLINE, PARTNER_LOCKED, PROTECTED_RESERVE, INSUFFICIENT_FUEL, DESTINATION_FULL;

    public String translationKey() { return "cesg.transfer." + name().toLowerCase(java.util.Locale.ROOT); }
    public static TransferDiagnostic fromId(int id) {
        return id >= 0 && id < values().length ? values()[id] : IDLE;
    }
}
