package com.synapsys.api.authentication.infrastructure.persistence.repository;

import com.synapsys.api.authentication.infrastructure.persistence.entity.UserCredentialEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface UserCredentialJpaRepository extends JpaRepository<UserCredentialEntity, UUID> {

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE UserCredentialEntity c SET c.passwordHash = :hash WHERE c.userId = :id")
    void updatePasswordHash(@Param("id") UUID id, @Param("hash") String hash);
}