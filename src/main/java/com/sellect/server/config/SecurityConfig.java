package com.sellect.server.config;

import com.sellect.server.common.infrastructure.jwt.JwtFilter;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;


@Slf4j
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${cloudfront.domain-name}")
    private String CLOUDFRONT_DOMAIN_NAME;

    private static final String[] LOAD_TEST_PATHS = {
        "/api/v1/coupon/register/**",
        "/api/v0/test/**",
        "/api/v1/test/**",
    };


    private static final String[] SWAGGER_PATHS = {
        "/swagger-resources/**", "/swagger-ui/**", "/v3/api-docs/**", "/actuator/**"
    };

    // 인증 x JWT x
    private static final String[] NO_JWT_PATHS = {
        "/api/v1/auth/signup",
        "/api/v1/auth/login",
        "/api/v1/auth/seller/signup",
        "/api/v1/batch/performance-test", // 배치 테스트
    };

    // 인증 x JWT o
    private static final String[] PUBLIC_PATHS = {
        "/api/v1/payment/success/**",
        "/api/v1/payment/fail",
        "/api/v1/payment/cancel",
        "/api/v1/coupon/actives",
        "/api/v1/search/**",
        "/api/v1/product/**",
        "/api/v1/products/**",
        "/api/v1/images/**",
        "/api/v1/categories/**",
        "/api/v1/brands/**",
        "/api/v1/kakao-pay/**",
        "/api/v1/seller/**",

        // 리팩터링 전 버전
        "/api/v0/**"
    };
    private final JwtFilter jwtFilter;

    private HttpSecurity commonConfig(HttpSecurity http) throws Exception {
        return http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http = commonConfig(http);

        http.authorizeHttpRequests(auth -> auth
            .requestMatchers(LOAD_TEST_PATHS).permitAll() // 인증 없이 허용
            .requestMatchers(NO_JWT_PATHS).permitAll() // 인증 없이 허용
            .requestMatchers(PUBLIC_PATHS).permitAll() // JWT는 필요하지만 인증 없이 접근 가능
            .requestMatchers(SWAGGER_PATHS).permitAll() // Swagger 문서 접근 가능
            .anyRequest().authenticated()
        );

        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOrigin("http://localhost:3000");
        configuration.addAllowedOrigin("https://sellect-client.vercel.app");
        configuration.addAllowedOrigin(CLOUDFRONT_DOMAIN_NAME);
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(true);

        // 🔥 Set-Cookie 헤더를 클라이언트에서 읽을 수 있도록 설정
        configuration.setExposedHeaders(Arrays.asList("Set-Cookie"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}