package com.nido.api.finance.infrastructure.notification;

import com.nido.api.finance.domain.model.Involvement;
import com.nido.api.finance.domain.model.MemberShare;
import com.nido.api.finance.domain.model.OperationFacts;
import com.nido.api.finance.domain.model.RecurrenceInterval;
import com.nido.api.finance.domain.model.SeriesFacts;
import com.nido.api.finance.domain.model.TransactionType;
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

/**
 * The mails about an operation or a recurring transaction. The plain-text part is read: it is what every sentence
 * of the HTML must also say, and it writes a no-break space as a space.
 */
class OperationMailsTest {

    private static final ThymeleafMailRenderer RENDERER = new ThymeleafMailRenderer("https://nido.example");
    private static final AppPath FINANCE = new AppPath("/s/42/finance");
    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();
    private static final UUID CAROL = UUID.randomUUID();

    private static OperationFacts groceries(String amount, String share, TransactionType type, UUID payer, String payerName) {
        return new OperationFacts("Courses", new BigDecimal(amount), type, "Alimentation", LocalDate.of(2026, 10, 21),
            payer, payerName, List.of(new MemberShare(ALICE, "alice", new BigDecimal(share)),
                new MemberShare(BOB, "bob", new BigDecimal(share))));
    }

    private static OperationFacts groceries() {
        return groceries("86.40", "43.20", TransactionType.EXPENSE, ALICE, "alice");
    }

    private static SeriesFacts rent(RecurrenceInterval interval, int count, LocalDate until) {
        return new SeriesFacts("Loyer", new BigDecimal("1200.00"), TransactionType.EXPENSE, "Logement", ALICE, "alice",
            List.of(new MemberShare(ALICE, "alice", new BigDecimal("600.00")), new MemberShare(BOB, "bob", new BigDecimal("600.00"))),
            interval, count, LocalDate.of(2026, 11, 1), until);
    }

    private static String kindOf(Class<?> record) {
        return record.getAnnotation(NotificationKind.class).value();
    }

    @Test
    void each_mail_declares_its_kind_and_template() {
        assertThat(kindOf(TransactionAddedNotification.class)).isEqualTo("finance.transaction-added");
        assertThat(kindOf(TransactionUpdatedNotification.class)).isEqualTo("finance.transaction-updated");
        assertThat(kindOf(TransactionDeletedNotification.class)).isEqualTo("finance.transaction-deleted");
        assertThat(kindOf(RecurringCreatedNotification.class)).isEqualTo("finance.recurring-created");
        assertThat(kindOf(RecurringUpdatedNotification.class)).isEqualTo("finance.recurring-updated");
        assertThat(kindOf(RecurringStoppedNotification.class)).isEqualTo("finance.recurring-stopped");
        assertThat(new TransactionAddedNotification("u", BOB, "a", "s", groceries(), FINANCE).template())
            .isEqualTo("finance/transaction-added");
        assertThat(new TransactionUpdatedNotification("u", BOB, "a", "s", Involvement.CHANGED, groceries(), groceries(), FINANCE)
            .template()).isEqualTo("finance/transaction-updated");
        assertThat(new TransactionDeletedNotification("u", BOB, "a", "s", groceries(), FINANCE).template())
            .isEqualTo("finance/transaction-deleted");
        SeriesFacts rent = rent(RecurrenceInterval.MONTHLY, 1, null);
        assertThat(new RecurringCreatedNotification("u", BOB, "a", "s", rent, FINANCE).template()).isEqualTo("finance/recurring-created");
        assertThat(new RecurringUpdatedNotification("u", BOB, "a", "s", Involvement.CHANGED, rent, rent, FINANCE).template())
            .isEqualTo("finance/recurring-updated");
        assertThat(new RecurringStoppedNotification("u", BOB, "a", "s", rent, FINANCE).template()).isEqualTo("finance/recurring-stopped");
    }

