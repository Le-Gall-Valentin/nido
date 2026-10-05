package com.nido.api.space.infrastructure.notification;

import com.nido.api.MailIntegrationTestConfig;
import com.nido.api.SharedGreenMail;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.infrastructure.ratelimit.RedisRateLimitBucketStore;
import com.nido.api.shared.model.Role;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.infrastructure.persistence.repository.SpaceJpaRepository;
import com.nido.api.space.infrastructure.persistence.repository.SpaceMemberJpaRepository;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.nido.api.TestAccessTokens.cookieFor;
import static com.nido.api.space.infrastructure.notification.SpaceNotificationITSupport.textOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Mail on, the whole chain: an invitation, the notification, the invitee's choices, the outbox, an SMTP
 * server. Accounts and the space are this test's own, and are removed after each test.
 */
@MailIntegrationTestConfig
class SpaceInvitationNotificationIT {

    @Autowired WebApplicationContext webApplicationContext;
    @Autowired UserIdentityJpaRepository users;
    @Autowired SpaceJpaRepository spaces;
    @Autowired SpaceMemberJpaRepository members;
    @Autowired RedisRateLimitBucketStore rateLimitBucketStore;
    @Autowired JdbcClient jdbc;

    private MockMvc mockMvc;
    private SpaceNotificationITSupport scene;
    private String suffix;
    private String aliceName;
    private String carolName;
    private UUID aliceId;
    private UUID carolId;
    private UUID spaceId;
    /** A super administrator, made only by the test that erases an account. */
    private UUID rootId;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
        scene = new SpaceNotificationITSupport(users, spaces, members);
        rateLimitBucketStore.clearAll();
        jdbc.sql("DELETE FROM mail_outbox").update();
        SharedGreenMail.server().purgeEmailFromAllMailboxes();
        suffix = UUID.randomUUID().toString().substring(0, 8);
        aliceName = "alice-" + suffix;
        carolName = "carol-" + suffix;
        aliceId = scene.saveUser(aliceName, "fr");
        carolId = scene.saveUser(carolName, "fr");
        spaceId = scene.saveSharedSpace("Chez nous", aliceId);
        scene.saveMembership(spaceId, aliceId, SpaceRole.OWNER);
    }

    @AfterEach
    void removeWhatThisTestCreated() {
        jdbc.sql("DELETE FROM space_invitations WHERE space_id = :id").param("id", spaceId).update();
        jdbc.sql("DELETE FROM space_members WHERE space_id = :id").param("id", spaceId).update();
        jdbc.sql("DELETE FROM spaces WHERE id = :id").param("id", spaceId).update();
        List<UUID> accounts = rootId == null ? List.of(aliceId, carolId) : List.of(aliceId, carolId, rootId);
        jdbc.sql("DELETE FROM users WHERE id IN (:ids)").param("ids", accounts).update();
    }

    @Test
    void an_invitation_reaches_the_invitee_in_their_language() throws Exception {
        invite(carolName, null).andExpect(status().isCreated());

        MimeMessage mail = theOnlyMail();
        assertThat(((InternetAddress) mail.getRecipients(MimeMessage.RecipientType.TO)[0]).getAddress())
            .isEqualTo(carolName + "@test.local");
        assertThat(mail.getSubject()).isEqualTo(aliceName + " vous invite dans l’espace Chez nous");
    }

    @Test
    void an_invitee_without_a_language_is_written_to_in_the_inviter_s() throws Exception {
        // French on purpose: without the inviter's language the mail would fall back to English.
        jdbc.sql("UPDATE users SET language = NULL WHERE id = :id").param("id", carolId).update();

        invite(carolName, "fr").andExpect(status().isCreated());

        assertThat(theOnlyMail().getSubject()).isEqualTo(aliceName + " vous invite dans l’espace Chez nous");
    }

    @Test
    void an_invitee_s_own_language_wins_over_the_inviter_s() throws Exception {
        jdbc.sql("UPDATE users SET language = 'en' WHERE id = :id").param("id", carolId).update();

        invite(carolName, "fr").andExpect(status().isCreated());

        assertThat(theOnlyMail().getSubject()).isEqualTo(aliceName + " invited you to the space Chez nous");
    }

    @Test
    void switching_the_kind_off_stops_the_mail() throws Exception {
        choose(carolId, "types/space.invitation", false);

        invite(carolName, null).andExpect(status().isCreated());

        assertThat(SharedGreenMail.server().waitForIncomingEmail(1_500, 1)).isFalse();
    }

    @Test
    void switching_mail_off_stops_it_and_keeps_each_kind_as_chosen() throws Exception {
        choose(carolId, "types/space.member-joined", false);
        choose(carolId, "channels/email", false);

        invite(carolName, null).andExpect(status().isCreated());
        assertThat(SharedGreenMail.server().waitForIncomingEmail(1_500, 1)).isFalse();

        choose(carolId, "channels/email", true);
        mockMvc.perform(get("/api/notifications/preferences").cookie(cookieFor(carolId)))
            .andExpect(jsonPath("$.channels[0].enabled").value(true))
            .andExpect(jsonPath("$.types[?(@.type == 'space.invitation')].enabled").value(true))
            .andExpect(jsonPath("$.types[?(@.type == 'space.member-joined')].enabled").value(false));
    }

    @Test
    void a_refused_invitation_sends_nothing() throws Exception {
        scene.saveMembership(spaceId, carolId, SpaceRole.MEMBER);

        invite(carolName, null).andExpect(status().isConflict());

        assertThat(SharedGreenMail.server().waitForIncomingEmail(1_500, 1)).isFalse();
    }

    @Test
    void revoking_an_invitation_tells_the_invitee() throws Exception {
        UUID invitationId = carolInvited();

        mockMvc.perform(delete("/api/spaces/" + spaceId + "/invitations/" + invitationId).cookie(cookieFor(aliceId)))
            .andExpect(status().isNoContent());

        MimeMessage mail = theOnlyMail();
        assertThat(mail.getSubject()).isEqualTo("Votre invitation dans Chez nous est annulée");
        assertThat(textOf(mail)).contains(aliceName + " a annulé votre invitation à rejoindre l’espace « Chez nous ».");
    }

    @Test
    void deleting_the_space_tells_its_invitee() throws Exception {
        carolInvited();

        mockMvc.perform(delete("/api/spaces/" + spaceId).cookie(cookieFor(aliceId)))
            .andExpect(status().isNoContent());

        assertThat(textOf(theOnlyMail()))
            .contains(aliceName + " a supprimé l’espace « Chez nous » : votre invitation n’est plus valable.");
    }

    @Test
    void erasing_an_owner_without_heir_tells_the_invitee_who_left() throws Exception {
        carolInvited();
        rootId = scene.saveUser("root-" + suffix, null, Role.SUPER_ADMIN);

        mockMvc.perform(delete("/api/users/" + aliceId).cookie(cookieFor(rootId, Role.SUPER_ADMIN)))
            .andExpect(status().isNoContent());

        // alice is told too, her account being deleted: the invitee's is one of two mails.
        assertThat(textOf(theMailTo(carolName + "@test.local", 2))).contains(aliceName
            + " a quitté Nido et l’espace « Chez nous » a été supprimé : votre invitation n’est plus valable.");
    }

    /** Invites carol, waits for her invitation mail, and forgets it: what follows is the only mail left. */
    private UUID carolInvited() throws Exception {
        invite(carolName, null).andExpect(status().isCreated());
        theOnlyMail();
        SharedGreenMail.server().purgeEmailFromAllMailboxes();
        return jdbc.sql("SELECT id FROM space_invitations WHERE space_id = :space AND invitee_id = :invitee")
            .param("space", spaceId).param("invitee", carolId).query(UUID.class).single();
    }

    private void choose(UUID userId, String target, boolean enabled) throws Exception {
        mockMvc.perform(put("/api/notifications/preferences/" + target).cookie(cookieFor(userId))
                .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":" + enabled + "}"))
            .andExpect(status().isNoContent());
    }

    private ResultActions invite(String identifier, String inviterLanguage) throws Exception {
        var request = post("/api/spaces/" + spaceId + "/invitations")
            .cookie(cookieFor(aliceId))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"identifier\":\"" + identifier + "\",\"role\":\"MEMBER\"}");
        if (inviterLanguage != null) {
            request.header("Accept-Language", inviterLanguage);
        }
        return mockMvc.perform(request);
    }

    /** Waits for {@code count} mails and returns the one sent to {@code address} — the others went elsewhere. */
    private MimeMessage theMailTo(String address, int count) {
        assertThat(SharedGreenMail.server().waitForIncomingEmail(10_000, count)).isTrue();
        return Arrays.stream(SharedGreenMail.server().getReceivedMessages())
            .filter(message -> SpaceNotificationITSupport.recipientOf(message).equals(address))
            .findFirst().orElseThrow();
    }

    private MimeMessage theOnlyMail() {
        assertThat(SharedGreenMail.server().waitForIncomingEmail(10_000, 1)).isTrue();
        MimeMessage[] received = SharedGreenMail.server().getReceivedMessages();
        assertThat(received).hasSize(1);
        return received[0];
    }
}
