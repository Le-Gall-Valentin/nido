package com.nido.api.dashboard.domain.model;

import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardModelTest {

    @Test
    void everyAttentionItemCarriesItsKindAndSeverity() {
        AttentionItem overdue = new AttentionItem.OverdueTasks(2, List.of("a", "b"));
        AttentionItem overrun = new AttentionItem.BudgetOverrun(UUID.randomUUID(), "Restaurants",
            new BigDecimal("212.00"), new BigDecimal("180.00"));
        AttentionItem debt = new AttentionItem.Debt(UUID.randomUUID(), new BigDecimal("42.50"));
        AttentionItem invitation = new AttentionItem.Invitation(UUID.randomUUID(), "Coloc Lyon", "🏠", "#c17a5c",
            SpaceRole.MEMBER, "camille", Instant.parse("2026-10-01T09:00:00Z"));

        assertThat(List.of(overdue, overrun, debt, invitation))
            .extracting(AttentionItem::kind, AttentionItem::severity)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(AttentionKind.OVERDUE_TASKS, Severity.HIGH),
                org.assertj.core.groups.Tuple.tuple(AttentionKind.BUDGET_OVERRUN, Severity.HIGH),
                org.assertj.core.groups.Tuple.tuple(AttentionKind.DEBT, Severity.MEDIUM),
                org.assertj.core.groups.Tuple.tuple(AttentionKind.INVITATION, Severity.INFO));
    }

    @Test
    void invitationsAreTheOnlyKindWithoutACard() {
        assertThat(List.of(CardKind.values()))
            .filteredOn(kind -> !kind.hasCard())
            .containsExactly(CardKind.INVITATIONS);
    }

    @Test
    void aSourceResultKeepsItsOwnCopyOfTheAttentionItems() {
        List<AttentionItem> items = new ArrayList<>();
        items.add(new AttentionItem.Debt(UUID.randomUUID(), BigDecimal.TEN));
        SourceResult result = SourceResult.attentionOnly(items);
        items.clear();

        assertThat(result.card()).isNull();
        assertThat(result.attention()).hasSize(1);
        assertThat(SourceResult.nothing().card()).isNull();
        assertThat(SourceResult.nothing().attention()).isEmpty();
    }

    @Test
    void theContextKnowsTheCallerAndWhetherTheSpaceIsShared() {
        UUID userId = UUID.randomUUID();
        SpaceMembership caller = new SpaceMembership(UUID.randomUUID(), UUID.randomUUID(), userId,
            SpaceRole.MEMBER, Instant.now());

        DashboardContext shared = new DashboardContext(caller, "a@b.c", LocalDate.of(2026, 9, 26), SpaceType.SHARED);
        DashboardContext personal = new DashboardContext(caller, "a@b.c", LocalDate.of(2026, 9, 26), SpaceType.PERSONAL);

        assertThat(shared.callerId()).isEqualTo(userId);
        assertThat(shared.isShared()).isTrue();
        assertThat(personal.isShared()).isFalse();
    }
}