    @Test
    void an_added_expense_says_who_paid_and_your_share() {
        RenderedMail fr = RENDERER.render(new TransactionAddedNotification("bob", BOB, "alice", "Chez nous", groceries(), FINANCE),
            Locale.FRENCH);
        RenderedMail en = RENDERER.render(new TransactionAddedNotification("bob", BOB, "alice", "Our place", groceries(), FINANCE),
            Locale.ENGLISH);

        assertThat(fr.subject()).isEqualTo("alice a ajouté « Courses »");
        assertThat(fr.text()).contains(
            "Bonjour bob,",
            "alice a ajouté une opération qui vous concerne dans l’espace « Chez nous ».",
            "Montant : 86,40 €",
            "Date : 21 octobre 2026",
            "Catégorie : Alimentation",
            "Payée par : alice",
            "Partagée avec : alice (43,20 €), bob (43,20 €)",
            "Votre part : 43,20 €",
            "Voir les finances\nhttps://nido.example/s/42/finance");
        assertThat(en.subject()).isEqualTo("alice added “Courses”");
        assertThat(en.text()).contains(
            "alice added a transaction that involves you in the space “Our place”.",
            "Amount: €86.40",
            "Paid by: alice",
            "Shared with: alice (€43.20), bob (€43.20)",
            "Your share: €43.20",
            "See the finances\nhttps://nido.example/s/42/finance");
    }

    @Test
    void an_income_is_received_not_paid() {
        OperationFacts refund = groceries("86.40", "43.20", TransactionType.INCOME, ALICE, "alice");

        RenderedMail fr = RENDERER.render(new TransactionAddedNotification("bob", BOB, "alice", "Chez nous", refund, FINANCE),
            Locale.FRENCH);

        assertThat(fr.text()).contains("Reçue par : alice").doesNotContain("Payée par");
    }

    @Test
    void the_reader_who_paid_reads_you() {
        OperationFacts paidByBob = groceries("86.40", "43.20", TransactionType.EXPENSE, BOB, "bob");

        RenderedMail fr = RENDERER.render(new TransactionAddedNotification("bob", BOB, "alice", "Chez nous", paidByBob, FINANCE),
            Locale.FRENCH);

        assertThat(fr.text()).contains("Payée par : vous");
    }

    @Test
    void a_former_member_is_named_as_such() {
        OperationFacts formerPayer = new OperationFacts("Courses", new BigDecimal("86.40"), TransactionType.EXPENSE,
            "Alimentation", LocalDate.of(2026, 10, 21), CAROL, null,
            List.of(new MemberShare(CAROL, null, new BigDecimal("43.20")), new MemberShare(BOB, "bob", new BigDecimal("43.20"))));

        RenderedMail fr = RENDERER.render(new TransactionAddedNotification("bob", BOB, "alice", "Chez nous", formerPayer, FINANCE),
            Locale.FRENCH);
        RenderedMail en = RENDERER.render(new TransactionAddedNotification("bob", BOB, "alice", "Our place", formerPayer, FINANCE),
            Locale.ENGLISH);

        assertThat(fr.text()).contains("Payée par : un ancien membre", "Partagée avec : un ancien membre (43,20 €), bob (43,20 €)");
        assertThat(en.text()).contains("Paid by: a former member");
    }

    @Test
    void an_unshared_operation_paid_by_the_reader_has_no_share_line() {
        OperationFacts unshared = new OperationFacts("Essence", new BigDecimal("60.00"), TransactionType.EXPENSE, "Transport",
            LocalDate.of(2026, 10, 22), BOB, "bob", List.of());

        RenderedMail fr = RENDERER.render(new TransactionAddedNotification("bob", BOB, "alice", "Chez nous", unshared, FINANCE),
            Locale.FRENCH);

        assertThat(fr.text()).contains("Payée par : vous").doesNotContain("Partagée avec", "Votre part");
    }

    @Test
    void a_change_shows_before_and_after() {
        OperationFacts before = groceries("80.00", "40.00", TransactionType.EXPENSE, ALICE, "alice");

        RenderedMail fr = RENDERER.render(new TransactionUpdatedNotification("bob", BOB, "alice", "Chez nous",
            Involvement.CHANGED, before, groceries(), FINANCE), Locale.FRENCH);

        assertThat(fr.subject()).isEqualTo("alice a modifié « Courses »");
        assertThat(fr.text()).contains(
            "alice a modifié une opération qui vous concerne dans l’espace « Chez nous ».",
            "Montant : 80,00 € → 86,40 €",
            "Votre part : 40,00 € → 43,20 €",
            "Date : 21 octobre 2026",
            "Payée par : alice");
    }

