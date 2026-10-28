package com.cesg.gateways;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AutomationFuelBudgetTest {
    @Test void blockedMovesRefundAndDoNotConsumeTheFlushCharge() {
        int[] fuel = {300};
        AutomationFuelBudget budget = new AutomationFuelBudget(cross -> {
            if (fuel[0] < 100) return false;
            fuel[0] -= 100;
            return true;
        }, cross -> fuel[0] += 100);
        assertEquals(0, budget.transfer(false, () -> 0));
        assertEquals(300, fuel[0]);
        assertEquals(4, budget.transfer(false, () -> 4));
        assertEquals(200, fuel[0]);
        assertEquals(20, budget.transfer(false, () -> 20));
        assertEquals(200, fuel[0]);
    }

    @Test void eachFuelPaysOnceAndAnUnfundedRouteCannotMove() {
        int[] fuel = {100, 0};
        AutomationFuelBudget budget = new AutomationFuelBudget(cross -> {
            int type = cross ? 1 : 0;
            if (fuel[type] < 100) return false;
            fuel[type] -= 100;
            return true;
        }, cross -> fuel[cross ? 1 : 0] += 100);
        assertEquals(1, budget.transfer(false, () -> 1));
        assertEquals(0, budget.transfer(true, () -> fail("Unfunded route moved cargo")));
        fuel[1] = 100;
        assertEquals(1, budget.transfer(true, () -> 1));
        assertEquals(1, budget.transfer(true, () -> 1));
        assertArrayEquals(new int[]{0, 0}, fuel);
    }
}
