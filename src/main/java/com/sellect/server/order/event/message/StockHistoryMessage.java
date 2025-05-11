package com.sellect.server.order.event.message;

import java.time.LocalDateTime;
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
    private String type; // "INCREMENT" or "DECREMENT"
    private List<HistoryItem> historyItems;
    private LocalDateTime createdAt;

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
