package com.nido.api.space.infrastructure.notification;

import com.nido.api.MailIntegrationTestConfig;
import com.nido.api.SharedGreenMail;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.infrastructure.ratelimit.RedisRateLimitBucketStore;
import com.nido.api.mail.domain.model.OutgoingMail;
import com.nido.api.mail.domain.model.Recipient;
import com.nido.api.mail.domain.model.RenderedMail;
import com.nido.api.mail.domain.port.out.MailOutboxPort;
import com.nido.api.shared.model.Role;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.infrastructure.persistence.repository.SpaceJpaRepository;
import com.nido.api.space.infrastructure.persistence.repository.SpaceMemberJpaRepository;
import jakarta.mail.internet.MimeMessage;
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

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.nido.api.TestAccessTokens.cookieFor;
import static com.nido.api.space.infrastructure.notification.SpaceNotificationITSupport.mailsByRecipient;
import static com.nido.api.space.infrastructure.notification.SpaceNotificationITSupport.subjectsByRecipient;
import static com.nido.api.space.infrastructure.notification.SpaceNotificationITSupport.textOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Mail on: a space of three, and what each of them receives when one removes another or deletes it. */
@MailIntegrationTestConfig
class SpaceLifeNotificationIT {

    @Autowired WebApplicationContext webApplicationContext;
    @Autowired UserIdentityJpaRepository users;
    @Autowired SpaceJpaRepository spaces;
    @Autowired SpaceMemberJpaRepository members;
    @Autowired RedisRateLimitBucketStore rateLimitBucketStore;
    @Autowired JdbcClient jdbc;
    @Autowired MailOutboxPort outbox;

