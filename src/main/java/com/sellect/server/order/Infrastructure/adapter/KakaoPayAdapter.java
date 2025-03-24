package com.sellect.server.order.Infrastructure.adapter;

import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.common.response.ApiResponse;
import com.sellect.server.order.application.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/kakao-pay")
public class KakaoPayAdapter {

    private final OrderService orderService;

    public KakaoPayAdapter(OrderService orderService) {
        this.orderService = orderService;
    }


    @GetMapping("/success/{pid}")
    public String approvePayment(
        @PathVariable String pid,
        @RequestParam("pg_token") String token) {

        Long longPid = convertToLong(pid);
        orderService.approvePayment(longPid, token);
        return """
            <!DOCTYPE html>
            <html lang="ko">
            <head>
                <meta charset="UTF-8">
                <title>결제 성공</title>
                <link rel="icon" href="data:,"> <!-- favicon 요청 방지 -->
                <style>
                    body { font-family: Arial, sans-serif; text-align: center; padding: 50px; }
                    h1 { color: #4CAF50; }
                    p { font-size: 18px; }
                    button { 
                        padding: 10px 20px; 
                        font-size: 16px; 
                        color: white; 
                        background-color: #007BFF; 
                        border: none; 
                        border-radius: 5px; 
                        cursor: pointer; 
                    }
                    button:hover { background-color: #0056b3; }
                </style>
            </head>
            <body>
                <h1>결제 성공!</h1>
                <p>결제가 정상적으로 완료되었습니다.</p>
                <button onclick="window.close()">확인</button>
            </body>
            </html>
            """;
    }

    @GetMapping("/cancel/{pid}")
    public ApiResponse<Object> cancelPayment(
        @PathVariable String pid
    ) {
//        paymentService.cancelPayment(pid);
        return ApiResponse.ok();
    }

    @GetMapping("/fail/{pid}")
    public ApiResponse<Object> failPayment(
        @PathVariable String pid
    ) {
//        paymentService.failPayment(pid);
        return ApiResponse.ok();
    }

    private Long convertToLong(String pid) {
        try {
            return Long.parseLong(pid);
        } catch (NumberFormatException e) {
            log.error("Invalid pid format: {}", pid);
            throw new CommonException(BError.NOT_VALID, "Invalid pid format: " + pid);
        }
    }
}
