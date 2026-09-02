package com.basilcode.emsbackend.common.utils;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;

public class CookieUtil {

    /**
     * Creates an HttpOnly cookie with proper SameSite handling for cross-origin setups.
     * <p>
     * When secure=true (production / HTTPS): SameSite=None is required so the browser
     * sends the cookie across origins (e.g. Railway backend ↔ localhost frontend).
     * When secure=false (local dev / HTTP): SameSite=Lax is used — browsers reject
     * SameSite=None on non-Secure cookies, so Lax is the correct fallback.
     */
    public static void createCookie(HttpServletResponse response, String name, String value, int maxAge, boolean secure) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(maxAge)
                .sameSite(secure ? "None" : "Lax")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    /**
     * Clears a cookie by setting maxAge=0.
     * SameSite and Secure attributes must match the original cookie so the browser
     * can correctly identify and expire it.
     */
    public static void clearCookie(HttpServletResponse response, String name, boolean secure) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(0)
                .sameSite(secure ? "None" : "Lax")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}
