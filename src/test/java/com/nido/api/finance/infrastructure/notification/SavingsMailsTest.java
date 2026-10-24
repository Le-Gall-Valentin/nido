package com.nido.api.finance.infrastructure.notification;

import com.nido.api.finance.domain.model.ContributorSide;
import com.nido.api.finance.domain.model.GoalFacts;
import com.nido.api.finance.domain.model.MemberShare;
import com.nido.api.finance.domain.model.SavingsDeposit;
import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.RenderedMail;
import com.nido.api.mail.infrastructure.render.ThymeleafMailRenderer;
import com.nido.api.notifications.domain.model.NotificationKind;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The mails about the savings of a space: a deposit, a goal reached, created, changed or deleted. */
class SavingsMailsTest {

    private static final ThymeleafMailRenderer RENDERER = new ThymeleafMailRenderer("https://nido.example");
    private static final AppPath FINANCE = new AppPath("/s/42/finance");
    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();
    private static final GoalFacts HOLIDAYS = new GoalFacts("Vacances", new BigDecimal("1000.00"), LocalDate.of(2027, 7, 1));

    private static SavingsDeposit deposit(String saved, List<MemberShare> savedByMember) {
        return new SavingsDeposit(HOLIDAYS, BOB, "bob", new BigDecimal("100.00"), LocalDate.of(2026, 10, 21),
            new BigDecimal(saved), savedByMember);
    }

    private static SavingsDeposit deposit() {
        return deposit("700.00", List.of());
    }

    private static String kindOf(Class<?> record) {
        return record.getAnnotation(NotificationKind.class).value();
    }

    @Test
    void each_mail_declares_its_kind_and_template() {
        assertThat(kindOf(SavingsContributionNotification.class)).isEqualTo("finance.savings-contribution");
        assertThat(kindOf(SavingsGoalReachedNotification.class)).isEqualTo("finance.savings-goal-reached");
        assertThat(kindOf(SavingsGoalCreatedNotification.class)).isEqualTo("finance.savings-goal-created");
        assertThat(kindOf(SavingsGoalUpdatedNotification.class)).isEqualTo("finance.savings-goal-updated");
        assertThat(kindOf(SavingsGoalDeletedNotification.class)).isEqualTo("finance.savings-goal-deleted");
        assertThat(new SavingsContributionNotification("u", "a", "s", ContributorSide.AUTHOR, deposit(), FINANCE).template())
            .isEqualTo("finance/savings-contribution");
        assertThat(new SavingsGoalReachedNotification("u", "a", "s", ContributorSide.AUTHOR, deposit(), FINANCE).template())
            .isEqualTo("finance/savings-goal-reached");
        assertThat(new SavingsGoalCreatedNotification("u", "a", "s", HOLIDAYS, FINANCE).template())
            .isEqualTo("finance/savings-goal-created");
        assertThat(new SavingsGoalUpdatedNotification("u", "a", "s", HOLIDAYS, HOLIDAYS, FINANCE).template())
            .isEqualTo("finance/savings-goal-updated");
        assertThat(new SavingsGoalDeletedNotification("u", "a", "s", HOLIDAYS, BigDecimal.ZERO, List.of(), FINANCE).template())
            .isEqualTo("finance/savings-goal-deleted");
    }

    @Test
    void a_deposit_by_its_author() {
        RenderedMail fr = RENDERER.render(new SavingsContributionNotification("carol", "bob", "Chez nous",
            ContributorSide.AUTHOR, deposit(), FINANCE), Locale.FRENCH);
        RenderedMail en = RENDERER.render(new SavingsContributionNotification("carol", "bob", "Our place",
            ContributorSide.AUTHOR, deposit(), FINANCE), Locale.ENGLISH);

        assertThat(fr.subject()).isEqualTo("bob a versé 100,00 € sur « Vacances »");
        assertThat(fr.text()).contains("bob a versé 100,00 € sur l’objectif « Vacances » de l’espace « Chez nous ».",
            "Voir les finances\nhttps://nido.example/s/42/finance");
        assertThat(en.subject()).isEqualTo("bob put €100.00 into “Vacances”");
        assertThat(en.text()).contains("bob put €100.00 into the goal “Vacances” of the space “Our place”.");
    }

    @Test
    void a_deposit_in_another_member_s_name() {
        RenderedMail fr = RENDERER.render(new SavingsContributionNotification("carol", "alice", "Chez nous",
            ContributorSide.OTHER_MEMBER, deposit(), FINANCE), Locale.FRENCH);
        RenderedMail en = RENDERER.render(new SavingsContributionNotification("carol", "alice", "Our place",
            ContributorSide.OTHER_MEMBER, deposit(), FINANCE), Locale.ENGLISH);

        assertThat(fr.subject()).isEqualTo("alice a enregistré 100,00 € au nom de bob sur « Vacances »");
        assertThat(fr.text()).contains(
            "alice a enregistré un versement de 100,00 € au nom de bob sur l’objectif « Vacances » de l’espace « Chez nous ».");
        assertThat(en.subject()).isEqualTo("alice recorded €100.00 from bob into “Vacances”");
        assertThat(en.text()).contains("alice recorded a deposit of €100.00 from bob into the goal “Vacances” of the space “Our place”.");
    }

