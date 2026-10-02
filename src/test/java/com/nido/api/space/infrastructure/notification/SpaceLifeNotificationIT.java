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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Mail on: a space of three, and what each of them receives when one removes another or deletes it. */
@MailIntegrationTestConfig
class SpaceLifeNotificationIT {

    private static final String JWT_SECRET = "integration-test-secret-at-least-32-chars!";

    @Autowired WebApplicationContext webApplicationContext;
    @Autowired UserIdentityJpaRepository users;
    @Autowired SpaceJpaRepository spaces;
    @Autowired SpaceMemberJpaRepository members;
    @Autowired RedisRateLimitBucketStore rateLimitBucketStore;
    @Autowired JdbcClient jdbc;

    private MockMvc mockMvc;
    private String suffix;
    private UUID aliceId;
    private UUID bobId;
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
        suffix = UUID.randomUUID().toString().substring(0, 8);
        aliceId = saveUser("alice");
        bobId = saveUser("bob");
        carolId = saveUser("carol");
        spaceId = saveSharedSpace("Chez nous", aliceId);
        saveMembership(aliceId, SpaceRole.OWNER);
        saveMembership(bobId, SpaceRole.MEMBER);
        saveMembership(carolId, SpaceRole.MEMBER);
    }

    @AfterEach
    void removeWhatThisTestCreated() {
        jdbc.sql("DELETE FROM space_members WHERE space_id = :id").param("id", spaceId).update();
        jdbc.sql("DELETE FROM spaces WHERE id = :id").param("id", spaceId).update();
        jdbc.sql("DELETE FROM users WHERE id IN (:ids)").param("ids", List.of(aliceId, bobId, carolId)).update();
    }

    @Test
    void removing_a_member_tells_them_and_the_others_but_not_the_author() throws Exception {
        mockMvc.perform(delete("/api/spaces/" + spaceId + "/members/" + bobId).cookie(accessTokenFor(aliceId)))
            .andExpect(status().isNoContent());

        Map<String, String> subjects = subjectsByRecipient(2);
        assertThat(subjects).containsOnlyKeys(address("bob"), address("carol"));
        assertThat(subjects.get(address("bob"))).isEqualTo("Vous ne faites plus partie de Chez nous");
        assertThat(subjects.get(address("carol"))).isEqualTo(name("bob") + " ne fait plus partie de Chez nous");
    }

    @Test
    void deleting_the_space_tells_everyone_but_its_owner() throws Exception {
        mockMvc.perform(delete("/api/spaces/" + spaceId).cookie(accessTokenFor(aliceId)))
            .andExpect(status().isNoContent());

        Map<String, String> subjects = subjectsByRecipient(2);
        assertThat(subjects).containsOnlyKeys(address("bob"), address("carol"));
        assertThat(subjects.values()).containsOnly("L’espace Chez nous a été supprimé");
    }

    @Test
    void a_kind_switched_off_silences_only_whoever_switched_it_off() throws Exception {
        mockMvc.perform(put("/api/notifications/preferences/types/space.member-removed").cookie(accessTokenFor(carolId))
                .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
            .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/spaces/" + spaceId + "/members/" + bobId).cookie(accessTokenFor(aliceId)))
            .andExpect(status().isNoContent());

        assertThat(subjectsByRecipient(1)).containsOnlyKeys(address("bob"));
        assertThat(SharedGreenMail.server().waitForIncomingEmail(1_500, 2)).isFalse();
    }

    /** Waits for {@code count} mails and returns their subjects by recipient address. */
    private Map<String, String> subjectsByRecipient(int count) throws Exception {
        assertThat(SharedGreenMail.server().waitForIncomingEmail(10_000, count)).isTrue();
        MimeMessage[] received = SharedGreenMail.server().getReceivedMessages();
        return Arrays.stream(received).collect(Collectors.toMap(this::recipientOf, this::subjectOf));
    }

    private String recipientOf(MimeMessage message) {
        try {
            return ((InternetAddress) message.getRecipients(MimeMessage.RecipientType.TO)[0]).getAddress();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String subjectOf(MimeMessage message) {
        try {
            return message.getSubject();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String name(String who) {
        return who + "-" + suffix;
    }

    private String address(String who) {
        return name(who) + "@test.local";
    }

    private UUID saveUser(String who) {
        UserIdentityEntity user = new UserIdentityEntity();
        user.setUsername(name(who));
        user.setEmail(address(who));
        user.setRole(Role.USER);
        user.setLanguage("fr");
        return users.saveAndFlush(user).getId();
    }

    private UUID saveSharedSpace(String spaceName, UUID creatorId) {
        SpaceEntity space = new SpaceEntity();
        space.setType(SpaceType.SHARED);
        space.setName(spaceName);
        space.setAccent("#c17a5c");
        space.setGlyph("🏡");
        space.setCreatedBy(creatorId);
        return spaces.saveAndFlush(space).getId();
    }

    private void saveMembership(UUID userId, SpaceRole role) {
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
