package com.basilcode.emsbackend.policy.repository;

import com.basilcode.emsbackend.policy.entity.PolicyDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PolicyDocumentRepository extends JpaRepository<PolicyDocument, UUID> {
    List<PolicyDocument> findAllByOrderByCategoryAscTitleAsc();
    List<PolicyDocument> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrderByCategoryAscTitleAsc(
            String titleQuery, String descriptionQuery);
}
