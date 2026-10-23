package com.nido.api.finance.application.service;

import com.nido.api.finance.domain.model.Budget;
import com.nido.api.finance.domain.model.BudgetAlert;
import com.nido.api.finance.domain.model.BudgetStatus;
import com.nido.api.finance.domain.model.Category;
import com.nido.api.finance.domain.model.Gesture;
import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.finance.domain.model.TransactionType;
import com.nido.api.finance.domain.port.out.BudgetRepository;
import com.nido.api.finance.domain.port.out.CategoryRepository;
import com.nido.api.finance.domain.port.out.FinanceNotificationPort;
import com.nido.api.finance.domain.port.out.TransactionRepository;
import com.nido.api.space.application.port.in.GetSpaceTodayUseCase;
import com.nido.api.space.application.port.in.GetSpaceUseCase;
import com.nido.api.space.application.port.in.ListSpaceMembersUseCase;
import com.nido.api.space.domain.model.Addressee;
import com.nido.api.space.domain.model.SpaceDetailView;
import com.nido.api.space.domain.model.SpaceMemberView;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** A category's spending this month crossing 80 % of its budget, or the budget itself, tells the others. */
@ExtendWith(MockitoExtension.class)
class BudgetWatchTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 23);
    private static final YearMonth OCTOBER = YearMonth.of(2026, 10);

    @Mock TransactionRepository transactions;
    @Mock BudgetRepository budgets;
    @Mock CategoryRepository categories;
    @Mock GetSpaceTodayUseCase spaceToday;
    @Mock ListSpaceMembersUseCase members;
    @Mock GetSpaceUseCase spaces;
    @Mock FinanceNotificationPort port;

    private final UUID spaceId = UUID.randomUUID();
    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();
    private final UUID carol = UUID.randomUUID();
    private final UUID groceries = UUID.randomUUID();
    private final UUID transport = UUID.randomUUID();
    private final SpaceMembership aliceActs = new SpaceMembership(UUID.randomUUID(), spaceId, alice, SpaceRole.MEMBER, Instant.now());
    private final Gesture gesture = new Gesture(spaceId, "Chez nous", alice, "alice");
    private final List<Addressee> theOthers = List.of(new Addressee(bob, "bob"), new Addressee(carol, "carol"));

    private BudgetWatch watch;

    @BeforeEach
    void setUp() {
        lenient().when(spaceToday.today(spaceId)).thenReturn(TODAY);
        lenient().when(categories.findById(groceries)).thenReturn(Optional.of(
            new Category(groceries, spaceId, "Alimentation", "#f59e0b", "Utensils", true, TransactionType.EXPENSE)));
        lenient().when(members.list(aliceActs)).thenReturn(List.of(member(alice, "alice"), member(bob, "bob"), member(carol, "carol")));
        lenient().when(spaces.get(spaceId, aliceActs)).thenReturn(new SpaceDetailView(spaceId, SpaceType.SHARED, "Chez nous",
            null, "#c17a5c", "🏡", ZoneId.of("Europe/Paris"), SpaceRole.MEMBER, 3));
        watch = new BudgetWatch(transactions, budgets, categories, spaceToday, new FinanceAudience(members, spaces), port);
    }

    private static SpaceMemberView member(UUID id, String name) {
        return new SpaceMemberView(id, name, name + "@test.local", SpaceRole.MEMBER, Instant.now());
    }

    private void budgetOf(String limit) {
        when(budgets.findByCategoryId(groceries)).thenReturn(Optional.of(new Budget(UUID.randomUUID(), spaceId, groceries,
            new BigDecimal(limit))));
    }

    private Transaction expense(String amount, UUID category) {
        return new Transaction(UUID.randomUUID(), spaceId, "Courses", new BigDecimal(amount), TransactionType.EXPENSE, category,
            TODAY, alice, List.of(), null, Instant.now());
    }

    private List<Transaction> spending(String amount) {
        return List.of(expense(amount, groceries));
    }

    /** What October holds before the gesture, then after it. */
    private void octoberHolds(List<Transaction> before, List<Transaction> after) {
        when(transactions.findBySpaceIdAndMonth(spaceId, OCTOBER)).thenReturn(before, after);
    }

    private Optional<BudgetWatch.Watched> watchedGroceries() {
        return watch.before(spaceId, TransactionType.EXPENSE, groceries, TODAY);
    }

    private BudgetAlert alert(String spent, String limit) {
        return new BudgetAlert("Alimentation", OCTOBER, new BigDecimal(spent), new BigDecimal(limit), "Courses", new BigDecimal("35.00"));
    }

    @Test
    void crossing_eighty_percent_warns_everyone_but_the_author() {
        budgetOf("500.00");
        octoberHolds(spending("380.00"), spending("415.00"));

        watch.after(aliceActs, watchedGroceries(), expense("35.00", groceries));

        verify(port).budgetCrossed(gesture, BudgetStatus.WARNING, alert("415.00", "500.00"), theOthers);
    }

    @Test
    void crossing_the_limit_says_exceeded_even_from_below_eighty_percent() {
        budgetOf("500.00");
        octoberHolds(spending("300.00"), spending("520.00"));

        watch.after(aliceActs, watchedGroceries(), expense("35.00", groceries));

        verify(port).budgetCrossed(gesture, BudgetStatus.OVER, alert("520.00", "500.00"), theOthers);
    }

    @Test
    void from_warning_to_over_says_exceeded() {
        budgetOf("500.00");
        octoberHolds(spending("450.00"), spending("520.00"));

        watch.after(aliceActs, watchedGroceries(), expense("35.00", groceries));

        verify(port).budgetCrossed(gesture, BudgetStatus.OVER, alert("520.00", "500.00"), theOthers);
    }

    @Test
    void staying_above_a_threshold_says_nothing() {
        budgetOf("500.00");
        octoberHolds(spending("415.00"), spending("450.00"));

        watch.after(aliceActs, watchedGroceries(), expense("35.00", groceries));

        verify(port, never()).budgetCrossed(any(), any(), any(), any());
    }

    @Test
    void a_lower_spending_says_nothing() {
        budgetOf("500.00");
        octoberHolds(spending("450.00"), spending("300.00"));

        watch.after(aliceActs, watchedGroceries(), expense("35.00", groceries));

        verifyNoInteractions(port);
    }

    @Test
    void only_this_category_s_expenses_count() {
        budgetOf("500.00");
        List<Transaction> after = new ArrayList<>(spending("380.00"));
        after.add(expense("200.00", transport));
        after.add(new Transaction(UUID.randomUUID(), spaceId, "Remboursement", new BigDecimal("300.00"), TransactionType.INCOME,
            groceries, TODAY, alice, List.of(), null, Instant.now()));
        octoberHolds(spending("380.00"), after);

        watch.after(aliceActs, watchedGroceries(), expense("35.00", groceries));

        verifyNoInteractions(port);
    }

    @Test
    void an_expense_of_another_month_is_not_watched() {
        assertThat(watch.before(spaceId, TransactionType.EXPENSE, groceries, LocalDate.of(2026, 9, 30))).isEmpty();
        verifyNoInteractions(transactions, budgets);
    }

    @Test
    void an_income_is_not_watched() {
        assertThat(watch.before(spaceId, TransactionType.INCOME, groceries, TODAY)).isEmpty();
        verifyNoInteractions(transactions, budgets);
    }

    @Test
    void a_category_without_budget_is_not_watched() {
        when(budgets.findByCategoryId(groceries)).thenReturn(Optional.empty());

        assertThat(watchedGroceries()).isEmpty();
        verifyNoInteractions(transactions);
    }

    @Test
    void a_budget_of_another_space_is_not_watched() {
        when(budgets.findByCategoryId(groceries)).thenReturn(Optional.of(new Budget(UUID.randomUUID(), UUID.randomUUID(),
            groceries, new BigDecimal("500.00"))));

        assertThat(watchedGroceries()).isEmpty();
    }

    @Test
    void nothing_watched_tells_nobody() {
        watch.after(aliceActs, Optional.empty(), expense("35.00", groceries));

        verifyNoInteractions(transactions, port, members);
    }

    @Test
    void a_zero_budget_is_exceeded_by_the_first_cent() {
        budgetOf("0.00");
        octoberHolds(List.of(), spending("0.01"));

        watch.after(aliceActs, watchedGroceries(), expense("0.01", groceries));

        verify(port).budgetCrossed(gesture, BudgetStatus.OVER,
            new BudgetAlert("Alimentation", OCTOBER, new BigDecimal("0.01"), new BigDecimal("0.00"), "Courses", new BigDecimal("0.01")),
            theOthers);
    }
}
