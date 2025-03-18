package com.sellect.server.auth.application;

import com.sellect.server.auth.controller.request.LoginRequest;
import com.sellect.server.auth.controller.request.UserSignUpRequest;
import com.sellect.server.auth.controller.response.LoginDto;
import com.sellect.server.auth.domain.User;
import com.sellect.server.auth.domain.UserAuth;
import com.sellect.server.auth.repository.entity.Role;
import com.sellect.server.auth.repository.user.UserAuthRepository;
import com.sellect.server.auth.repository.user.UserRepository;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.common.infrastructure.jwt.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAuthService {

    private final JwtUtil jwtUtil;
    private final UserAuthRepository userAuthRepository;
    private final UserRepository userRepository;


    @Transactional(rollbackOn = RuntimeException.class)
    public void signUp(UserSignUpRequest request, Role role) {
        if (userAuthRepository.existsByEmail(request.email())) {
            throw new CommonException(BError.EXIST, "Email");
        }
        User saveUser = userRepository.save(User.register(request.nickname(), role));
        String encryptedPassword = jwtUtil.passwordEncoder().encode(request.password());
        UserAuth userAuth = UserAuth.signUp(saveUser, request.email(), encryptedPassword);
        userAuthRepository.save(userAuth);
    }

    public LoginDto login(LoginRequest loginRequest) {
        UserAuth userAuth = userAuthRepository.findByEmail(loginRequest.email())
            .orElseThrow(() -> new IllegalArgumentException("Invalid email"));

        if (!jwtUtil.passwordEncoder().matches(loginRequest.password(), userAuth.getPassword())) {
            throw new IllegalArgumentException("Invalid password");
        }

        User user = userRepository.findById(userAuth.getUser().getId())
            .orElseThrow(() -> new IllegalArgumentException("Invalid user"));

//        String accessToken = jwtUtil.generateAccessToken(user.getUuid(), user.getRole().name());
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole().name());
        String role = String.valueOf(user.getRole());
        String nickname = user.getNickname();

        return new LoginDto(accessToken, role, nickname);
    }

}
