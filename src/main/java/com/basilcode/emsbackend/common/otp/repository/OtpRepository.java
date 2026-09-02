package com.basilcode.emsbackend.common.otp.repository;


import com.basilcode.emsbackend.common.otp.entity.Otp;
import com.basilcode.emsbackend.common.otp.enums.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface OtpRepository extends JpaRepository<Otp, UUID> {

    Optional<Otp> findTopByIdentifierAndPurposeAndUsedFalseOrderByCreatedAtDesc(
            String identifier, OtpPurpose purpose
    );

    void deleteByIdentifierAndPurpose(String identifier, OtpPurpose purpose);
}