    @Test
    void a_deposit_in_the_reader_s_name() {
        RenderedMail fr = RENDERER.render(new SavingsContributionNotification("bob", "alice", "Chez nous",
            ContributorSide.READER, deposit(), FINANCE), Locale.FRENCH);
        RenderedMail en = RENDERER.render(new SavingsContributionNotification("bob", "alice", "Our place",
            ContributorSide.READER, deposit(), FINANCE), Locale.ENGLISH);

        assertThat(fr.subject()).isEqualTo("alice a enregistré 100,00 € à votre nom sur « Vacances »");
        assertThat(fr.text()).contains(
            "alice a enregistré un versement de 100,00 € à votre nom sur l’objectif « Vacances » de l’espace « Chez nous ».");
        assertThat(en.subject()).isEqualTo("alice recorded €100.00 from you into “Vacances”");
        assertThat(en.text()).contains("alice recorded a deposit of €100.00 from you into the goal “Vacances” of the space “Our place”.");
    }

    @Test
    void the_progress_reads_as_saved_of_target() {
        RenderedMail fr = RENDERER.render(new SavingsContributionNotification("carol", "bob", "Chez nous",
            ContributorSide.AUTHOR, deposit(), FINANCE), Locale.FRENCH);
        RenderedMail en = RENDERER.render(new SavingsContributionNotification("carol", "bob", "Our place",
            ContributorSide.AUTHOR, deposit(), FINANCE), Locale.ENGLISH);

        assertThat(fr.text()).contains("Versement : 100,00 €", "Date : 21 octobre 2026",
            "Progression : 700,00 € sur 1 000,00 € (70 %)", "Date visée : 1 juillet 2027");
        assertThat(en.text()).contains("Progress: €700.00 of €1,000.00 (70%)", "Target date: 1 July 2027");
    }

    @Test
    void a_reached_goal_lists_who_saved_what() {
        SavingsDeposit last = deposit("1000.00", List.of(new MemberShare(ALICE, "alice", new BigDecimal("600.00")),
            new MemberShare(BOB, null, new BigDecimal("400.00"))));

        RenderedMail fr = RENDERER.render(new SavingsGoalReachedNotification("carol", "bob", "Chez nous",
            ContributorSide.AUTHOR, last, FINANCE), Locale.FRENCH);
        RenderedMail mine = RENDERER.render(new SavingsGoalReachedNotification("bob", "alice", "Chez nous",
            ContributorSide.READER, last, FINANCE), Locale.FRENCH);
        RenderedMail en = RENDERER.render(new SavingsGoalReachedNotification("carol", "bob", "Our place",
            ContributorSide.AUTHOR, last, FINANCE), Locale.ENGLISH);

        assertThat(fr.subject()).isEqualTo("Objectif « Vacances » atteint");
        assertThat(fr.text()).contains(
            "Avec le versement de 100,00 € de bob, l’objectif « Vacances » de l’espace « Chez nous » est atteint : 1 000,00 € épargnés.",
            "Épargné en tout : 1 000,00 €", "alice : 600,00 €", "un ancien membre : 400,00 €");
        assertThat(mine.text()).contains(
            "Avec votre versement de 100,00 €, l’objectif « Vacances » de l’espace « Chez nous » est atteint : 1 000,00 € épargnés.");
        assertThat(en.subject()).isEqualTo("Goal “Vacances” reached");
        assertThat(en.text()).contains(
            "With bob’s deposit of €100.00, the goal “Vacances” of the space “Our place” is reached: €1,000.00 saved.",
            "Saved in total: €1,000.00", "alice: €600.00", "a former member: €400.00");
    }

    @Test
    void a_new_goal() {
        RenderedMail fr = RENDERER.render(new SavingsGoalCreatedNotification("bob", "alice", "Chez nous", HOLIDAYS, FINANCE),
            Locale.FRENCH);
        RenderedMail en = RENDERER.render(new SavingsGoalCreatedNotification("bob", "alice", "Our place", HOLIDAYS, FINANCE),
            Locale.ENGLISH);

        assertThat(fr.subject()).isEqualTo("alice a créé l’objectif « Vacances »");
        assertThat(fr.text()).contains("alice a créé l’objectif d’épargne « Vacances » dans l’espace « Chez nous ».",
            "Montant visé : 1 000,00 €", "Date visée : 1 juillet 2027");
        assertThat(en.subject()).isEqualTo("alice created the goal “Vacances”");
        assertThat(en.text()).contains("alice created the savings goal “Vacances” in the space “Our place”.", "Target amount: €1,000.00");
    }

