//package com.sellect.server.order.controller.v2;
//
//import com.sellect.server.auth.domain.User;
//import com.sellect.server.common.infrastructure.annotation.AuthUser;
//import com.sellect.server.common.response.ApiResponse;
//import com.sellect.server.order.application.temp.TempOrderService;
//import com.sellect.server.order.application.v1.OrderServiceV1After;
//import com.sellect.server.order.controller.request.OrderAddRequest;
//import com.sellect.server.order.controller.response.OrderDetailGetResponse;
//import com.sellect.server.order.controller.response.OrderGetResponse;
//import com.sellect.server.order.controller.response.OrderItemGetResponse;
//import com.sellect.server.order.controller.response.PendingOrderRegisterResponse;
//import jakarta.validation.Valid;
//import java.util.List;
//import lombok.RequiredArgsConstructor;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//@RequestMapping("/api/v2")
//@RestController
//@RequiredArgsConstructor
//public class OrderControllerV2 {
//
//    private final TempOrderService orderService;
//    /**
//     * 결제하기 카카오 페이 api (/ready 호출)
//     */
//    @PostMapping("/order/payment/{orderId}/ready")
//    public ApiResponse<String> readyPayment(@AuthUser User user, @PathVariable Long orderId,
//        @RequestParam(name = "coupon_id", required = false) Long userReceivedCouponId) {
//        orderService.preparePayment(user, orderId, userReceivedCouponId);
//        return ApiResponse.ok();
//    }
//
//    @GetMapping("/order/payment/{orderId}/ready")
//    public ApiResponse<String> getPaymentUrl(@AuthUser User user, @PathVariable Long orderId) {
//        orderService.getPaymentUrl(user, orderId);
//        return ApiResponse.ok();
//    }
//}
