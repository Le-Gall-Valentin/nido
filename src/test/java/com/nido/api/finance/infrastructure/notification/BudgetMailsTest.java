package com.nido.api.finance.infrastructure.notification;

import com.nido.api.finance.domain.model.BudgetAlert;
import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.RenderedMail;
import com.nido.api.mail.infrastructure.render.ThymeleafMailRenderer;
import com.nido.api.notifications.domain.model.NotificationKind;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/** The mails about a category's spending crossing a threshold of its monthly budget. */
class BudgetMailsTest {

    private static final ThymeleafMailRenderer RENDERER = new ThymeleafMailRenderer("https://nido.example");
    private static final AppPath FINANCE = new AppPath("/s/42/finance");

    private static BudgetAlert alert(String spent) {
        return new BudgetAlert("Alimentation", YearMonth.of(2026, 10), new BigDecimal(spent), new BigDecimal("500.00"),
            "Courses", new BigDecimal("35.00"));
    }

    private static String kindOf(Class<?> record) {
        return record.getAnnotation(NotificationKind.class).value();
    }

    @Test
    void each_mail_declares_its_kind_and_template() {
        assertThat(kindOf(BudgetWarningNotification.class)).isEqualTo("finance.budget-warning");
        assertThat(kindOf(BudgetExceededNotification.class)).isEqualTo("finance.budget-exceeded");
        assertThat(new BudgetWarningNotification("u", "a", "s", alert("415.00"), FINANCE).template()).isEqualTo("finance/budget-warning");
        assertThat(new BudgetExceededNotification("u", "a", "s", alert("520.00"), FINANCE).template()).isEqualTo("finance/budget-exceeded");
    }

    @Test
    void a_budget_nearly_used() {
        RenderedMail fr = RENDERER.render(new BudgetWarningNotification("bob", "alice", "Chez nous", alert("415.00"), FINANCE),
            Locale.FRENCH);
        RenderedMail en = RENDERER.render(new BudgetWarningNotification("bob", "alice", "Our place", alert("415.00"), FINANCE),
            Locale.ENGLISH);

        assertThat(fr.subject()).isEqualTo("Budget Alimentation : 83 % atteints");
        assertThat(fr.text()).contains("Bonjour bob,",
            "Dans l’espace « Chez nous », avec l’opération « Courses » (35,00 €) saisie par alice, les dépenses « Alimentation » "
                + "en octobre 2026 atteignent 415,00 € sur un budget de 500,00 €.",
            "Voir les finances\nhttps://nido.example/s/42/finance");
        assertThat(en.subject()).isEqualTo("Alimentation budget: 83% used");
        assertThat(en.text()).contains("In the space “Our place”, with alice’s transaction “Courses” (€35.00), the “Alimentation” "
            + "spending for October 2026 reaches €415.00 of a €500.00 budget.");
    }

    @Test
    void a_budget_exceeded() {
        RenderedMail fr = RENDERER.render(new BudgetExceededNotification("bob", "alice", "Chez nous", alert("520.00"), FINANCE),
            Locale.FRENCH);
        RenderedMail en = RENDERER.render(new BudgetExceededNotification("bob", "alice", "Our place", alert("520.00"), FINANCE),
            Locale.ENGLISH);

        assertThat(fr.subject()).isEqualTo("Budget Alimentation dépassé");
        assertThat(fr.text()).contains("Dans l’espace « Chez nous », avec l’opération « Courses » (35,00 €) saisie par alice, les dépenses "
            + "« Alimentation » en octobre 2026 atteignent 520,00 € : le budget de 500,00 € est dépassé.");
        assertThat(en.subject()).isEqualTo("Alimentation budget exceeded");
        assertThat(en.text()).contains("In the space “Our place”, with alice’s transaction “Courses” (€35.00), the “Alimentation” "
            + "spending for October 2026 reaches €520.00: the €500.00 budget is exceeded.");
    }
}
