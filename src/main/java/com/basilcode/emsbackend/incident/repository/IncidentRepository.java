package com.basilcode.emsbackend.incident.repository;

import com.basilcode.emsbackend.incident.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface IncidentRepository extends JpaRepository<Incident, UUID>,
        JpaSpecificationExecutor<Incident> {
}