    private MockMvc mockMvc;
    private SpaceNotificationITSupport scene;
    private String suffix;
    private UUID aliceId;
    private UUID bobId;
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
        aliceId = scene.saveUser(name("alice"), "fr");
        bobId = scene.saveUser(name("bob"), "fr");
        carolId = scene.saveUser(name("carol"), "fr");
        spaceId = scene.saveSharedSpace("Chez nous", aliceId);
        scene.saveMembership(spaceId, aliceId, SpaceRole.OWNER);
        scene.saveMembership(spaceId, bobId, SpaceRole.MEMBER);
        scene.saveMembership(spaceId, carolId, SpaceRole.MEMBER);
    }

    @AfterEach
    void removeWhatThisTestCreated() {
        jdbc.sql("DELETE FROM mail_outbox").update();
        jdbc.sql("DELETE FROM space_members WHERE space_id = :id").param("id", spaceId).update();
        jdbc.sql("DELETE FROM spaces WHERE id = :id").param("id", spaceId).update();
        List<UUID> accounts = rootId == null ? List.of(aliceId, bobId, carolId) : List.of(aliceId, bobId, carolId, rootId);
        jdbc.sql("DELETE FROM users WHERE id IN (:ids)").param("ids", accounts).update();
    }

    @Test
    void removing_a_member_tells_them_and_the_others_but_not_the_author() throws Exception {
        mockMvc.perform(delete("/api/spaces/" + spaceId + "/members/" + bobId).cookie(cookieFor(aliceId)))
            .andExpect(status().isNoContent());

        Map<String, String> subjects = subjectsByRecipient(2);
        assertThat(subjects).containsOnlyKeys(address("bob"), address("carol"));
        assertThat(subjects.get(address("bob"))).isEqualTo("Vous ne faites plus partie de Chez nous");
        assertThat(subjects.get(address("carol"))).isEqualTo(name("bob") + " ne fait plus partie de Chez nous");
    }

    @Test
    void deleting_the_space_tells_everyone_but_its_owner() throws Exception {
        mockMvc.perform(delete("/api/spaces/" + spaceId).cookie(cookieFor(aliceId)))
            .andExpect(status().isNoContent());

        Map<String, String> subjects = subjectsByRecipient(2);
        assertThat(subjects).containsOnlyKeys(address("bob"), address("carol"));
        assertThat(subjects.values()).containsOnly("L’espace Chez nous a été supprimé");
    }

    @Test
    void a_kind_switched_off_silences_only_whoever_switched_it_off() throws Exception {
        mockMvc.perform(put("/api/notifications/preferences/types/space.member-removed").cookie(cookieFor(carolId))
                .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
            .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/spaces/" + spaceId + "/members/" + bobId).cookie(cookieFor(aliceId)))
            .andExpect(status().isNoContent());

        assertThat(subjectsByRecipient(1)).containsOnlyKeys(address("bob"));
        assertThat(SharedGreenMail.server().waitForIncomingEmail(1_500, 2)).isFalse();
    }

    @Test
    void a_mail_that_cannot_be_queued_leaves_the_removal_done_and_the_other_mails_sent() throws Exception {
        // The outbox refuses the removed member's mail only, as a broken template would refuse that one mail.
        jdbc.sql("ALTER TABLE mail_outbox ADD CONSTRAINT it_refuse_removed CHECK (kind <> 'space/removed') NOT VALID").update();
        try {
            mockMvc.perform(delete("/api/spaces/" + spaceId + "/members/" + bobId).cookie(cookieFor(aliceId)))
                .andExpect(status().isNoContent());

            // Each notification is delivered on its own: carol's arrives although bob's failed.
            assertThat(subjectsByRecipient(1)).containsOnlyKeys(address("carol"));
            assertThat(SharedGreenMail.server().waitForIncomingEmail(1_500, 2)).isFalse();
        } finally {
            jdbc.sql("ALTER TABLE mail_outbox DROP CONSTRAINT it_refuse_removed").update();
        }

        assertThat(jdbc.sql("SELECT count(*) FROM space_members WHERE space_id = :space AND user_id = :user")
            .param("space", spaceId).param("user", bobId).query(Integer.class).single()).isZero();
    }

    @Test
    void erasing_the_owner_tells_the_successor_and_the_others_who_left() throws Exception {
        // bob outranks carol, so he is the successor whatever the order they joined in.
        jdbc.sql("UPDATE space_members SET role = 'ADMIN' WHERE space_id = :space AND user_id = :user")
            .param("space", spaceId).param("user", bobId).update();

        mockMvc.perform(delete("/api/users/" + aliceId).cookie(cookieFor(saveSuperAdmin(), Role.SUPER_ADMIN)))
            .andExpect(status().isNoContent());

        Map<String, MimeMessage> mails = mailsByRecipient(2);
        assertThat(mails).containsOnlyKeys(address("bob"), address("carol"));
        assertThat(mails.get(address("bob")).getSubject()).isEqualTo("Vous êtes propriétaire de Chez nous");
        assertThat(textOf(mails.get(address("bob"))))
            .contains(name("alice") + " a quitté Nido : vous êtes maintenant propriétaire de l’espace « Chez nous ».");
        assertThat(mails.get(address("carol")).getSubject()).isEqualTo(name("bob") + " est propriétaire de Chez nous");
        assertThat(textOf(mails.get(address("carol"))))
            .contains(name("alice") + " a quitté Nido : " + name("bob") + " est maintenant propriétaire de l’espace « Chez nous ».");
        assertThat(jdbc.sql("SELECT is_deleted FROM users WHERE id = :id").param("id", aliceId).query(Boolean.class).single())
            .isTrue();
    }

    @Test
    void erasing_a_member_tells_the_others_they_left_nido() throws Exception {
        mockMvc.perform(delete("/api/users/" + carolId).cookie(cookieFor(saveSuperAdmin(), Role.SUPER_ADMIN)))
            .andExpect(status().isNoContent());

        Map<String, MimeMessage> mails = mailsByRecipient(2);
        assertThat(mails).containsOnlyKeys(address("alice"), address("bob"));
        assertThat(mails.get(address("alice")).getSubject()).isEqualTo(name("carol") + " a quitté Chez nous");
        assertThat(textOf(mails.get(address("bob"))))
            .contains(name("carol") + " a quitté Nido et ne fait plus partie de l’espace « Chez nous ».");
    }

    @Test
    void erasing_an_account_withdraws_the_mails_still_waiting_for_it() throws Exception {
        // Due in an hour: no dispatch takes them meanwhile, whatever this test sends.
        Instant later = Instant.now().plus(Duration.ofHours(1));
        outbox.enqueue("it/waiting", waiting(address("carol")), later, null);
        outbox.enqueue("it/waiting", waiting(address("bob")), later, null);

        mockMvc.perform(delete("/api/users/" + carolId).cookie(cookieFor(saveSuperAdmin(), Role.SUPER_ADMIN)))
            .andExpect(status().isNoContent());

        // The erasure's own mails, due now, go out: waited for here, they cannot reach the next test.
        assertThat(mailsByRecipient(2)).containsOnlyKeys(address("alice"), address("bob"));
        assertThat(outbox.claimDue(later, 20, Duration.ofMinutes(1)))
            .filteredOn(entry -> entry.kind().equals("it/waiting"))
            .extracting(entry -> entry.mail().to().address())
            .containsExactly(address("bob"));
    }

    private static OutgoingMail waiting(String address) {
        return new OutgoingMail(new Recipient(address, null), new RenderedMail("s", "<p>h</p>", "t"));
    }

    private UUID saveSuperAdmin() {
        rootId = scene.saveUser(name("root"), null, Role.SUPER_ADMIN);
        return rootId;
    }

    private String name(String who) {
        return who + "-" + suffix;
    }

    private String address(String who) {
        return name(who) + "@test.local";
    }
}
