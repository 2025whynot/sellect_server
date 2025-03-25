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
public class OrderCompleteRollbackMessage {

    private Long orderId;
    private Long pid;
    private boolean orderCompleted;
}
