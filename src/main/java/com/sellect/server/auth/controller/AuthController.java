package com.sellect.server.auth.controller;

import com.sellect.server.auth.controller.application.UserAuthService;
import com.sellect.server.auth.controller.request.LoginRequest;
import com.sellect.server.auth.controller.request.UserSignUpRequest;
import com.sellect.server.auth.repository.entity.Role;
import com.sellect.server.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;


@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private final UserAuthService userAuthService;

    @PostMapping("/signup")
    public ApiResponse<Void> signup(@RequestBody @Valid UserSignUpRequest request) {
        userAuthService.signUp(request, Role.USER);
        return ApiResponse.ok(null);
    }

    @PostMapping("/seller/signup")
    public ApiResponse<Void> sellerSignUp(@RequestBody UserSignUpRequest request) {
        userAuthService.signUp(request, Role.SELLER);
        return ApiResponse.ok(null);
    }

    @PostMapping("/login")
    public ApiResponse<Void> login(@RequestBody @Valid LoginRequest request,
        HttpServletResponse response) {
        String accessToken = userAuthService.login(request, Role.USER);
        ResponseCookie cookie = ResponseCookie.from("access_token", accessToken).httpOnly(true)
            .secure(false).path("/").maxAge(Duration.ofMinutes(60)).build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ApiResponse.ok(null);
    }

    @PostMapping("/seller/login")
    public ApiResponse<Void> sellerLogin(@RequestBody @Valid LoginRequest request,
        HttpServletResponse response) {
        String accessToken = userAuthService.login(request, Role.SELLER);

        ResponseCookie cookie = ResponseCookie.from("access_token", accessToken).httpOnly(true)
            .secure(false).path("/").maxAge(Duration.ofMinutes(60)).build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ApiResponse.ok(null);
    }


}
