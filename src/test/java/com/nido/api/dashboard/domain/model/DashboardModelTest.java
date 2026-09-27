package com.nido.api.dashboard.domain.model;

import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

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
    void everyCardKeepsItsOwnCopyOfItsLists() {
        // What a source hands over must not change under the card afterwards, whoever keeps the list.
        List<UUID> ids = mutable(UUID.randomUUID());
        List<String> names = mutable("Tomates");
        AgendaEvent event = new AgendaEvent("evt-1", "Marché", null, null, LocalDate.of(2026, 9, 27), LocalDate.of(2026, 9, 27),
            LocalTime.of(10, 0), LocalTime.of(11, 0), ids);
        TaskItem task = new TaskItem(UUID.randomUUID(), "Poubelles", null, TaskItem.Priority.MED, TaskItem.Status.TODO,
            ids, 0, 0, false);
        ShoppingGroup group = new ShoppingGroup(UUID.randomUUID(), "Fruits et légumes", 1, names);
        List<AgendaEvent> events = mutable(event);
        List<TaskItem> tasks = mutable(task);
        List<MealItem> meals = mutable(new MealItem(UUID.randomUUID(), null, null, null, null, 4));
        List<LocalDate> days = mutable(LocalDate.of(2026, 9, 28));
        List<BudgetWatch> budgets = mutable(new BudgetWatch(UUID.randomUUID(), "Restaurants", null, BigDecimal.TEN,
            BigDecimal.ONE, BudgetWatch.Status.OVER));
        List<UpcomingOperation> upcoming = mutable(new UpcomingOperation(LocalDate.of(2026, 9, 30), "Loyer",
            BigDecimal.TEN, UpcomingOperation.Type.EXPENSE, UUID.randomUUID()));
        List<BalanceWithMember> balances = mutable(new BalanceWithMember(UUID.randomUUID(), BigDecimal.TEN, BalanceDirection.I_OWE));
        List<SavingsGoalItem> goals = mutable(new SavingsGoalItem(UUID.randomUUID(), "Vacances", "🎯", "#5c7a58",
            BigDecimal.TEN, BigDecimal.ONE, null, GoalState.IN_PROGRESS, null));
        List<ShoppingGroup> groups = mutable(group);

        AgendaCard agenda = new AgendaCard(events, events, tasks, null);
        TasksCard tasksCard = new TasksCard(tasks, tasks, tasks, 1, 1);
        MenuCard menu = new MenuCard(meals, meals, days);
        FinanceCard finance = new FinanceCard(YearMonth.of(2026, 9), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, budgets, upcoming, balances);
        SavingsCard savings = new SavingsCard(goals);
        ShoppingCard shopping = new ShoppingCard(1, groups);
        AttentionItem.OverdueTasks overdue = new AttentionItem.OverdueTasks(1, names);

        Stream.of(ids, names, events, tasks, meals, days, budgets, upcoming, balances, goals, groups).forEach(List::clear);

        assertThat(event.participantIds()).hasSize(1);
        assertThat(task.assigneeIds()).hasSize(1);
        assertThat(group.preview()).hasSize(1);
        assertThat(overdue.titles()).hasSize(1);
        assertThat(List.of(agenda.allDay(), agenda.timed(), agenda.dueToday())).allSatisfy(list -> assertThat(list).hasSize(1));
        assertThat(List.of(tasksCard.overdue(), tasksCard.thisWeek(), tasksCard.inProgress())).allSatisfy(list -> assertThat(list).hasSize(1));
        assertThat(List.of(menu.today(), menu.tomorrow(), menu.unplannedDays())).allSatisfy(list -> assertThat(list).hasSize(1));
        assertThat(List.of(finance.budgetsToWatch(), finance.upcoming(), finance.balances())).allSatisfy(list -> assertThat(list).hasSize(1));
        assertThat(savings.goals()).hasSize(1);
        assertThat(shopping.categories()).hasSize(1);
    }

    @Test
    void aPersonalFinanceCardStillHasNoBalances() {
        FinanceCard personal = new FinanceCard(YearMonth.of(2026, 9), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, List.of(), List.of(), null);

        assertThat(personal.balances()).isNull();
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

    @SafeVarargs
    private static <T> List<T> mutable(T... items) {
        return new ArrayList<>(List.of(items));
    }
}
