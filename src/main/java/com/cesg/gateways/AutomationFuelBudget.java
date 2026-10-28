package com.cesg.gateways;

import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

/** One flush pays once for each fuel type that actually transports cargo. Server-thread only. */
final class AutomationFuelBudget {
    private final Predicate<Boolean> reserve;
    private final Consumer<Boolean> refund;
    private boolean essencePaid;
    private boolean eyePaid;

    AutomationFuelBudget(Predicate<Boolean> reserve, Consumer<Boolean> refund) {
        this.reserve = reserve;
        this.refund = refund;
    }

    int transfer(boolean crossDimension, IntSupplier move) {
        boolean paid = crossDimension ? eyePaid : essencePaid;
        if (!paid && !reserve.test(crossDimension))
            return 0;
        int moved = move.getAsInt();
        if (!paid) {
            if (moved <= 0)
                refund.accept(crossDimension);
            else if (crossDimension)
                eyePaid = true;
            else
                essencePaid = true;
        }
        return moved;
    }
}
