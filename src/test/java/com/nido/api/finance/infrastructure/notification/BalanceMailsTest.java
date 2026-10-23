package com.nido.api.finance.infrastructure.notification;

import com.nido.api.finance.domain.model.SettlementAuthor;
import com.nido.api.finance.domain.model.Standing;
import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.RenderedMail;
import com.nido.api.mail.infrastructure.render.ThymeleafMailRenderer;
import com.nido.api.notifications.domain.model.NotificationKind;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/** The mails about the balances of a space: a repayment between two members, and the opening balance. */
class BalanceMailsTest {

    private static final ThymeleafMailRenderer RENDERER = new ThymeleafMailRenderer("https://nido.example");
    private static final AppPath FINANCE = new AppPath("/s/42/finance");
    private static final LocalDate DAY = LocalDate.of(2026, 10, 21);

    private static SettlementRecordedNotification repayment(SettlementAuthor author, Standing standing, String remaining,
                                                            String space) {
        return new SettlementRecordedNotification("alice", "bob", space, author, new BigDecimal("50.00"), DAY, standing,
            new BigDecimal(remaining), FINANCE);
    }

    private static String kindOf(Class<?> record) {
        return record.getAnnotation(NotificationKind.class).value();
    }

    @Test
    void each_mail_declares_its_kind_and_template() {
        assertThat(kindOf(SettlementRecordedNotification.class)).isEqualTo("finance.settlement-recorded");
        assertThat(kindOf(OpeningBalanceChangedNotification.class)).isEqualTo("finance.opening-balance-changed");
        assertThat(repayment(SettlementAuthor.DEBTOR, Standing.EVEN, "0", "s").template()).isEqualTo("finance/settlement-recorded");
        assertThat(new OpeningBalanceChangedNotification("u", "a", "s", true, null, null, FINANCE).template())
            .isEqualTo("finance/opening-balance-changed");
    }

    @Test
    void a_debtor_paying_back_tells_their_creditor() {
        RenderedMail fr = RENDERER.render(repayment(SettlementAuthor.DEBTOR, Standing.EVEN, "0", "Chez nous"), Locale.FRENCH);
        RenderedMail en = RENDERER.render(repayment(SettlementAuthor.DEBTOR, Standing.EVEN, "0", "Our place"), Locale.ENGLISH);

        assertThat(fr.subject()).isEqualTo("bob vous a remboursé 50,00 €");
        assertThat(fr.text()).contains("Bonjour alice,", "bob vous a remboursé 50,00 € le 21 octobre 2026, dans l’espace « Chez nous ».",
            "Vous êtes quittes.", "Voir les finances\nhttps://nido.example/s/42/finance");
        assertThat(en.subject()).isEqualTo("bob paid you back €50.00");
        assertThat(en.text()).contains("bob paid you back €50.00 on 21 October 2026, in the space “Our place”.", "You are even.");
    }

    @Test
    void a_creditor_recording_the_repayment_tells_the_debtor() {
        RenderedMail fr = RENDERER.render(repayment(SettlementAuthor.CREDITOR, Standing.EVEN, "0", "Chez nous"), Locale.FRENCH);
        RenderedMail en = RENDERER.render(repayment(SettlementAuthor.CREDITOR, Standing.EVEN, "0", "Our place"), Locale.ENGLISH);

        assertThat(fr.subject()).isEqualTo("bob a enregistré votre remboursement de 50,00 €");
        assertThat(fr.text()).contains("bob a noté que vous lui avez remboursé 50,00 € le 21 octobre 2026, dans l’espace « Chez nous ».");
        assertThat(en.subject()).isEqualTo("bob recorded your repayment of €50.00");
        assertThat(en.text()).contains("bob noted that you paid them back €50.00 on 21 October 2026, in the space “Our place”.");
    }

    @Test
    void what_is_left_is_told_from_the_reader_s_side() {
        RenderedMail youOwe = RENDERER.render(repayment(SettlementAuthor.CREDITOR, Standing.YOU_OWE, "20.00", "Chez nous"), Locale.FRENCH);
        RenderedMail owedToYou = RENDERER.render(repayment(SettlementAuthor.DEBTOR, Standing.OWED_TO_YOU, "20.00", "Chez nous"), Locale.FRENCH);
        RenderedMail english = RENDERER.render(repayment(SettlementAuthor.CREDITOR, Standing.YOU_OWE, "20.00", "Our place"), Locale.ENGLISH);
        RenderedMail englishOwed = RENDERER.render(repayment(SettlementAuthor.DEBTOR, Standing.OWED_TO_YOU, "20.00", "Our place"), Locale.ENGLISH);

        assertThat(youOwe.text()).contains("Vous devez encore 20,00 € à bob.").doesNotContain("quittes");
        assertThat(owedToYou.text()).contains("bob vous doit encore 20,00 €.");
        assertThat(english.text()).contains("You still owe bob €20.00.");
        assertThat(englishOwed.text()).contains("bob still owes you €20.00.");
    }

    @Test
    void an_opening_balance_set() {
        OpeningBalanceChangedNotification set = new OpeningBalanceChangedNotification("alice", "bob", "Chez nous", false,
            new BigDecimal("1520.30"), LocalDate.of(2026, 10, 1), FINANCE);

        RenderedMail fr = RENDERER.render(set, Locale.FRENCH);
        RenderedMail en = RENDERER.render(set, Locale.ENGLISH);

        assertThat(fr.subject()).isEqualTo("bob a fixé le solde de départ de Chez nous");
        assertThat(fr.text()).contains("bob a fixé le solde de départ de l’espace « Chez nous » : 1 520,30 € au 1 octobre 2026. "
            + "Tous les soldes de l’espace en partent.");
        assertThat(en.subject()).isEqualTo("bob set the opening balance of Chez nous");
        assertThat(en.text()).contains("bob set the opening balance of the space “Chez nous”: €1,520.30 on 1 October 2026. "
            + "Every balance of the space starts from it.");
    }

    @Test
    void an_opening_balance_removed() {
        OpeningBalanceChangedNotification cleared = new OpeningBalanceChangedNotification("alice", "bob", "Chez nous", true,
            null, null, FINANCE);

        RenderedMail fr = RENDERER.render(cleared, Locale.FRENCH);
        RenderedMail en = RENDERER.render(cleared, Locale.ENGLISH);

        assertThat(fr.subject()).isEqualTo("bob a retiré le solde de départ de Chez nous");
        assertThat(fr.text()).contains("bob a retiré le solde de départ de l’espace « Chez nous ». "
            + "Les soldes de l’espace sont de nouveau calculés à partir de zéro, avec les seules opérations.");
        assertThat(en.subject()).isEqualTo("bob removed the opening balance of Chez nous");
        assertThat(en.text()).contains("bob removed the opening balance of the space “Chez nous”. "
            + "The space’s balances are computed from zero again, from its transactions alone.");
    }
}
