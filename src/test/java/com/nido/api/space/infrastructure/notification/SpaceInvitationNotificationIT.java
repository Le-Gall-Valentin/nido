package com.nido.api.space.infrastructure.notification;

import com.nido.api.MailIntegrationTestConfig;
import com.nido.api.SharedGreenMail;
import com.nido.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.infrastructure.ratelimit.RedisRateLimitBucketStore;
import com.nido.api.shared.model.Role;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.space.infrastructure.persistence.entity.SpaceEntity;
import com.nido.api.space.infrastructure.persistence.entity.SpaceMemberEntity;
import com.nido.api.space.infrastructure.persistence.repository.SpaceJpaRepository;
import com.nido.api.space.infrastructure.persistence.repository.SpaceMemberJpaRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.Cookie;
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

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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

    private static final String JWT_SECRET = "integration-test-secret-at-least-32-chars!";

    @Autowired WebApplicationContext webApplicationContext;
    @Autowired UserIdentityJpaRepository users;
    @Autowired SpaceJpaRepository spaces;
    @Autowired SpaceMemberJpaRepository members;
    @Autowired RedisRateLimitBucketStore rateLimitBucketStore;
    @Autowired JdbcClient jdbc;

    private MockMvc mockMvc;
    private String aliceName;
    private String carolName;
    private UUID aliceId;
    private UUID carolId;
    private UUID spaceId;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
        rateLimitBucketStore.clearAll();
        jdbc.sql("DELETE FROM mail_outbox").update();
        SharedGreenMail.server().purgeEmailFromAllMailboxes();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        aliceName = "alice-" + suffix;
        carolName = "carol-" + suffix;
        aliceId = saveUser(aliceName, "fr");
        carolId = saveUser(carolName, "fr");
        spaceId = saveSharedSpace("Chez nous", aliceId);
        saveMembership(spaceId, aliceId, SpaceRole.OWNER);
    }

    @AfterEach
    void removeWhatThisTestCreated() {
        jdbc.sql("DELETE FROM space_invitations WHERE space_id = :id").param("id", spaceId).update();
        jdbc.sql("DELETE FROM space_members WHERE space_id = :id").param("id", spaceId).update();
        jdbc.sql("DELETE FROM spaces WHERE id = :id").param("id", spaceId).update();
        jdbc.sql("DELETE FROM users WHERE id IN (:ids)").param("ids", List.of(aliceId, carolId)).update();
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
        mockMvc.perform(put("/api/notifications/preferences/types/space.invitation").cookie(accessTokenFor(carolId))
                .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
            .andExpect(status().isNoContent());

        invite(carolName, null).andExpect(status().isCreated());

        assertThat(SharedGreenMail.server().waitForIncomingEmail(1_500, 1)).isFalse();
    }

    @Test
    void switching_mail_off_stops_it_and_keeps_each_kind_as_chosen() throws Exception {
        mockMvc.perform(put("/api/notifications/preferences/channels/email").cookie(accessTokenFor(carolId))
                .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
            .andExpect(status().isNoContent());

        invite(carolName, null).andExpect(status().isCreated());

        assertThat(SharedGreenMail.server().waitForIncomingEmail(1_500, 1)).isFalse();
        mockMvc.perform(get("/api/notifications/preferences").cookie(accessTokenFor(carolId)))
            .andExpect(jsonPath("$.channels[0].enabled").value(false))
            .andExpect(jsonPath("$.types[?(@.type == 'space.invitation')].enabled").value(true));
    }

    @Test
    void a_refused_invitation_sends_nothing() throws Exception {
        saveMembership(spaceId, carolId, SpaceRole.MEMBER);

        invite(carolName, null).andExpect(status().isConflict());

        assertThat(SharedGreenMail.server().waitForIncomingEmail(1_500, 1)).isFalse();
    }

    private ResultActions invite(String identifier, String inviterLanguage) throws Exception {
        var request = post("/api/spaces/" + spaceId + "/invitations")
            .cookie(accessTokenFor(aliceId))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"identifier\":\"" + identifier + "\",\"role\":\"MEMBER\"}");
        if (inviterLanguage != null) {
            request.header("Accept-Language", inviterLanguage);
        }
        return mockMvc.perform(request);
    }

    private MimeMessage theOnlyMail() {
        assertThat(SharedGreenMail.server().waitForIncomingEmail(10_000, 1)).isTrue();
        MimeMessage[] received = SharedGreenMail.server().getReceivedMessages();
        assertThat(received).hasSize(1);
        return received[0];
    }

    private UUID saveUser(String username, String language) {
        UserIdentityEntity user = new UserIdentityEntity();
        user.setUsername(username);
        user.setEmail(username + "@test.local");
        user.setRole(Role.USER);
        user.setLanguage(language);
        return users.saveAndFlush(user).getId();
    }

    private UUID saveSharedSpace(String name, UUID creatorId) {
        SpaceEntity space = new SpaceEntity();
        space.setType(SpaceType.SHARED);
        space.setName(name);
        space.setAccent("#c17a5c");
        space.setGlyph("🏡");
        space.setCreatedBy(creatorId);
        return spaces.saveAndFlush(space).getId();
    }

    private void saveMembership(UUID spaceId, UUID userId, SpaceRole role) {
        SpaceMemberEntity member = new SpaceMemberEntity();
        member.setSpaceId(spaceId);
        member.setUserId(userId);
        member.setRole(role);
        members.saveAndFlush(member);
    }

    private Cookie accessTokenFor(UUID userId) {
        String token = Jwts.builder()
            .issuer("nido")
            .audience().add("nido").and()
            .subject(userId.toString())
            .claim("role", Role.USER.name())
            .claim("email", userId + "@test.local")
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plusSeconds(900)))
            .signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
            .compact();
        return new Cookie("access_token", token);
    }
}