    @Test
    void added_to_an_operation() {
        RenderedMail fr = RENDERER.render(new TransactionUpdatedNotification("bob", BOB, "alice", "Chez nous",
            Involvement.ADDED, groceries(), groceries(), FINANCE), Locale.FRENCH);
        RenderedMail en = RENDERER.render(new TransactionUpdatedNotification("bob", BOB, "alice", "Our place",
            Involvement.ADDED, groceries(), groceries(), FINANCE), Locale.ENGLISH);

        assertThat(fr.subject()).isEqualTo("alice vous a ajouté à « Courses »");
        assertThat(fr.text()).contains("alice vous a ajouté à une opération de l’espace « Chez nous ».", "Votre part : 43,20 €")
            .doesNotContain("→");
        assertThat(en.subject()).isEqualTo("alice added you to “Courses”");
        assertThat(en.text()).contains("alice added you to a transaction in the space “Our place”.");
    }

    @Test
    void removed_from_an_operation_shows_what_it_was() {
        OperationFacts before = new OperationFacts("Courses du marché", new BigDecimal("90.00"), TransactionType.EXPENSE,
            "Alimentation", LocalDate.of(2026, 10, 21), ALICE, "alice",
            List.of(new MemberShare(ALICE, "alice", new BigDecimal("45.00")), new MemberShare(BOB, "bob", new BigDecimal("45.00"))));
        OperationFacts after = new OperationFacts("Courses", new BigDecimal("90.00"), TransactionType.EXPENSE,
            "Alimentation", LocalDate.of(2026, 10, 21), ALICE, "alice", List.of());

        RenderedMail fr = RENDERER.render(new TransactionUpdatedNotification("bob", BOB, "alice", "Chez nous",
            Involvement.REMOVED, before, after, FINANCE), Locale.FRENCH);
        RenderedMail en = RENDERER.render(new TransactionUpdatedNotification("bob", BOB, "alice", "Our place",
            Involvement.REMOVED, before, after, FINANCE), Locale.ENGLISH);

        assertThat(fr.subject()).isEqualTo("alice vous a retiré de « Courses du marché »");
        assertThat(fr.text()).contains("alice vous a retiré d’une opération de l’espace « Chez nous ». Vous n’y participez plus.",
            "Votre part : 45,00 €");
        assertThat(en.subject()).isEqualTo("alice removed you from “Courses du marché”");
        assertThat(en.text()).contains("alice removed you from a transaction in the space “Our place”. You are no longer part of it.");
    }

    @Test
    void a_deleted_operation_says_what_it_was() {
        RenderedMail fr = RENDERER.render(new TransactionDeletedNotification("bob", BOB, "alice", "Chez nous", groceries(), FINANCE),
            Locale.FRENCH);
        RenderedMail en = RENDERER.render(new TransactionDeletedNotification("bob", BOB, "alice", "Our place", groceries(), FINANCE),
            Locale.ENGLISH);

        assertThat(fr.subject()).isEqualTo("alice a supprimé « Courses »");
        assertThat(fr.text()).contains("alice a supprimé une opération qui vous concernait dans l’espace « Chez nous ».",
            "Montant : 86,40 €", "Votre part : 43,20 €");
        assertThat(en.subject()).isEqualTo("alice deleted “Courses”");
        assertThat(en.text()).contains("alice deleted a transaction that involved you in the space “Our place”.");
    }

