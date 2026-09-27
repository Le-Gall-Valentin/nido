package com.nido.api.dashboard.infrastructure.web.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nido.api.dashboard.domain.model.AgendaCard;
import com.nido.api.dashboard.domain.model.AttentionItem;
import com.nido.api.dashboard.domain.model.CardKind;
import com.nido.api.dashboard.domain.model.CardResult;
import com.nido.api.dashboard.domain.model.Dashboard;
import com.nido.api.dashboard.domain.model.FinanceCard;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardResponseTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 26);

    @Test
    void cardsAreKeyedInLowercaseInCardOrderAndAnInvitationsEntryNeverShows() {
        Dashboard dashboard = new Dashboard(TODAY, SpaceType.SHARED, true, List.of(), Map.of(
            CardKind.FINANCE, new CardResult.Unavailable(),
            CardKind.AGENDA, new CardResult.Ok(new AgendaCard(List.of(), List.of(), List.of(), null)),
            CardKind.INVITATIONS, new CardResult.Unavailable()));

        DashboardResponse response = DashboardResponse.from(dashboard);

        assertThat(response.cards()).containsOnlyKeys("agenda", "finance");
        assertThat(response.cards().keySet()).containsExactly("agenda", "finance");
        assertThat(response.cards().get("finance")).isEqualTo(new CardResultResponse("UNAVAILABLE", null));
        assertThat(response.cards().get("agenda").status()).isEqualTo("OK");
        assertThat(response.cards().get("agenda").data()).isInstanceOf(CardDataResponses.AgendaCardResponse.class);
        // The failed invitations show no card, but the client must still learn the answer is incomplete.
        assertThat(response.complete()).isFalse();
    }

    @Test
    void anUnavailableCardSerializesWithoutAnyDataKey() throws Exception {
        assertThat(new ObjectMapper().writeValueAsString(new CardResultResponse("UNAVAILABLE", null)))
            .isEqualTo("{\"status\":\"UNAVAILABLE\"}");
    }

    @Test
    void attentionItemsCarryTheirKindAndSeverityAsText() {
        UUID invitationId = UUID.randomUUID();
        Dashboard dashboard = new Dashboard(TODAY, SpaceType.SHARED, true, List.of(
            new AttentionItem.OverdueTasks(2, List.of("a", "b")),
            new AttentionItem.Invitation(invitationId, "Coloc Lyon", "🏠", "#c17a5c", SpaceRole.MEMBER, null,
                Instant.parse("2026-10-01T09:00:00Z"))), Map.of());

        List<AttentionItemResponse> attention = DashboardResponse.from(dashboard).attention();

        assertThat(attention).containsExactly(
            new AttentionItemResponse.OverdueTasks("OVERDUE_TASKS", "HIGH", 2, List.of("a", "b")),
            new AttentionItemResponse.Invitation("INVITATION", "INFO", invitationId, "Coloc Lyon", "🏠", "#c17a5c",
                SpaceRole.MEMBER, null, Instant.parse("2026-10-01T09:00:00Z")));
    }

    @Test
    void theFinanceMonthIsPlainYearMonthTextAndPersonalBalancesStayNull() {
        FinanceCard finance = new FinanceCard(YearMonth.of(2026, 9), BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN,
            BigDecimal.ZERO, List.of(), List.of(), null);
        Dashboard dashboard = new Dashboard(TODAY, SpaceType.PERSONAL, false, List.of(),
            Map.of(CardKind.FINANCE, new CardResult.Ok(finance)));

        CardDataResponses.FinanceCardResponse data =
            (CardDataResponses.FinanceCardResponse) DashboardResponse.from(dashboard).cards().get("finance").data();

        assertThat(data.month()).isEqualTo("2026-09");
        assertThat(data.balances()).isNull();
        assertThat(DashboardResponse.from(dashboard).complete()).isTrue();
    }
}
