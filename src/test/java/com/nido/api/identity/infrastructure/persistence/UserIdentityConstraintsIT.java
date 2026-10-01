package com.nido.api.identity.infrastructure.persistence;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.shared.model.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * What the database itself refuses since migration 060. The application already writes addresses in
 * lower case and validates usernames; these are the guarantees for every path that forgets to.
 * Names are unique per test: the database is shared, nothing here deletes other tests' rows.
 */
@IntegrationTestConfig
class UserIdentityConstraintsIT {

    @Autowired UserIdentityJpaRepository users;

    @Test
    void an_address_in_capitals_is_refused() {
        assertThatThrownBy(() -> users.saveAndFlush(user(unique(), "Someone." + unique() + "@Test.com")))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasStackTraceContaining("ck_users_email_lowercase");
    }

    @Test
    void an_account_that_is_not_deleted_needs_an_address() {
        assertThatThrownBy(() -> users.saveAndFlush(user(unique(), null)))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasStackTraceContaining("ck_users_email_required");
    }

    @Test
    void an_account_that_is_not_deleted_needs_a_username() {
        assertThatThrownBy(() -> users.saveAndFlush(user(null, unique() + "@test.com")))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasStackTraceContaining("ck_users_username_required");
    }

    @Test
    void a_username_cannot_hold_an_at_sign() {
        assertThatThrownBy(() -> users.saveAndFlush(user("jane@" + unique(), unique() + "@test.com")))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasStackTraceContaining("ck_users_username_no_at");
    }

    @Test
    void anonymizing_an_account_still_clears_its_username_and_address() {
        UserIdentityEntity saved = users.saveAndFlush(user(unique(), unique() + "@test.com"));

        users.gdprAnonymize(saved.getId());

        UserIdentityEntity gone = users.findById(saved.getId()).orElseThrow();
        assertThat(gone.getUsername()).isNull();
        assertThat(gone.getEmail()).isNull();
        assertThat(gone.isDeleted()).isTrue();
    }

    private static String unique() {
        return "c" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static UserIdentityEntity user(String username, String email) {
        UserIdentityEntity e = new UserIdentityEntity();
        e.setUsername(username);
        e.setEmail(email);
        e.setRole(Role.USER);
        return e;
    }
}
