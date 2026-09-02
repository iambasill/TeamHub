package com.basilcode.emsbackend.common.otp;

import com.basilcode.emsbackend.common.exception.OtpException;
import com.basilcode.emsbackend.common.otp.entity.Otp;
import com.basilcode.emsbackend.common.otp.enums.OtpPurpose;
import com.basilcode.emsbackend.common.otp.repository.OtpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpRepository otpRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();

    private static final int OTP_LENGTH_BOUND = 900_000; // 6-digit range
    private static final int OTP_MIN = 100_000;
    private static final Duration OTP_TTL = Duration.ofMinutes(10);
    private static final int MAX_ATTEMPTS = 5;

    public String generateOtp(String identifier, OtpPurpose purpose) {

        // implement limit the code or res use send the previous
        otpRepository.deleteByIdentifierAndPurpose(identifier, purpose);

        String rawCode = String.valueOf(OTP_MIN + secureRandom.nextInt(OTP_LENGTH_BOUND));

        Otp otp = Otp.builder()
                .identifier(identifier)
                .hashedCode(passwordEncoder.encode(rawCode))
                .purpose(purpose)
                .expiresAt(Instant.now().plus(OTP_TTL))
                .createdAt(Instant.now())
                .attempts(0)
                .used(false)
                .build();

        otpRepository.save(otp);

        return rawCode;
    }

    public void verifyOtp(String identifier, OtpPurpose purpose, String submittedCode) {
        Otp otp = otpRepository
                .findTopByIdentifierAndPurposeAndUsedFalseOrderByCreatedAtDesc(identifier, purpose)
                .orElseThrow(() -> new OtpException.Invalid("No active code found. Please request a new one."));

        if (otp.isUsed()) {
            throw new OtpException.Invalid("This code has already been used.");
        }

        if (Instant.now().isAfter(otp.getExpiresAt())) {
            throw new OtpException.Expired("This code has expired. Please request a new one.");
        }

        if (otp.getAttempts() >= MAX_ATTEMPTS) {
            throw new OtpException.Locked("Too many failed attempts. Please request a new code.");
        }

        boolean matches = passwordEncoder.matches(submittedCode, otp.getHashedCode());

        if (!matches) {
            otp.setAttempts(otp.getAttempts() + 1);
            otpRepository.save(otp);
            throw new OtpException.Invalid("Incorrect code.");
        }

        otp.setUsed(true);
        otpRepository.save(otp);
    }
}