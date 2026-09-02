package com.basilcode.emsbackend.board.repository;

import com.basilcode.emsbackend.board.entity.BoardMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BoardMemberRepository extends JpaRepository<BoardMember, UUID> {
    boolean existsByUser_Id(UUID userId);
    Optional<BoardMember> findByUser_Id(UUID userId);
}
