package com.nido.api.identity.infrastructure.persistence;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.identity.application.port.in.FindUserUseCase;
import com.nido.api.identity.domain.model.User;
import com.nido.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.shared.model.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Which account a typed identifier names, against the real database — sign-in, "forgot password" and
 * invitations all go through it. Names are unique per test: the database is shared, nothing here deletes
 * other tests' rows.
 */
@IntegrationTestConfig
class FindUserByIdentifierIT {

    @Autowired FindUserUseCase findUser;
    @Autowired UserIdentityJpaRepository users;

    @Test
    void a_deleted_account_is_never_found_by_its_username() {
        String name = unique();
        save(name, name + "@test.com", true);

        assertThat(findUser.findByIdentifier(name)).isEmpty();
    }

    @Test
    void a_username_a_deleted_account_kept_and_a_new_one_took_names_the_new_one() {
        // The unique index only covers accounts that are not deleted, so the name may be taken again.
        String name = unique();
        save(name, name + "@test.com", true);
        UUID live = save(name, name + ".new@test.com", false);

        assertThat(findUser.findByIdentifier(name)).map(User::id).contains(live);
    }

    @Test
    void a_username_is_found_whatever_its_letter_case_and_keeps_the_case_it_was_given() {
        String name = "Jane." + unique();
        save(name, name.toLowerCase() + "@test.com", false);

        assertThat(findUser.findByIdentifier(name.toUpperCase())).map(User::username).contains(name);
        assertThat(findUser.findByIdentifier(name.toLowerCase())).map(User::username).contains(name);
    }

    private UUID save(String username, String email, boolean deleted) {
        UserIdentityEntity e = new UserIdentityEntity();
        e.setUsername(username);
        e.setEmail(email);
        e.setRole(Role.USER);
        e.setDeleted(deleted);
        return users.saveAndFlush(e).getId();
    }

    private static String unique() {
        return "f" + UUID.randomUUID().toString().substring(0, 8);
    }
}
