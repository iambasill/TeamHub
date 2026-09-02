package com.basilcode.emsbackend.common.otp.entity;

import com.basilcode.emsbackend.common.otp.enums.OtpPurpose;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "otps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Otp {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String identifier;

    @Column(nullable = false)
    private String hashedCode;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OtpPurpose purpose;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(nullable = false)
    private boolean used = false;

    @Column(nullable = false)
    private Instant createdAt;
}