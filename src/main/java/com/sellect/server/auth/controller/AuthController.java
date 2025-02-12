package com.sellect.server.auth.controller;

import com.sellect.server.auth.controller.application.UserAuthService;
import com.sellect.server.auth.controller.request.UserSignUpRequest;
import com.sellect.server.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/auth")
public class AuthController {
    private final UserAuthService userAuthService;

    @PostMapping("/signup")
    public ApiResponse<Void> signup(@RequestBody UserSignUpRequest request) {
        userAuthService.signUp(request);
        return ApiResponse.ok(null);
    }


}
