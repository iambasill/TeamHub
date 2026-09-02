package com.basilcode.emsbackend.auth.service;

import com.basilcode.emsbackend.auth.dto.*;
import com.basilcode.emsbackend.auth.entity.RefreshToken;
import com.basilcode.emsbackend.auth.jwt.JwtService;
import com.basilcode.emsbackend.auth.mapper.AuthMapper;
import com.basilcode.emsbackend.auth.repository.RefreshTokenRepository;
import com.basilcode.emsbackend.common.exception.InvalidCredentialsException;
import com.basilcode.emsbackend.common.exception.NotFoundException;
import com.basilcode.emsbackend.common.otp.OtpService;
import com.basilcode.emsbackend.common.otp.enums.OtpPurpose;
import com.basilcode.emsbackend.mailService.MailService;
import com.basilcode.emsbackend.user.entity.User;
import com.basilcode.emsbackend.user.repository.UserRepository;
import com.basilcode.emsbackend.user.service.UserService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
@AllArgsConstructor
public class AuthService {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final AuthMapper authMapper;
    private final OtpService otpService;
    private final MailService mailService;


    public User authenticate(LoginDto loginDto) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginDto.getEmail(),
                        loginDto.getPassword()
                )
        );

        return userService.findUserByEmail(loginDto.getEmail())
                .orElseThrow(() -> new NotFoundException("User Not Found"));
    }

    @Transactional
    public void createRefreshToken(User user, String token) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(token)
                .expiryDate(Instant.now().plusMillis(jwtService.getRefreshExpirationTime()))
                .build();
        refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public AuthTokens refreshTokens(String refreshToken) {
        RefreshToken tokenOpt = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new InvalidCredentialsException("Refresh token is invalid"));

        if (tokenOpt.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(tokenOpt);
            throw new InvalidCredentialsException("Refresh token has expired. Please sign in again");
        }

        User user = tokenOpt.getUser();
        String accessToken = jwtService.generateToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        // update the refresh token
        tokenOpt.setToken(newRefreshToken);
        tokenOpt.setExpiryDate(Instant.now().plusMillis(jwtService.getRefreshExpirationTime()));
        refreshTokenRepository.save(tokenOpt);

        return AuthTokens.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.deleteByToken(refreshToken);
    }

    @Transactional
    public void resetPassword(ResetPasswordDto resetPasswordDto) {
        User user = userRepository.findByEmailId(resetPasswordDto.getEmail())
                .orElseThrow(() -> new NotFoundException("User Not Found"));
        otpService.verifyOtp(resetPasswordDto.getEmail(),OtpPurpose.EMAIL_VERIFICATION,resetPasswordDto.getToken());
        user.setPassword(passwordEncoder.encode(resetPasswordDto.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);
    }

    public AuthResponseDto getProfile(String email) {
        return userService.findUserByEmail(email)
                .map(authMapper::toAuthResponseDto)
                .orElseThrow(() -> new NotFoundException("User not found with email"));
    }

    @Transactional
    public void forgotPassword(ForgotPasswordDto forgotPasswordDto) {
        User user = userRepository.findByEmailId(forgotPasswordDto.getEmail())
                .orElseThrow(() -> new NotFoundException("User Not Found"));

        String otp = otpService.generateOtp(user.getEmailId(), OtpPurpose.EMAIL_VERIFICATION);

        String body = mailService.renderTemplate(
                "com/basilcode/emsbackend/auth/template/forgot-password-email.html",
                Map.of("otp", otp)
        );
        mailService.sendMail(user.getEmailId(), "Your password reset code", body);
    }

    @Transactional
    public void changePassword(ChangePasswordDto changePasswordDto, UserDetails userDetails) {
        User user = userRepository.findByEmailId(userDetails.getUsername())
                .orElseThrow(() -> new NotFoundException("User Not Found"));

        if (!passwordEncoder.matches(changePasswordDto.getOldPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Password is incorrect");
        }

        if (passwordEncoder.matches(changePasswordDto.getNewPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("New password must be different from the old password");
        }

        user.setPassword(passwordEncoder.encode(changePasswordDto.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);

        String body = mailService.renderTemplate(
                "com/basilcode/emsbackend/auth/template/change-password.html",
                Map.of()
        );
        mailService.sendMail(user.getEmailId(), "Your password has been changed", body);
    }

}
