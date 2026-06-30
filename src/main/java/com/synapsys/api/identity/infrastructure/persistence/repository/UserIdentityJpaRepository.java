package com.synapsys.api.identity.infrastructure.persistence.repository;

import com.synapsys.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.synapsys.api.shared.model.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserIdentityJpaRepository extends JpaRepository<UserIdentityEntity, UUID> {
    Optional<UserIdentityEntity> findByUsername(String username);
    Optional<UserIdentityEntity> findByEmail(String email);

    @Query("SELECT u FROM UserIdentityEntity u WHERE u.id = :id AND u.deleted = false")
    Optional<UserIdentityEntity> findByIdAndDeletedFalse(@Param("id") UUID id);

    List<UserIdentityEntity> findByIdInAndDeletedFalse(Collection<UUID> ids);

    @Query("SELECT u FROM UserIdentityEntity u WHERE u.deleted = false")
    Page<UserIdentityEntity> findAllNotDeleted(Pageable pageable);

    // The pattern is pre-escaped by the adapter ('!' escapes '!', '%' and '_').
    @Query("""
        SELECT u FROM UserIdentityEntity u
        WHERE u.deleted = false
          AND (LOWER(u.username) LIKE :pattern ESCAPE '!' OR LOWER(u.email) LIKE :pattern ESCAPE '!')
        """)
    Page<UserIdentityEntity> searchNotDeleted(@Param("pattern") String pattern, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE UserIdentityEntity u SET u.active = false WHERE u.id = :id")
    void deactivateById(@Param("id") UUID id);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE UserIdentityEntity u SET u.active = true WHERE u.id = :id")
    void activateById(@Param("id") UUID id);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE UserIdentityEntity u SET u.username = :username, u.email = :email WHERE u.id = :id")
    void updateProfile(@Param("id") UUID id,
                       @Param("username") String username,
                       @Param("email") String email);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE UserIdentityEntity u SET u.role = :newRole WHERE u.id = :id AND u.role = :currentRole AND u.deleted = false")
    int updateRoleById(@Param("id") UUID id, @Param("currentRole") Role currentRole, @Param("newRole") Role newRole);

    long countByDeletedFalse();

    // ON DELETE CASCADE does not fire on this UPDATE, so credential and TOTP cleanup
    // is handled explicitly by DeleteUserHandler after this call.
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE UserIdentityEntity u SET u.username = null, u.email = null, u.deleted = true, u.active = false WHERE u.id = :id")
    void gdprAnonymize(@Param("id") UUID id);
}