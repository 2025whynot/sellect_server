package com.sellect.server.auth.controller.application;

import com.sellect.server.auth.controller.request.LoginRequest;
import com.sellect.server.auth.controller.request.UserSignUpRequest;
import com.sellect.server.auth.domain.Seller;
import com.sellect.server.auth.domain.SellerAuth;
import com.sellect.server.auth.repository.seller.SellerAuthRepository;
import com.sellect.server.auth.repository.seller.SellerRepository;
import com.sellect.server.common.infrastructure.jwt.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SellerAuthService {
    private final JwtUtil jwtUtil;
    private final SellerRepository sellerRepository;
    private final SellerAuthRepository sellerAuthRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(rollbackOn = RuntimeException.class)
    public void signUp(UserSignUpRequest request) {
        Seller savedSeller = sellerRepository.save(Seller.register(request.nickname()));
        String encryptPassword = passwordEncoder.encode(request.password());
        SellerAuth sellerAuth = SellerAuth.signUp(savedSeller, request.email(), encryptPassword);
        sellerAuthRepository.save(sellerAuth);
    }

    public String login(LoginRequest request) {
        SellerAuth sellerAuth = sellerAuthRepository.findByEmail(request.email());
        if (!passwordEncoder.matches(request.password(), sellerAuth.getPassword())) {
            // todo
            throw new RuntimeException("패스워드 안맞음");
        }
        Seller seller = sellerRepository.findById(sellerAuth.getSeller().getId());

        return jwtUtil.generateAccessToken(seller.getUuid(), "SELLER");
    }
}
