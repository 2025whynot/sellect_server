package com.sellect.server.order.event.message;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockHistoryMessage {

    private Long userId;
    private String type; // "IN" or "OUT"
    private List<HistoryItem> historyItems;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryItem {
        private Long productId;
        private Integer quantity;
    }

}
