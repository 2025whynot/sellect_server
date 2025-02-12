package com.sellect.server.common.infrastructure.jwt;

import com.sellect.server.auth.domain.Seller;
import com.sellect.server.auth.domain.User;
import com.sellect.server.auth.repository.seller.SellerRepository;
import com.sellect.server.auth.repository.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@Component
@RequiredArgsConstructor
@Slf4j
public class  JwtFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final SellerRepository sellerRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);
        if (token != null && jwtUtil.isTokenValid(token)) {
            String uuid = jwtUtil.extractUuid(token);
            String role = jwtUtil.extractRole(token);

            Object userDetails = null;
            List<GrantedAuthority> authorities = new ArrayList<>();
            if ("USER".equals(role)) {
                User byUuid = userRepository.findByUuid(uuid);
                if (byUuid != null) {
                    userDetails = byUuid;
                    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                }
            } else if ("SELLER".equals(role)) {
                Seller byUuid = sellerRepository.findByUuid(uuid);
                if (byUuid != null) {
                    userDetails = byUuid;
                    authorities.add(new SimpleGrantedAuthority("ROLE_SELLER"));
                }
            }

            if (userDetails != null) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return null;
    }
}

