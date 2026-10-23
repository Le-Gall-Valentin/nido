package com.nido.api.calendar.infrastructure.identity;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.calendar.domain.port.out.AccountLanguage;
import com.nido.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.shared.model.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestConfig
class AccountLanguageAdapterIT {

    @Autowired AccountLanguage language;
    @Autowired UserIdentityJpaRepository users;

    private final List<UUID> made = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        made.forEach(users::deleteById);
    }

    private UUID account(String language) {
        UserIdentityEntity user = new UserIdentityEntity();
        user.setUsername("speaker-" + UUID.randomUUID());
        user.setEmail(UUID.randomUUID() + "@test.com");
        user.setRole(Role.USER);
        user.setLanguage(language);
        UUID id = users.saveAndFlush(user).getId();
        made.add(id);
        return id;
    }

    @Test
    void an_export_speaks_the_language_of_its_account_and_english_when_it_has_none() {
        assertThat(language.of(account("fr"))).isEqualTo(Locale.FRENCH);
        assertThat(language.of(account(null))).as("no language recorded, outside any request").isEqualTo(Locale.ENGLISH);
        assertThat(language.of(UUID.randomUUID())).isEqualTo(Locale.ENGLISH);
    }
}
