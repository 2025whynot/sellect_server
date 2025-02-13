package com.sellect.server.auth.controller.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserSignUpRequest(
    @Email(message = "유효한 이메일 형식이 아닙니다.")
    @NotBlank(message = "이메일은 필수 입력 사항입니다.")
    String email,
    @NotBlank(message = "비밀번호는 필수입니다.")
    String password,
    @NotBlank(message = "닉네임은 필수 입력사항입니다.")
    String nickname
) {

}
