package com.nido.api.finance.application.service;

import com.nido.api.finance.domain.model.Budget;
import com.nido.api.finance.domain.model.BudgetAlert;
import com.nido.api.finance.domain.model.BudgetLine;
import com.nido.api.finance.domain.model.BudgetStatus;
import com.nido.api.finance.domain.model.Category;
import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.finance.domain.model.TransactionType;
import com.nido.api.finance.domain.port.out.BudgetRepository;
import com.nido.api.finance.domain.port.out.CategoryRepository;
import com.nido.api.finance.domain.port.out.FinanceNotificationPort;
import com.nido.api.finance.domain.port.out.TransactionRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.port.in.GetSpaceTodayUseCase;
import com.nido.api.space.domain.model.Addressee;
import com.nido.api.space.domain.model.SpaceMembership;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Tells the members of a space when an operation takes a category's spending this month past 80 % of its budget, or
 * past the budget itself — every member but the author, once per crossing: the status is read before the gesture
 * and after it, and only a worse one is told. Nothing remembers an alert once sent, so taking an expense out and
 * putting it back crosses again; rare, and it spares a table.
 *
 * <p>The same figures as the finance page: what {@link BudgetLine} makes of the month's expenses in the category. A
 * past month is not watched — its budget is over and done with — and neither are the occurrences a recurring
 * transaction records by itself, which nobody's gesture writes.
 */
@ApplicationService
public class BudgetWatch {

    /** Where a category of a space stood this month before a gesture. */
    public record Watched(UUID categoryId, YearMonth month, BigDecimal limit, BudgetStatus status) {
    }

    private final TransactionRepository transactions;
    private final BudgetRepository budgets;
    private final CategoryRepository categories;
    private final GetSpaceTodayUseCase spaceToday;
    private final FinanceAudience audience;
    private final FinanceNotificationPort notifications;

    public BudgetWatch(TransactionRepository transactions, BudgetRepository budgets, CategoryRepository categories,
                       GetSpaceTodayUseCase spaceToday, FinanceAudience audience, FinanceNotificationPort notifications) {
        this.transactions = transactions;
        this.budgets = budgets;
        this.categories = categories;
        this.spaceToday = spaceToday;
        this.audience = audience;
        this.notifications = notifications;
    }

    /**
     * Where the category an operation is about to be written in stands, read before writing it — empty when there is
     * nothing to watch: an income, another month than the current one, a category without a budget.
     */
    public Optional<Watched> before(UUID spaceId, TransactionType type, UUID categoryId, LocalDate date) {
        if (type != TransactionType.EXPENSE) {
            return Optional.empty();
        }
        YearMonth month = YearMonth.from(date);
        if (!month.equals(YearMonth.from(spaceToday.today(spaceId)))) {
            return Optional.empty();
        }
        return budgets.findByCategoryId(categoryId)
            .filter(budget -> budget.spaceId().equals(spaceId))
            .map(Budget::monthlyLimit)
            .map(limit -> new Watched(categoryId, month, limit,
                new BudgetLine(categoryId, limit, spent(spaceId, categoryId, month)).status()));
    }

    /** Reads the category again once {@code cause} is written, and tells the others if it crossed a threshold. */
    public void after(SpaceMembership actor, Optional<Watched> watched, Transaction cause) {
        watched.ifPresent(before -> {
            BigDecimal spent = spent(actor.spaceId(), before.categoryId(), before.month());
            BudgetStatus status = new BudgetLine(before.categoryId(), before.limit(), spent).status();
            if (status.isWorseThan(before.status())) {
                audience.of(actor).ifPresent(heard -> {
                    List<Addressee> readers = heard.everyoneButTheActor();
                    if (!readers.isEmpty()) {
                        notifications.budgetCrossed(heard.gesture(), status, new BudgetAlert(categoryLabel(before.categoryId()),
                            before.month(), spent, before.limit(), cause.label(), cause.amount()), readers);
                    }
                });
            }
        });
    }

    /** The month's expenses in the category, as the finance page sums them. */
    private BigDecimal spent(UUID spaceId, UUID categoryId, YearMonth month) {
        return transactions.findBySpaceIdAndMonth(spaceId, month).stream()
            .filter(t -> t.type() == TransactionType.EXPENSE && t.categoryId().equals(categoryId))
            .map(Transaction::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String categoryLabel(UUID categoryId) {
        return categories.findById(categoryId).map(Category::label).orElse(null);
    }
}
