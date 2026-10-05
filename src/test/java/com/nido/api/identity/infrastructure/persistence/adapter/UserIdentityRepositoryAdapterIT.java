package com.nido.api.identity.infrastructure.persistence.adapter;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.identity.domain.model.CreateUserProfileCommand;
import com.nido.api.identity.domain.model.User;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.shared.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestConfig
class UserIdentityRepositoryAdapterIT {

    @Autowired UserIdentityRepositoryAdapter repositoryAdapter;
    @Autowired UserIdentityJpaRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void findByIds_returnsOnlyNonDeletedMatchingUsers() {
        UUID a = repositoryAdapter.createProfile(
            new CreateUserProfileCommand("alice", "alice@example.com", Role.USER)).id();
        UUID b = repositoryAdapter.createProfile(
            new CreateUserProfileCommand("bob", "bob@example.com", Role.USER)).id();
        UUID absent = UUID.randomUUID();

        List<User> found = repositoryAdapter.findByIds(List.of(a, b, absent));

        assertThat(found).extracting(User::username)
            .containsExactlyInAnyOrder("alice", "bob");
    }

    @Test
    void findByIds_excludesSoftDeletedUsers() {
        UUID alive = repositoryAdapter.createProfile(
            new CreateUserProfileCommand("carol", "carol@example.com", Role.USER)).id();
        UUID deleted = repositoryAdapter.createProfile(
            new CreateUserProfileCommand("dave", "dave@example.com", Role.USER)).id();
        repositoryAdapter.deleteGdpr(deleted);

        List<User> found = repositoryAdapter.findByIds(List.of(alive, deleted));

        assertThat(found).extracting(User::username).containsExactly("carol");
    }

    @Test
    void findActiveByRole_keepsOnlyActiveAccountsOfThatRoleThatAreNotDeleted() {
        UUID kept = repositoryAdapter.createProfile(
            new CreateUserProfileCommand("root", "root@example.com", Role.SUPER_ADMIN)).id();
        UUID off = repositoryAdapter.createProfile(
            new CreateUserProfileCommand("dormant", "dormant@example.com", Role.SUPER_ADMIN)).id();
        UUID gone = repositoryAdapter.createProfile(
            new CreateUserProfileCommand("former", "former@example.com", Role.SUPER_ADMIN)).id();
        repositoryAdapter.createProfile(new CreateUserProfileCommand("admin", "admin@example.com", Role.ADMIN));
        repositoryAdapter.deactivate(off);
        repositoryAdapter.deleteGdpr(gone);

        assertThat(repositoryAdapter.findActiveByRole(Role.SUPER_ADMIN)).extracting(User::id).containsExactly(kept);
    }
}
