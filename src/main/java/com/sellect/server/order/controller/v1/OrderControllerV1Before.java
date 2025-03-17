package com.sellect.server.order.controller.v1;

import com.sellect.server.auth.domain.User;
import com.sellect.server.common.infrastructure.annotation.AuthUser;
import com.sellect.server.common.response.ApiResponse;
import com.sellect.server.order.application.v1.OrderServiceV1Before;
import com.sellect.server.order.controller.request.OrderAddRequest;
import com.sellect.server.order.controller.response.OrderDetailGetResponse;
import com.sellect.server.order.controller.response.OrderGetResponse;
import com.sellect.server.order.controller.response.OrderItemGetResponse;
import com.sellect.server.order.controller.response.PendingOrderRegisterResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/v1/before")
@RestController
@RequiredArgsConstructor
public class OrderControllerV1Before {

    private final OrderServiceV1Before orderService;

    /**
     * 결제하기 카카오 페이 api (/ready 호출)
     */
    @PostMapping("/order/payment/{orderId}/ready")
    public ApiResponse<String> readyPayment(@AuthUser User user, @PathVariable Long orderId,
        @RequestParam(name = "coupon_id", required = false) Long userReceivedCouponId) {
        String redirectionUrl = orderService.payOrder(user, orderId, userReceivedCouponId);
        return ApiResponse.ok(redirectionUrl);
    }

    // ----------------------[밑에 로직들은 핵심이 아니기에 우선순위에 배제] ---------------------------
    /**
     * 주문 생성(pending)
     */
    @PostMapping("/order/pending")
    public ApiResponse<PendingOrderRegisterResponse> registerPendingOrder(@AuthUser User user,
        @Valid @RequestBody OrderAddRequest requests) {

        PendingOrderRegisterResponse response = orderService.registerPendingOrder(user, requests);
        return ApiResponse.ok(response);
    }

    /**
     * 주문 페이지 조회용 (결제 전)
     */
    @GetMapping("/orders/{orderId}/pending")
    public ApiResponse<List<OrderItemGetResponse>> readPending(
        @AuthUser User user,
        @PathVariable Long orderId
    ) {
        List<OrderItemGetResponse> result = orderService.readPending(
            user, orderId);

        return ApiResponse.ok(result);
    }

    /**
     * 주문 내역 확인
     */
    @GetMapping("/orders")
    public ApiResponse<List<OrderGetResponse>> getOrderDetail(@AuthUser User user) {
        return ApiResponse.ok(orderService.getOrdersByUser(user));
    }

    /**
     * 주문 내역 상세 확인
     */
    @GetMapping("/orders/{orderId}")
    public ApiResponse<OrderDetailGetResponse> getOrderDetail(@PathVariable Long orderId) {
        return ApiResponse.ok(orderService.getOrderDetail(orderId));
    }
}