    @Test
    void a_series_says_its_frequency() {
        RenderedMail monthly = RENDERER.render(new RecurringCreatedNotification("bob", BOB, "alice", "Chez nous",
            rent(RecurrenceInterval.MONTHLY, 1, null), FINANCE), Locale.FRENCH);
        RenderedMail fortnightly = RENDERER.render(new RecurringCreatedNotification("bob", BOB, "alice", "Chez nous",
            rent(RecurrenceInterval.WEEKLY, 2, LocalDate.of(2027, 6, 30)), FINANCE), Locale.FRENCH);
        RenderedMail yearly = RENDERER.render(new RecurringCreatedNotification("bob", BOB, "alice", "Our place",
            rent(RecurrenceInterval.YEARLY, 1, null), FINANCE), Locale.ENGLISH);

        assertThat(monthly.subject()).isEqualTo("alice a créé l’opération récurrente « Loyer »");
        assertThat(monthly.text()).contains(
            "alice a créé une opération récurrente qui vous concerne dans l’espace « Chez nous ».",
            "Montant : 1\u202f200,00 €",
            "Fréquence : Tous les mois",
            "À partir du : 1 novembre 2026",
            "Votre part par échéance : 600,00 €").doesNotContain("Jusqu’au");
        assertThat(fortnightly.text()).contains("Fréquence : Toutes les 2 semaines", "Jusqu’au : 30 juin 2027");
        assertThat(yearly.subject()).isEqualTo("alice created the recurring transaction “Loyer”");
        assertThat(yearly.text()).contains("Frequency: Every year", "Your share per occurrence: €600.00");
    }

    @Test
    void a_changed_series_says_only_the_coming_occurrences_change() {
        SeriesFacts before = rent(RecurrenceInterval.MONTHLY, 1, null);
        SeriesFacts after = rent(RecurrenceInterval.MONTHLY, 1, LocalDate.of(2027, 6, 30));

        RenderedMail fr = RENDERER.render(new RecurringUpdatedNotification("bob", BOB, "alice", "Chez nous",
            Involvement.CHANGED, before, after, FINANCE), Locale.FRENCH);
        RenderedMail removed = RENDERER.render(new RecurringUpdatedNotification("bob", BOB, "alice", "Chez nous",
            Involvement.REMOVED, before, after, FINANCE), Locale.FRENCH);
        RenderedMail en = RENDERER.render(new RecurringUpdatedNotification("bob", BOB, "alice", "Our place",
            Involvement.ADDED, before, after, FINANCE), Locale.ENGLISH);

        assertThat(fr.subject()).isEqualTo("alice a modifié l’opération récurrente « Loyer »");
        assertThat(fr.text()).contains(
            "alice a modifié une opération récurrente qui vous concerne dans l’espace « Chez nous ».",
            "Seules les prochaines échéances changent ; les opérations déjà enregistrées restent telles quelles.",
            "Jusqu’au : jamais → 30 juin 2027");
        assertThat(removed.subject()).isEqualTo("alice vous a retiré de l’opération récurrente « Loyer »");
        assertThat(removed.text()).contains("alice vous a retiré d’une opération récurrente de l’espace « Chez nous ». Vous n’y participez plus.")
            .doesNotContain("Seules les prochaines échéances");
        assertThat(en.subject()).isEqualTo("alice added you to the recurring transaction “Loyer”");
        assertThat(en.text()).contains("alice added you to a recurring transaction in the space “Our place”.",
            "Only the coming occurrences change; the transactions already recorded stay as they are.");
    }

    @Test
    void a_stopped_series_keeps_its_history() {
        RenderedMail fr = RENDERER.render(new RecurringStoppedNotification("bob", BOB, "alice", "Chez nous",
            rent(RecurrenceInterval.MONTHLY, 1, null), FINANCE), Locale.FRENCH);
        RenderedMail en = RENDERER.render(new RecurringStoppedNotification("bob", BOB, "alice", "Our place",
            rent(RecurrenceInterval.MONTHLY, 1, null), FINANCE), Locale.ENGLISH);

        assertThat(fr.subject()).isEqualTo("alice a arrêté l’opération récurrente « Loyer »");
        assertThat(fr.text()).contains("alice a arrêté une opération récurrente qui vous concernait dans l’espace « Chez nous ». "
            + "Les opérations déjà enregistrées restent dans l’historique ; il n’y en aura pas de nouvelles.");
        assertThat(en.subject()).isEqualTo("alice stopped the recurring transaction “Loyer”");
        assertThat(en.text()).contains("alice stopped a recurring transaction that involved you in the space “Our place”. "
            + "The transactions already recorded stay in the history; there will be no new ones.");
    }
}
