package com.sellect.server.auth.controller.application;

import com.sellect.server.auth.controller.request.UserSignUpRequest;
import com.sellect.server.auth.domain.User;
import com.sellect.server.auth.domain.UserAuth;
import com.sellect.server.auth.repository.user.UserAuthRepository;
import com.sellect.server.auth.repository.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserAuthService {
    private final UserAuthRepository userAuthRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(rollbackOn = RuntimeException.class)
    public void signUp(UserSignUpRequest request) {
        User saveUser = userRepository.save(User.register(request.nickname()));
        String encryptedPassword = passwordEncoder.encode(request.password());
        UserAuth userAuth = UserAuth.signUp(saveUser, request.email(), encryptedPassword);
        userAuthRepository.save(userAuth);
    }
}
