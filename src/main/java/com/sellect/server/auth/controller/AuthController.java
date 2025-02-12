package com.sellect.server.auth.controller;

import com.sellect.server.auth.controller.application.SellerAuthService;
import com.sellect.server.auth.controller.application.UserAuthService;
import com.sellect.server.auth.controller.request.LoginRequest;
import com.sellect.server.auth.controller.request.UserSignUpRequest;
import com.sellect.server.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;


@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/auth")
public class AuthController {
    private final UserAuthService userAuthService;
    private final SellerAuthService sellerAuthService;

    @PostMapping("/signup")
    public ApiResponse<Void> signup(@RequestBody UserSignUpRequest request) {
        userAuthService.signUp(request);
        return ApiResponse.ok(null);
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        String accessToken = userAuthService.login(request);
        ResponseCookie cookie = ResponseCookie.from("access_token", accessToken)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(Duration.ofMinutes(60))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(null);
    }


    @PostMapping("/seller/signup")
    public ApiResponse<Void> sellerSignUp(@RequestBody UserSignUpRequest request) {
        sellerAuthService.signUp(request);
        return ApiResponse.ok(null);
    }


}
