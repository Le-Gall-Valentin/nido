package com.nido.api.identity.infrastructure.web;

import com.jayway.jsonpath.JsonPath;
import com.nido.api.MailIntegrationTestConfig;
import com.nido.api.SharedGreenMail;
import com.nido.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.infrastructure.ratelimit.RedisRateLimitBucketStore;
import com.nido.api.mfa.infrastructure.config.TotpEncryptorFactory;
import com.nido.api.mfa.infrastructure.persistence.entity.TwoFactorMethodEntity;
import com.nido.api.mfa.infrastructure.persistence.repository.TwoFactorMethodJpaRepository;
import com.nido.api.shared.model.TwoFactorMethod;
import com.nido.api.shared.model.Role;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.UUID;

import static com.nido.api.ReceivedMails.allTo;
import static com.nido.api.ReceivedMails.subjectOf;
import static com.nido.api.ReceivedMails.textOf;
import static com.nido.api.ReceivedMails.to;
import static com.nido.api.TestAccessTokens.cookieFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Mail on: what an administrator's gesture sends — to the account's holder, and to every active
 * super-administrator. The database is shared: other super-administrators may be told too, so mails are
 * read by recipient, and the queue is left empty for the next test.
 */
@MailIntegrationTestConfig
class AdminGesturesMailIT {

    @Autowired WebApplicationContext webApplicationContext;
    @Autowired UserIdentityJpaRepository users;
    @Autowired RedisRateLimitBucketStore rateLimitBucketStore;
    @Autowired JdbcClient jdbc;
    @Autowired TwoFactorMethodJpaRepository totps;
    @Autowired TotpEncryptorFactory encryptorFactory;

    private MockMvc mockMvc;
    private String suffix;
    private UUID aliceId;
    private UUID bobId;
    private UUID carolId;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
        rateLimitBucketStore.clearAll();
        SharedGreenMail.server().purgeEmailFromAllMailboxes();
        suffix = UUID.randomUUID().toString().substring(0, 8);
        aliceId = save("alice", Role.SUPER_ADMIN);
        bobId = save("bob", Role.ADMIN);
        carolId = save("carol", Role.USER);
    }

    @AfterEach
    void leaveNoMailForTheNextTest() throws InterruptedException {
        for (int attempt = 0; attempt < 100 && queued() > 0; attempt++) {
            Thread.sleep(100);
        }
        jdbc.sql("DELETE FROM mail_outbox").update();
        // Out of the super-administrators the next tests would tell.
        jdbc.sql("UPDATE users SET is_active = false WHERE id = :id").param("id", aliceId).update();
    }

    private int queued() {
        return jdbc.sql("SELECT count(*) FROM mail_outbox").query(Integer.class).single();
    }

    private UUID save(String who, Role role) {
        UserIdentityEntity user = new UserIdentityEntity();
        user.setUsername(name(who));
        user.setEmail(address(who));
        user.setRole(role);
        user.setLanguage("fr");
        return users.saveAndFlush(user).getId();
    }

    private String name(String who) {
        return who + "-" + suffix;
    }

    private String address(String who) {
        return name(who) + "@test.local";
    }

    @Test
    void an_admin_deactivating_an_account_tells_its_holder_and_the_super_administrators() throws Exception {
        mockMvc.perform(post("/api/users/" + carolId + "/deactivate").cookie(cookieFor(bobId, Role.ADMIN)))
            .andExpect(status().isNoContent());

        MimeMessage toCarol = to(address("carol"));
        assertThat(subjectOf(toCarol)).isEqualTo("Votre compte Nido est temporairement désactivé");
        assertThat(textOf(toCarol)).contains(name("bob") + " a désactivé votre compte Nido.");
        assertThat(subjectOf(to(address("alice"))))
            .isEqualTo(name("bob") + " a désactivé le compte de " + name("carol"));
    }

    @Test
    void reactivating_an_account_welcomes_its_holder_back() throws Exception {
        jdbc.sql("UPDATE users SET is_active = false WHERE id = :id").param("id", carolId).update();

        mockMvc.perform(post("/api/users/" + carolId + "/activate").cookie(cookieFor(bobId, Role.ADMIN)))
            .andExpect(status().isNoContent());

        assertThat(subjectOf(to(address("carol")))).isEqualTo("Votre compte Nido est de nouveau actif");
        assertThat(subjectOf(to(address("alice"))))
            .isEqualTo(name("bob") + " a réactivé le compte de " + name("carol"));
    }

    @Test
    void deleting_an_invited_account_cancels_its_invitation_by_mail() throws Exception {
        String body = mockMvc.perform(post("/api/users").cookie(cookieFor(bobId, Role.ADMIN))
                .header(HttpHeaders.ACCEPT_LANGUAGE, "fr")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + name("dave") + "\",\"email\":\"" + address("dave") + "\",\"role\":\"USER\"}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String daveId = JsonPath.read(body, "$.id");
        assertThat(subjectOf(to(address("alice")))).isEqualTo(name("bob") + " a invité " + name("dave"));
        // Received before the deletion, which would otherwise withdraw it from the queue.
        assertThat(subjectOf(to(address("dave")))).isEqualTo(name("bob") + " vous invite à rejoindre Nido");

        mockMvc.perform(delete("/api/users/" + daveId).cookie(cookieFor(bobId, Role.ADMIN))
                .header(HttpHeaders.ACCEPT_LANGUAGE, "fr"))
            .andExpect(status().isNoContent());

        List<MimeMessage> toDave = allTo(address("dave"), 2);
        assertThat(subjectOf(toDave.get(1))).isEqualTo("Votre invitation à Nido a été annulée");
        assertThat(allTo(address("alice"), 2)).extracting(mail -> subjectOf(mail))
            .contains(name("bob") + " a annulé l’invitation de " + name("dave"));
    }

    @Test
    void an_admin_resetting_a_second_factor_tells_its_holder_and_the_super_administrators() throws Exception {
        totps.save(new TwoFactorMethodEntity(carolId, TwoFactorMethod.APP, encryptorFactory.forUser(carolId).encrypt("JBSWY3DPEHPK3PXP")));

        mockMvc.perform(post("/api/users/" + carolId + "/2fa/reset").cookie(cookieFor(bobId, Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content("{\"methods\":[\"APP\"]}"))
            .andExpect(status().isNoContent());

        MimeMessage toCarol = to(address("carol"));
        assertThat(subjectOf(toCarol)).isEqualTo("La double authentification de votre compte Nido a été réinitialisée");
        assertThat(textOf(toCarol)).contains(name("bob") + " a retiré l’application d’authentification de votre compte.",
            "Nido ne vous demande plus de code à la connexion.");
        assertThat(subjectOf(to(address("alice"))))
            .isEqualTo(name("bob") + " a réinitialisé la 2FA de " + name("carol"));
    }

    @Test
    void a_role_change_tells_its_holder_it_applies_at_once() throws Exception {
        mockMvc.perform(patch("/api/users/" + carolId).cookie(cookieFor(aliceId, Role.SUPER_ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"ADMIN\"}"))
            .andExpect(status().isNoContent());

        MimeMessage toCarol = to(address("carol"));
        assertThat(subjectOf(toCarol)).isEqualTo("Vous êtes maintenant administrateur de Nido");
        assertThat(textOf(toCarol)).contains(name("alice") + " vous a nommé administrateur.",
            "Le changement s’applique tout de suite.");
    }
}
