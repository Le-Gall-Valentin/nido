package com.nido.api.dashboard.infrastructure.web.dto;

import com.nido.api.dashboard.domain.model.AgendaCard;
import com.nido.api.dashboard.domain.model.AgendaEvent;
import com.nido.api.dashboard.domain.model.BalanceWithMember;
import com.nido.api.dashboard.domain.model.BudgetWatch;
import com.nido.api.dashboard.domain.model.DashboardCard;
import com.nido.api.dashboard.domain.model.FinanceCard;
import com.nido.api.dashboard.domain.model.MealItem;
import com.nido.api.dashboard.domain.model.MenuCard;
import com.nido.api.dashboard.domain.model.SavingsCard;
import com.nido.api.dashboard.domain.model.SavingsGoalItem;
import com.nido.api.dashboard.domain.model.ShoppingCard;
import com.nido.api.dashboard.domain.model.ShoppingGroup;
import com.nido.api.dashboard.domain.model.TaskItem;
import com.nido.api.dashboard.domain.model.TasksCard;
import com.nido.api.dashboard.domain.model.UpcomingOperation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * The {@code data} of each card, field for field as the client's types declare them. Kept apart from
 * the domain records so that renaming a domain field can never silently change the API.
 */
public final class CardDataResponses {

    private CardDataResponses() {}

    public record AgendaEventResponse(String id, String title, String location, String color, LocalDate startDate,
                                      LocalDate endDate, LocalTime startTime, LocalTime endTime,
                                      List<UUID> participantIds) {}

    public record TaskItemResponse(UUID id, String title, LocalDate dueDate, String priority, String status,
                                   List<UUID> assigneeIds, int subtasksDone, int subtasksTotal, boolean recurring) {}

    public record AgendaCardResponse(List<AgendaEventResponse> allDay, List<AgendaEventResponse> timed,
                                     List<TaskItemResponse> dueToday, AgendaEventResponse tomorrow) {}

    public record MealItemResponse(UUID entryId, UUID recipeId, String recipeName, String category, Integer minutes,
                                   int portions) {}

    public record MenuCardResponse(List<MealItemResponse> today, List<MealItemResponse> tomorrow,
                                   List<LocalDate> unplannedDays) {}

    public record TasksCardResponse(List<TaskItemResponse> overdue, List<TaskItemResponse> thisWeek,
                                    List<TaskItemResponse> inProgress, int openCount, int openCountMine) {}

    public record BudgetWatchResponse(UUID categoryId, String label, String color, BigDecimal spent, BigDecimal limit,
                                      String status) {}

    public record UpcomingOperationResponse(LocalDate date, String label, BigDecimal amount, String type, UUID seriesId) {}

    public record MemberBalanceResponse(UUID memberId, BigDecimal amount, String direction) {}

    /** {@code month} is "yyyy-MM"; {@code balances} is null in a personal space. */
    public record FinanceCardResponse(String month, BigDecimal balance, BigDecimal totalExpense, BigDecimal totalIncome,
                                      BigDecimal remainingBudget, List<BudgetWatchResponse> budgetsToWatch,
                                      List<UpcomingOperationResponse> upcoming, List<MemberBalanceResponse> balances) {}

    public record SavingsGoalItemResponse(UUID goalId, String name, String glyph, String color, BigDecimal target,
                                          BigDecimal contributed, LocalDate targetDate, String state,
                                          BigDecimal monthlyNeeded) {}

    public record SavingsCardResponse(List<SavingsGoalItemResponse> goals) {}

    public record ShoppingGroupResponse(UUID categoryId, String name, int count, List<String> preview) {}

    public record ShoppingCardResponse(int remaining, List<ShoppingGroupResponse> categories) {}

    public static Object from(DashboardCard card) {
        return switch (card) {
            case AgendaCard agenda -> new AgendaCardResponse(events(agenda.allDay()), events(agenda.timed()),
                tasks(agenda.dueToday()), agenda.tomorrow() == null ? null : event(agenda.tomorrow()));
            case MenuCard menu -> new MenuCardResponse(meals(menu.today()), meals(menu.tomorrow()), menu.unplannedDays());
            case TasksCard tasks -> new TasksCardResponse(tasks(tasks.overdue()), tasks(tasks.thisWeek()),
                tasks(tasks.inProgress()), tasks.openCount(), tasks.openCountMine());
            case FinanceCard finance -> new FinanceCardResponse(finance.month().toString(), finance.balance(),
                finance.totalExpense(), finance.totalIncome(), finance.remainingBudget(),
                finance.budgetsToWatch().stream().map(CardDataResponses::budget).toList(),
                finance.upcoming().stream().map(CardDataResponses::upcoming).toList(),
                finance.balances() == null ? null : finance.balances().stream().map(CardDataResponses::balance).toList());
            case SavingsCard savings -> new SavingsCardResponse(savings.goals().stream().map(CardDataResponses::goal).toList());
            case ShoppingCard shopping -> new ShoppingCardResponse(shopping.remaining(),
                shopping.categories().stream().map(CardDataResponses::group).toList());
        };
    }

    private static AgendaEventResponse event(AgendaEvent e) {
        return new AgendaEventResponse(e.id(), e.title(), e.location(), e.color(), e.startDate(), e.endDate(),
            e.startTime(), e.endTime(), e.participantIds());
    }

    private static List<AgendaEventResponse> events(List<AgendaEvent> events) {
        return events.stream().map(CardDataResponses::event).toList();
    }

    private static List<TaskItemResponse> tasks(List<TaskItem> tasks) {
        return tasks.stream().map(t -> new TaskItemResponse(t.id(), t.title(), t.dueDate(), t.priority().name(), t.status().name(),
            t.assigneeIds(), t.subtasksDone(), t.subtasksTotal(), t.recurring())).toList();
    }

    private static List<MealItemResponse> meals(List<MealItem> meals) {
        return meals.stream().map(m -> new MealItemResponse(m.entryId(), m.recipeId(), m.recipeName(),
            m.category() == null ? null : m.category().name(),
            m.minutes(), m.portions())).toList();
    }

    private static BudgetWatchResponse budget(BudgetWatch b) {
        return new BudgetWatchResponse(b.categoryId(), b.label(), b.color(), b.spent(), b.limit(), b.status().name());
    }

    private static UpcomingOperationResponse upcoming(UpcomingOperation u) {
        return new UpcomingOperationResponse(u.date(), u.label(), u.amount(), u.type().name(), u.seriesId());
    }

    private static MemberBalanceResponse balance(BalanceWithMember b) {
        return new MemberBalanceResponse(b.memberId(), b.amount(), b.direction().name());
    }

    private static SavingsGoalItemResponse goal(SavingsGoalItem g) {
        return new SavingsGoalItemResponse(g.goalId(), g.name(), g.glyph(), g.color(), g.target(), g.contributed(),
            g.targetDate(), g.state().name(), g.monthlyNeeded());
    }

    private static ShoppingGroupResponse group(ShoppingGroup g) {
        return new ShoppingGroupResponse(g.categoryId(), g.name(), g.count(), g.preview());
    }
}