    @Test
    void a_goal_change_shows_before_and_after() {
        GoalFacts after = new GoalFacts("Vacances", new BigDecimal("1200.00"), null);

        RenderedMail fr = RENDERER.render(new SavingsGoalUpdatedNotification("bob", "alice", "Chez nous", HOLIDAYS, after, FINANCE),
            Locale.FRENCH);
        RenderedMail en = RENDERER.render(new SavingsGoalUpdatedNotification("bob", "alice", "Our place", HOLIDAYS, after, FINANCE),
            Locale.ENGLISH);

        assertThat(fr.subject()).isEqualTo("alice a modifié l’objectif « Vacances »");
        assertThat(fr.text()).contains("alice a modifié l’objectif d’épargne « Vacances » de l’espace « Chez nous ».",
            "Montant visé : 1 000,00 € → 1 200,00 €", "Date visée : 1 juillet 2027 → aucune")
            .doesNotContain("Nom :");
        assertThat(en.subject()).isEqualTo("alice changed the goal “Vacances”");
        assertThat(en.text()).contains("Target amount: €1,000.00 → €1,200.00", "Target date: 1 July 2027 → none");
    }

    @Test
    void a_renamed_goal_is_named_as_it_is_now() {
        GoalFacts renamed = new GoalFacts("Voyage", new BigDecimal("1000.00"), LocalDate.of(2027, 7, 1));

        RenderedMail fr = RENDERER.render(new SavingsGoalUpdatedNotification("bob", "alice", "Chez nous", HOLIDAYS, renamed, FINANCE),
            Locale.FRENCH);

        assertThat(fr.subject()).isEqualTo("alice a modifié l’objectif « Voyage »");
        assertThat(fr.text()).contains("Nom : Vacances → Voyage");
    }

    @Test
    void a_deleted_goal_says_its_deposits_went_with_it() {
        RenderedMail fr = RENDERER.render(new SavingsGoalDeletedNotification("bob", "alice", "Chez nous", HOLIDAYS,
            new BigDecimal("500.00"), List.of(new MemberShare(ALICE, "alice", new BigDecimal("300.00")),
                new MemberShare(BOB, "bob", new BigDecimal("200.00"))), FINANCE), Locale.FRENCH);
        RenderedMail en = RENDERER.render(new SavingsGoalDeletedNotification("bob", "alice", "Our place", HOLIDAYS,
            BigDecimal.ZERO, List.of(), FINANCE), Locale.ENGLISH);

        assertThat(fr.subject()).isEqualTo("alice a supprimé l’objectif « Vacances »");
        assertThat(fr.text()).contains("alice a supprimé l’objectif d’épargne « Vacances » de l’espace « Chez nous ». "
            + "Les versements enregistrés sur cet objectif ont été supprimés avec lui.",
            "Épargné en tout : 500,00 €", "alice : 300,00 €", "bob : 200,00 €");
        assertThat(en.subject()).isEqualTo("alice deleted the goal “Vacances”");
        assertThat(en.text()).contains("alice deleted the savings goal “Vacances” of the space “Our place”. "
            + "The deposits recorded on it were deleted with it.", "Saved in total: €0.00");
    }

    @Test
    void a_deposit_by_a_member_who_can_no_longer_be_named() {
        SavingsDeposit unnamed = new SavingsDeposit(HOLIDAYS, BOB, null, new BigDecimal("100.00"), LocalDate.of(2026, 10, 21),
            new BigDecimal("1000.00"), List.of(new MemberShare(BOB, null, new BigDecimal("1000.00"))));

        RenderedMail fr = RENDERER.render(new SavingsContributionNotification("carol", "alice", "Chez nous",
            ContributorSide.OTHER_MEMBER, unnamed, FINANCE), Locale.FRENCH);
        RenderedMail en = RENDERER.render(new SavingsContributionNotification("carol", "alice", "Our place",
            ContributorSide.OTHER_MEMBER, unnamed, FINANCE), Locale.ENGLISH);
        RenderedMail reached = RENDERER.render(new SavingsGoalReachedNotification("carol", "alice", "Chez nous",
            ContributorSide.OTHER_MEMBER, unnamed, FINANCE), Locale.FRENCH);
        RenderedMail reachedEn = RENDERER.render(new SavingsGoalReachedNotification("carol", "alice", "Our place",
            ContributorSide.OTHER_MEMBER, unnamed, FINANCE), Locale.ENGLISH);

        assertThat(fr.subject()).isEqualTo("alice a enregistré 100,00 € au nom d’un ancien membre sur « Vacances »");
        assertThat(fr.text()).contains(
            "alice a enregistré un versement de 100,00 € au nom d’un ancien membre sur l’objectif « Vacances » de l’espace « Chez nous ».");
        assertThat(en.subject()).isEqualTo("alice recorded €100.00 from a former member into “Vacances”");
        assertThat(en.text()).contains(
            "alice recorded a deposit of €100.00 from a former member into the goal “Vacances” of the space “Our place”.");
        assertThat(reached.text()).contains("Avec le versement de 100,00 € d’un ancien membre, l’objectif « Vacances » de l’espace "
            + "« Chez nous » est atteint : 1\u202f000,00 € épargnés.", "un ancien membre : 1\u202f000,00 €");
        assertThat(reachedEn.text()).contains("With a former member’s deposit of €100.00, the goal “Vacances” of the space "
            + "“Our place” is reached: €1,000.00 saved.");
    }
}
