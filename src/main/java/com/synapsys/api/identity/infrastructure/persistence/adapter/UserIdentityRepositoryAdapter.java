package com.synapsys.api.identity.infrastructure.persistence.adapter;

import com.synapsys.api.identity.domain.model.CreateUserProfileCommand;
import com.synapsys.api.identity.domain.model.UpdateProfileCommand;
import com.synapsys.api.identity.domain.model.IdentityException;
import com.synapsys.api.shared.model.Role;
import com.synapsys.api.identity.domain.model.User;
import com.synapsys.api.identity.domain.port.out.UserAdminPort;
import com.synapsys.api.identity.domain.port.out.UserCommandPort;
import com.synapsys.api.identity.domain.port.out.UserRepository;
import com.synapsys.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.synapsys.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import org.hibernate.exception.ConstraintViolationException;
import com.synapsys.api.shared.model.PageResult;
import com.synapsys.api.shared.model.SortRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Component
public class UserIdentityRepositoryAdapter implements UserRepository, UserCommandPort, UserAdminPort {

    private final UserIdentityJpaRepository jpa;

    public UserIdentityRepositoryAdapter(UserIdentityJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public boolean isEmpty() {
        return jpa.countByDeletedFalse() == 0;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpa.findByUsername(username).map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpa.findByEmail(email).map(this::toDomain);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpa.findByIdAndDeletedFalse(id).map(this::toDomain);
    }

    @Override
    public List<User> findByIds(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jpa.findByIdInAndDeletedFalse(ids).stream().map(this::toDomain).toList();
    }

    @Override
    public User createProfile(CreateUserProfileCommand command) {
        try {
            UserIdentityEntity e = new UserIdentityEntity();
            e.setUsername(command.username());
            e.setEmail(command.email());
            e.setRole(command.role());
            return toDomain(jpa.saveAndFlush(e));
        } catch (DataIntegrityViolationException ex) {
            throw resolveConstraintViolation(ex);
        }
    }

    @Override
    public PageResult<User> findAll(int page, int size, SortRequest sort, String search) {
        Sort springSort = sort.ascending()
                ? Sort.by(sort.field()).ascending()
                : Sort.by(sort.field()).descending();
        PageRequest pageRequest = PageRequest.of(page, size, springSort);
        var result = (search == null || search.isBlank())
                ? jpa.findAllNotDeleted(pageRequest)
                : jpa.searchNotDeleted(toLikePattern(search), pageRequest);
        return new PageResult<>(result.getContent().stream().map(this::toDomain).toList(),
                result.getTotalElements(), page, size);
    }

    /** Escapes LIKE wildcards with '!' (matching the ESCAPE clause in the query). */
    private static String toLikePattern(String search) {
        String escaped = search.trim().toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escaped + "%";
    }

    @Override
    public void deactivate(UUID userId) {
        jpa.deactivateById(userId);
    }

    @Override
    public void activate(UUID userId) {
        jpa.activateById(userId);
    }

    @Override
    public void deleteGdpr(UUID userId) {
        jpa.gdprAnonymize(userId);
    }

    @Override
    public void updateProfile(UpdateProfileCommand command) {
        try {
            jpa.updateProfile(command.userId(), command.username(), command.email());
        } catch (DataIntegrityViolationException ex) {
            throw resolveConstraintViolation(ex);
        }
    }

    @Override
    public void updateRole(UUID userId, Role currentRole, Role newRole) {
        int updated = jpa.updateRoleById(userId, currentRole, newRole);
        if (updated == 0) {
            throw new IdentityException.DataIntegrityError();
        }
    }

    private IdentityException resolveConstraintViolation(DataIntegrityViolationException e) {
        if (e.getCause() instanceof ConstraintViolationException cve) {
            String c = cve.getConstraintName();
            if (c != null) {
                if (c.contains("uq_users_email")) return new IdentityException.EmailAlreadyExists();
                if (c.contains("uq_users_username")) return new IdentityException.UsernameAlreadyExists();
            }
        }
        return new IdentityException.DataIntegrityError();
    }

    private User toDomain(UserIdentityEntity e) {
        return new User(e.getId(), e.getUsername(), e.getEmail(), e.getRole(), e.isActive(), e.getCreatedAt());
    }
}