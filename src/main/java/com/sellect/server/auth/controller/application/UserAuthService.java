package com.sellect.server.auth.controller.application;

import com.sellect.server.auth.controller.request.LoginRequest;
import com.sellect.server.auth.controller.request.UserSignUpRequest;
import com.sellect.server.auth.domain.User;
import com.sellect.server.auth.domain.UserAuth;
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
    public void signUp(UserSignUpRequest request) {
        if (userAuthRepository.existsByEmail(request.email())) {
            // todo: 커스텀 exception
            throw new IllegalArgumentException("Email already exists");
        }
        User saveUser = userRepository.save(User.register(request.nickname()));
        String encryptedPassword = passwordEncoder.encode(request.password());
        UserAuth userAuth = UserAuth.signUp(saveUser, request.email(), encryptedPassword);
        userAuthRepository.save(userAuth);
    }

    public String login(LoginRequest loginRequest) {
        UserAuth userAuth = userAuthRepository.findByEmail(loginRequest.email());
        if (!passwordEncoder.matches(loginRequest.password(), userAuth.getPassword())) {
            throw new RuntimeException("패스워드 안맞음");
        }
        User user = userRepository.findById(userAuth.getUser().getId());

        return jwtUtil.generateAccessToken(user.getUuid(), "USER");
    }

}
