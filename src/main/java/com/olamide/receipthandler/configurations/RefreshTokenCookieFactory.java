package com.olamide.receipthandler.configurations;

import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCookieFactory {

    @Value("${cookie.secure:true}")
    private boolean secure;

    private static final String COOKIE_NAME = "refreshToken";
    private static final int MAX_AGE_SECONDS = 7 * 24 * 60 * 60;

    public Cookie create(String refreshToken) {
        Cookie cookie = new Cookie(COOKIE_NAME, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        cookie.setPath("/api/auth");
        cookie.setAttribute("SameSite", "Strict");
        cookie.setMaxAge(MAX_AGE_SECONDS);
        return cookie;
    }

    public Cookie clear() {
        Cookie cookie = new Cookie(COOKIE_NAME, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(0);
        return cookie;
    }
}