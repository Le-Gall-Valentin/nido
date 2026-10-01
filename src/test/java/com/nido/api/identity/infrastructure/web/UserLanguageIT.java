package com.nido.api.identity.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nido.api.IntegrationTestConfig;
import com.nido.api.authentication.infrastructure.persistence.entity.UserCredentialEntity;
import com.nido.api.authentication.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import com.nido.api.authentication.infrastructure.persistence.repository.UserCredentialJpaRepository;
import com.nido.api.authentication.infrastructure.web.dto.LoginRequest;
import com.nido.api.identity.application.port.in.FindUserUseCase;
import com.nido.api.identity.domain.model.User;
import com.nido.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.infrastructure.ratelimit.RedisRateLimitBucketStore;
import com.nido.api.mfa.infrastructure.persistence.repository.UserTotpJpaRepository;
import com.nido.api.shared.model.Role;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTestConfig
class UserLanguageIT {

    @Autowired WebApplicationContext webApplicationContext;
    @Autowired UserIdentityJpaRepository users;
    @Autowired UserCredentialJpaRepository credentials;
    @Autowired RefreshTokenJpaRepository refreshTokens;
    @Autowired UserTotpJpaRepository totps;
    @Autowired RedisRateLimitBucketStore rateLimitBucketStore;
    @Autowired FindUserUseCase findUser;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(SecurityMockMvcConfigurers.springSecurity()).build();
        rateLimitBucketStore.clearAll();
        refreshTokens.deleteAll();
        totps.deleteAll();
        credentials.deleteAll();
        users.deleteAll();
        createUser("jane", "jane@test.com", "password", null);
    }

    private UserIdentityEntity createUser(String username, String email, String password, String language) {
        UserIdentityEntity user = new UserIdentityEntity();
        user.setUsername(username);
        user.setEmail(email);
        user.setRole(Role.USER);
        user.setLanguage(language);
        users.saveAndFlush(user);
        UserCredentialEntity credential = new UserCredentialEntity();
        credential.setUserId(user.getId());
        credential.setPasswordHash(encoder.encode(password));
        credentials.save(credential);
        return user;
    }

    private Cookie loginAs(String username, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(username, password))))
            .andReturn().getResponse().getCookie("access_token");
    }

    @Test
    void an_account_starts_without_a_language_then_keeps_the_one_it_is_given() throws Exception {
        Cookie access = loginAs("jane", "password");

        mockMvc.perform(get("/api/users/me").cookie(access))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.language").isEmpty());

        mockMvc.perform(put("/api/users/me/language").cookie(access)
                .contentType(MediaType.APPLICATION_JSON).content("{\"language\":\"fr\"}"))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/me").cookie(access))
            .andExpect(jsonPath("$.language").value("fr"));
    }

    @Test
    void only_the_languages_the_app_speaks_are_accepted() throws Exception {
        Cookie access = loginAs("jane", "password");

        mockMvc.perform(put("/api/users/me/language").cookie(access)
                .contentType(MediaType.APPLICATION_JSON).content("{\"language\":\"de\"}"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/users/me/language").cookie(access)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void changing_the_language_needs_a_session() throws Exception {
        mockMvc.perform(put("/api/users/me/language")
                .contentType(MediaType.APPLICATION_JSON).content("{\"language\":\"fr\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void the_login_response_carries_the_language_so_the_app_can_apply_it_at_once() throws Exception {
        createUser("john", "john@test.com", "password", "en");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("john", "password"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.language").value("en"));
    }

    @Test
    void an_address_is_found_whatever_its_letter_case() {
        createUser("mixed", "mixed.case@test.com", "password", null);

        assertThat(findUser.findByIdentifier("MIXED.case@test.COM")).map(User::username).contains("mixed");
    }

    @Test
    void a_deleted_account_is_never_found_even_when_it_kept_its_address() {
        UserIdentityEntity gone = createUser("gone", "gone@test.com", "password", null);
        gone.setDeleted(true);
        users.saveAndFlush(gone);

        assertThat(findUser.findByIdentifier("gone@test.com")).isEmpty();
    }

    @Test
    void a_deactivated_account_is_still_found() {
        UserIdentityEntity asleep = createUser("asleep", "asleep@test.com", "password", null);
        asleep.setActive(false);
        users.saveAndFlush(asleep);

        assertThat(findUser.findByIdentifier("asleep@test.com")).map(User::username).contains("asleep");
    }

    @Test
    void a_username_is_found_whatever_its_letter_case() {
        assertThat(findUser.findByIdentifier("jane")).map(User::username).contains("jane");
        assertThat(findUser.findByIdentifier("Jane")).map(User::username).contains("jane");
    }
}
