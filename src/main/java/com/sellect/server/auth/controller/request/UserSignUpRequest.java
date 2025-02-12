package com.sellect.server.auth.controller.request;

public record UserSignUpRequest(
        String email,
        String password,
        String nickname
)
{
}
