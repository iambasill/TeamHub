package com.basilcode.emsbackend.auth.contoller;

import com.basilcode.emsbackend.auth.dto.*;
import com.basilcode.emsbackend.auth.jwt.JwtService;
import com.basilcode.emsbackend.auth.service.AuthService;
import com.basilcode.emsbackend.common.response.ApiResponse;
import com.basilcode.emsbackend.common.utils.CookieUtil;
import com.basilcode.emsbackend.user.entity.User;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    
    private final AuthService authService;
    private final JwtService jwtService;

    @org.springframework.beans.factory.annotation.Value("${spring.security.jwt.cookie-secure:false}")
    private boolean isCookieSecure;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDto>> login(@Valid @RequestBody LoginDto loginDto, HttpServletResponse response) {
        User authenticatedUser = authService.authenticate(loginDto);
        
        String accessToken = jwtService.generateToken(authenticatedUser);
        String refreshToken = jwtService.generateRefreshToken(authenticatedUser);
        
        authService.createRefreshToken(authenticatedUser, refreshToken);

        int accessCookieExp = (int) (jwtService.getExpirationTime() / 1000);
        int refreshCookieExp = (int) (jwtService.getRefreshExpirationTime() / 1000);

        CookieUtil.createCookie(response, "accessToken", accessToken, accessCookieExp, isCookieSecure);
        CookieUtil.createCookie(response, "refreshToken", refreshToken, refreshCookieExp, isCookieSecure);

        AuthResponseDto authResponse = authService.getProfile(loginDto.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponseDto>> getCurrentUser(@AuthenticationPrincipal UserDetails currentUser) {
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", authService.getProfile(currentUser.getUsername())));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<String>> refresh(
            @CookieValue(value = "refreshToken", required = false) String cookieRefreshToken,
            @RequestBody(required = false) AuthTokens authTokensDto,
            HttpServletResponse response) {
        
        String refreshToken = cookieRefreshToken != null ? cookieRefreshToken :
                (authTokensDto != null ? authTokensDto.getRefreshToken() : null);

        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Refresh token is required"));
        }

        AuthTokens tokens = authService.refreshTokens(refreshToken);

        int accessCookieExp = (int) (jwtService.getExpirationTime() / 1000);
        int refreshCookieExp = (int) (jwtService.getRefreshExpirationTime() / 1000);

        CookieUtil.createCookie(response, "accessToken", tokens.getAccessToken(), accessCookieExp, isCookieSecure);
        CookieUtil.createCookie(response, "refreshToken", tokens.getRefreshToken(), refreshCookieExp, isCookieSecure);

        return ResponseEntity.ok(ApiResponse.success("Tokens refreshed successfully"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {
        
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }
        
        CookieUtil.clearCookie(response, "accessToken", isCookieSecure);
        CookieUtil.clearCookie(response, "refreshToken", isCookieSecure);

        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordDto resetPasswordDto) {
        authService.resetPassword(resetPasswordDto);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordDto forgotPasswordDto) {

        authService.forgotPassword(forgotPasswordDto);

        return ResponseEntity.ok(ApiResponse.success("A password reset code has been sent to your email."));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @Valid @RequestBody ChangePasswordDto changePasswordDto,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        authService.changePassword(changePasswordDto, userDetails);

        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }
}
