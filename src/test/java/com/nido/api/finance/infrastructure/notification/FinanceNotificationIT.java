package com.nido.api.finance.infrastructure.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nido.api.MailIntegrationTestConfig;
import com.nido.api.SharedGreenMail;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.infrastructure.ratelimit.RedisRateLimitBucketStore;
import com.nido.api.space.application.port.in.GetSpaceTodayUseCase;
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

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.nido.api.TestAccessTokens.cookieFor;
import static com.nido.api.finance.infrastructure.notification.FinanceNotificationITSupport.drain;
import static com.nido.api.finance.infrastructure.notification.FinanceNotificationITSupport.mailsByRecipient;
import static com.nido.api.finance.infrastructure.notification.FinanceNotificationITSupport.nothingBeyond;
import static com.nido.api.finance.infrastructure.notification.FinanceNotificationITSupport.subjectsByRecipient;
import static com.nido.api.finance.infrastructure.notification.FinanceNotificationITSupport.textOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Mail on: a space of three, and what each of them receives when one of them touches its finances. */
@MailIntegrationTestConfig
class FinanceNotificationIT {

    @Autowired WebApplicationContext webApplicationContext;
    @Autowired UserIdentityJpaRepository users;
    @Autowired SpaceJpaRepository spaces;
    @Autowired SpaceMemberJpaRepository members;
    @Autowired RedisRateLimitBucketStore rateLimitBucketStore;
    @Autowired JdbcClient jdbc;
    @Autowired GetSpaceTodayUseCase spaceToday;

