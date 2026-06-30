package com.synapsys.api.authentication.infrastructure.persistence.adapter;

import com.synapsys.api.authentication.domain.port.out.UserCredentialPort;
import com.synapsys.api.authentication.infrastructure.persistence.entity.UserCredentialEntity;
import com.synapsys.api.authentication.infrastructure.persistence.repository.UserCredentialJpaRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UserCredentialAdapter implements UserCredentialPort {

    private final UserCredentialJpaRepository jpa;

    public UserCredentialAdapter(UserCredentialJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void saveCredential(UUID userId, String passwordHash) {
        UserCredentialEntity e = new UserCredentialEntity();
        e.setUserId(userId);
        e.setPasswordHash(passwordHash);
        jpa.save(e);
    }

    @Override
    public void updatePasswordHash(UUID userId, String newHash) {
        jpa.updatePasswordHash(userId, newHash);
    }

    @Override
    public void deleteCredential(UUID userId) {
        jpa.deleteById(userId);
    }
}
