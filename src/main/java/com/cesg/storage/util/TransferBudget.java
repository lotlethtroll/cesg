package com.cesg.storage.util;

/**
 * Per-tick item throughput allowance handed to the station's content handlers. The station refills it
 * each tick proportionally to its rotation speed, so insertion/extraction scales linearly with RPM
 * (and therefore with the Stress Units it draws from the network).
 */
public interface TransferBudget {
    /** Whole items that may still be moved this tick. */
    int available();

    /** Spends part of the allowance after a successful transfer. */
    void consume(int amount);

    /** No throttling - used as the default before a station binds its budget. */
    TransferBudget UNLIMITED = new TransferBudget() {
        @Override
        public int available() {
            return Integer.MAX_VALUE;
        }

        @Override
        public void consume(int amount) {
        }
    };
}
