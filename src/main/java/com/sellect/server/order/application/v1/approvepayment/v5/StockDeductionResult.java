package com.sellect.server.order.application.v1.approvepayment.v5;

import java.util.Map;
import lombok.Getter;

@Getter
public class StockDeductionResult {

    private final boolean success;
    private final Map<String, Integer> deductedStocks;

    public StockDeductionResult(boolean success, Map<String, Integer> deductedStocks) {
        this.success = success;
        this.deductedStocks = deductedStocks;
    }
}