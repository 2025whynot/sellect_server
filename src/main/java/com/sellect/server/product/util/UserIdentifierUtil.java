package com.sellect.server.product.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;

public class UserIdentifierUtil {

    private static final String COOKIE_NAME = "SEARCH_SSID";
    private static final int COOKIE_EXPIRATION = 60 * 60 * 24 * 7; // 7일 유지 (일주일)

    public static String getUserIdentifier(Long userId, HttpServletRequest request,
        HttpServletResponse response) {
        if (userId != null) {
            return "USER_" + userId; // 회원은 userId 기반
        }

        // 비회원 (쿠키를 사용해서 UUID 확인)
        Cookie existingCookie = getCookie(request, COOKIE_NAME);
        if (existingCookie != null) {
            return "GUEST_" + existingCookie.getValue();
        }

        // 기존 쿠키가 없다면 새로운 UUID 생성 ㅎ 쿠키로 저장
        String newSessionId = UUID.randomUUID().toString();
        addCookie(response, COOKIE_NAME, newSessionId);

        return "GUEST_" + newSessionId;
    }

    private static Cookie getCookie(HttpServletRequest request, String name) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookie.getName().equals(name)) {
                    return cookie;
                }
            }
        }
        return null;
    }

    private static void addCookie(HttpServletResponse response, String name,
        String value) {
        Cookie cookie = new Cookie(name, value); // 쿠키 만료 기간 설정
        cookie.setPath("/"); // 모든 경로에서 사용 가능 (검색은 어디든 가능)
        cookie.setHttpOnly(true); // JavaScript 에서 접근 불가
//        cookie.setSecure(true); // HTTPS 환경에서만 전송

        response.addCookie(cookie);
    }

}