    private final ObjectMapper json = new ObjectMapper();
    private MockMvc mockMvc;
    private String suffix;
    private UUID aliceId;
    private UUID bobId;
    private UUID carolId;
    private UUID spaceId;
    private String categoryId;
    private LocalDate today;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
        FinanceNotificationITSupport scene = new FinanceNotificationITSupport(users, spaces, members);
        rateLimitBucketStore.clearAll();
        jdbc.sql("DELETE FROM mail_outbox").update();
        SharedGreenMail.server().purgeEmailFromAllMailboxes();
        suffix = UUID.randomUUID().toString().substring(0, 8);
        aliceId = scene.saveUser(name("alice"));
        bobId = scene.saveUser(name("bob"));
        carolId = scene.saveUser(name("carol"));
        spaceId = scene.saveSharedSpace("Chez nous", aliceId);
        scene.saveMembership(spaceId, aliceId, SpaceRole.OWNER);
        scene.saveMembership(spaceId, bobId, SpaceRole.MEMBER);
        scene.saveMembership(spaceId, carolId, SpaceRole.MEMBER);
        today = spaceToday.today(spaceId);
        categoryId = json.readTree(mockMvc.perform(post(finance("/categories")).cookie(cookieFor(aliceId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"label\":\"Alimentation\",\"color\":\"#a3e635\",\"icon\":\"Utensils\",\"type\":\"EXPENSE\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText();
    }

    @AfterEach
    void removeWhatThisTestCreated() {
        jdbc.sql("DELETE FROM mail_outbox").update();
        // The space takes its finances with it: every finance table cascades from it.
        jdbc.sql("DELETE FROM space_members WHERE space_id = :id").param("id", spaceId).update();
        jdbc.sql("DELETE FROM spaces WHERE id = :id").param("id", spaceId).update();
        jdbc.sql("DELETE FROM users WHERE id IN (:ids)").param("ids", List.of(aliceId, bobId, carolId)).update();
    }

    @Test
    void a_shared_expense_tells_its_contributor_only() throws Exception {
        addExpense(aliceId, "Courses", "86.40", aliceId, aliceId, bobId);

        assertThat(subjectsByRecipient(1)).isEqualTo(Map.of(address("bob"), name("alice") + " a ajouté « Courses »"));
        assertThat(nothingBeyond(1)).isTrue();
    }

    @Test
    void removing_a_contributor_tells_both_the_removed_and_the_remaining() throws Exception {
        String id = addExpense(aliceId, "Courses", "90.00", aliceId, aliceId, bobId, carolId);
        drain(2);

        mockMvc.perform(patch(finance("/transactions/" + id)).cookie(cookieFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content(operation("Courses", "90.00", aliceId, aliceId, bobId)))
            .andExpect(status().isOk());

        assertThat(subjectsByRecipient(2)).isEqualTo(Map.of(
            address("bob"), name("alice") + " a modifié « Courses »",
            address("carol"), name("alice") + " vous a retiré de « Courses »"));
    }

    @Test
    void a_repayment_tells_the_creditor_what_is_left() throws Exception {
        addExpense(aliceId, "Courses", "86.40", aliceId, aliceId, bobId);
        drain(1);

        settle(bobId, bobId);

        Map<String, MimeMessage> mails = mailsByRecipient(1);
        assertThat(mails).containsOnlyKeys(address("alice"));
        assertThat(mails.get(address("alice")).getSubject()).isEqualTo(name("bob") + " vous a remboursé 43,20 €");
        assertThat(textOf(mails.get(address("alice")))).contains("Vous êtes quittes.");
        assertThat(nothingBeyond(1)).isTrue();
    }

    @Test
    void a_creditor_recording_a_repayment_tells_the_debtor() throws Exception {
        addExpense(aliceId, "Courses", "86.40", aliceId, aliceId, bobId);
        drain(1);

        settle(aliceId, bobId);

        Map<String, MimeMessage> mails = mailsByRecipient(1);
        assertThat(mails).containsOnlyKeys(address("bob"));
        assertThat(mails.get(address("bob")).getSubject()).isEqualTo(name("alice") + " a enregistré votre remboursement de 43,20 €");
        assertThat(textOf(mails.get(address("bob")))).contains("Vous êtes quittes.");
    }

    @Test
    void a_deposit_that_reaches_the_goal_falls_back_for_whoever_switched_reached_off() throws Exception {
        mockMvc.perform(put("/api/notifications/preferences/types/finance.savings-goal-reached").cookie(cookieFor(carolId))
                .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
            .andExpect(status().isNoContent());
        String goalId = json.readTree(mockMvc.perform(post(finance("/savings-goals")).cookie(cookieFor(aliceId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Vacances\",\"targetAmount\":100.00,\"color\":\"#5c7a58\",\"glyph\":\"🏖️\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText();
        drain(2);

        mockMvc.perform(post(finance("/savings-goals/" + goalId + "/contributions")).cookie(cookieFor(aliceId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":\"" + aliceId + "\",\"amount\":100.00,\"date\":\"" + today + "\"}"))
            .andExpect(status().isCreated());

        assertThat(subjectsByRecipient(2)).isEqualTo(Map.of(
            address("bob"), "Objectif « Vacances » atteint",
            address("carol"), name("alice") + " a versé 100,00 € sur « Vacances »"));
    }

    @Test
    void an_expense_past_the_budget_tells_the_others() throws Exception {
        mockMvc.perform(put(finance("/budgets/" + categoryId)).cookie(cookieFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"monthlyLimit\":100.00}"))
            .andExpect(status().isOk());

        addExpense(aliceId, "Courses", "120.00", null);

        assertThat(subjectsByRecipient(2)).isEqualTo(Map.of(
            address("bob"), "Budget Alimentation dépassé",
            address("carol"), "Budget Alimentation dépassé"));
        assertThat(nothingBeyond(2)).isTrue();
    }

    @Test
    void a_failing_mail_leaves_the_operation_recorded() throws Exception {
        // The outbox refuses this kind of mail, as a broken template would refuse it.
        jdbc.sql("ALTER TABLE mail_outbox ADD CONSTRAINT it_refuse_finance CHECK (kind <> 'finance/transaction-added') NOT VALID")
            .update();
        try {
            addExpense(aliceId, "Courses", "86.40", aliceId, aliceId, bobId);

            assertThat(nothingBeyond(0)).isTrue();
        } finally {
            jdbc.sql("ALTER TABLE mail_outbox DROP CONSTRAINT it_refuse_finance").update();
        }

        assertThat(jdbc.sql("SELECT count(*) FROM finance_transactions WHERE space_id = :space")
            .param("space", spaceId).query(Integer.class).single()).isOne();
    }

    private String addExpense(UUID author, String label, String amount, UUID payer, UUID... sharedBy) throws Exception {
        return json.readTree(mockMvc.perform(post(finance("/transactions")).cookie(cookieFor(author))
                .contentType(MediaType.APPLICATION_JSON).content(operation(label, amount, payer, sharedBy)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText();
    }

    private String operation(String label, String amount, UUID payer, UUID... sharedBy) {
        String contributors = Arrays.stream(sharedBy).map(id -> "{\"memberId\":\"" + id + "\"}").collect(Collectors.joining(","));
        return "{\"label\":\"" + label + "\",\"amount\":" + amount + ",\"type\":\"EXPENSE\",\"categoryId\":\"" + categoryId
            + "\",\"date\":\"" + today + "\"" + (payer == null ? "" : ",\"payerId\":\"" + payer + "\"")
            + ",\"contributors\":[" + contributors + "]}";
    }

    /** {@code debtor} pays alice back what they owe her for the shared groceries, 43,20 €, as {@code author} records it. */
    private void settle(UUID author, UUID debtor) throws Exception {
        mockMvc.perform(post(finance("/balances/settle")).cookie(cookieFor(author)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromMemberId\":\"" + debtor + "\",\"toMemberId\":\"" + aliceId + "\",\"amount\":43.20,\"date\":\""
                    + today + "\"}"))
            .andExpect(status().isCreated());
    }

    private String finance(String path) {
        return "/api/spaces/" + spaceId + "/finance" + path;
    }

    private String name(String who) {
        return who + "-" + suffix;
    }

    private String address(String who) {
        return name(who) + "@test.local";
    }
}
