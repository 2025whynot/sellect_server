package com.sellect.server.auth.controller.application;

import com.sellect.server.auth.controller.request.LoginRequest;
import com.sellect.server.auth.controller.request.UserSignUpRequest;
import com.sellect.server.auth.domain.User;
import com.sellect.server.auth.domain.UserAuth;
import com.sellect.server.auth.repository.entity.Role;
import com.sellect.server.auth.repository.user.UserAuthRepository;
import com.sellect.server.auth.repository.user.UserRepository;
import com.sellect.server.common.infrastructure.jwt.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAuthService {

    private final JwtUtil jwtUtil;
    private final UserAuthRepository userAuthRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(rollbackOn = RuntimeException.class)
    public void signUp(UserSignUpRequest request, Role role) {
        if (userAuthRepository.existsByEmail(request.email())) {
            // todo: 커스텀 exception
            throw new IllegalArgumentException("Email already exists");
        }
        User saveUser = userRepository.save(User.register(request.nickname(), role));
        String encryptedPassword = passwordEncoder.encode(request.password());
        UserAuth userAuth = UserAuth.signUp(saveUser, request.email(), encryptedPassword);
        userAuthRepository.save(userAuth);
    }

    public String login(LoginRequest loginRequest, Role expectedRole) {
        UserAuth userAuth = userAuthRepository.findByEmail(loginRequest.email())
            .orElseThrow(() -> new IllegalArgumentException("Invalid email"));

        if (!passwordEncoder.matches(loginRequest.password(), userAuth.getPassword())) {
            throw new IllegalArgumentException("Invalid password");
        }

        User user = userRepository.findById(userAuth.getUser().getId())
            .orElseThrow(() -> new IllegalArgumentException("Invalid user"));

        // 역할이 맞지 않으면 로그인 실패 처리
        if (user.getRole() != expectedRole) {
            throw new IllegalArgumentException("Unauthorized role");
        }

        return jwtUtil.generateAccessToken(user.getUuid(), user.getRole().name());
    }

}
