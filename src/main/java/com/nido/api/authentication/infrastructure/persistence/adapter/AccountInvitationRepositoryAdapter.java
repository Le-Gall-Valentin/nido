package com.nido.api.authentication.infrastructure.persistence.adapter;

import com.nido.api.authentication.domain.model.AccountInvitation;
import com.nido.api.authentication.domain.port.out.AccountInvitationRepository;
import com.nido.api.authentication.infrastructure.persistence.entity.AccountInvitationEntity;
import com.nido.api.authentication.infrastructure.persistence.repository.AccountInvitationJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class AccountInvitationRepositoryAdapter implements AccountInvitationRepository {

    private final AccountInvitationJpaRepository jpa;

    public AccountInvitationRepositoryAdapter(AccountInvitationJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(UUID userId, String tokenHash, Instant createdAt, Instant expiresAt) {
        // The account is the key: saving merges onto the previous row, which takes its token with it.
        jpa.saveAndFlush(new AccountInvitationEntity(userId, tokenHash, createdAt, expiresAt));
    }

    @Override
    public Optional<AccountInvitation> findByHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(AccountInvitationRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional
    public Optional<AccountInvitation> consumeByHash(String tokenHash) {
        Optional<AccountInvitationEntity> found = jpa.findByTokenHashForUpdate(tokenHash);
        // Flushed at once, like the reset tokens: a clearing bulk update later in the caller's transaction
        // would otherwise discard the pending delete and leave the link usable again.
        found.ifPresent(entity -> {
            jpa.delete(entity);
            jpa.flush();
        });
        return found.map(AccountInvitationRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<AccountInvitation> findByUserId(UUID userId) {
        return jpa.findById(userId).map(AccountInvitationRepositoryAdapter::toDomain);
    }

    @Override
    public Map<UUID, AccountInvitation> findByUserIds(Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return jpa.findByUserIdIn(userIds).stream()
            .collect(Collectors.toUnmodifiableMap(AccountInvitationEntity::getUserId,
                AccountInvitationRepositoryAdapter::toDomain));
    }

    @Override
    public void deleteForUser(UUID userId) {
        jpa.deleteById(userId);
    }

    private static AccountInvitation toDomain(AccountInvitationEntity entity) {
        return new AccountInvitation(entity.getUserId(), entity.getCreatedAt(), entity.getExpiresAt());
    }
}
