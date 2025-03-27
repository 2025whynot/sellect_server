package com.sellect.server.order.application.v1.approvepayment.v5;

import java.util.Map;
import lombok.Getter;

public class StockDeductionResult {

    @Getter
    private final boolean success;
    private final Map<String, Integer> deductedStocks;
    private final Runnable rollbackAction;

    public StockDeductionResult(boolean success, Map<String, Integer> deductedStocks, Runnable rollbackAction) {
        this.success = success;
        this.deductedStocks = deductedStocks;
        this.rollbackAction = rollbackAction;
    }

    public void rollbackIfNeeded() {
        if (!success && !deductedStocks.isEmpty()) {
            rollbackAction.run();
        }
    }

    public void forceRollback() {
        rollbackAction.run();
    }
}
