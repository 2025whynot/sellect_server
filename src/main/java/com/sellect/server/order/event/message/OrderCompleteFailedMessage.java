package com.sellect.server.order.event.message;

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
public class OrderCompleteFailedMessage {

    private Long orderId;
    private String pid;
    private boolean orderCompleted;
}
