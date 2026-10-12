package com.nido.api.finance.infrastructure.persistence.adapter;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.finance.application.port.in.DeleteCategoryUseCase;
import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.model.TransactionType;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTestConfig
class CategoryDeletionRaceIT extends RunningTotalsITSupport {

    @Autowired DeleteCategoryUseCase deleteCategory;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    void a_category_deleted_while_an_operation_is_being_saved_in_it_is_refused_rather_than_taking_it_along() throws Exception {
        // The deletion checks that no operation uses the category, then deletes it, and the foreign key takes the
        // operations of a deleted category with it. An operation saved between the two would be lost without a trace,
        // and its month would go on counting it.
        SpaceMembership owner = new SpaceMembership(UUID.randomUUID(), spaceId, aliceId, SpaceRole.OWNER, Instant.now());
        ExecutorService other = Executors.newSingleThreadExecutor();
        try {
            Future<?> deletion = new TransactionTemplate(transactionManager).execute(status -> {
                create("Courses", "30.00", TransactionType.EXPENSE, LocalDate.of(2026, 1, 15), null, List.of());
                // Started while the operation is saved but not committed yet.
                Future<?> started = other.submit(() -> deleteCategory.delete(categoryId, spaceId, owner));
                pause();
                return started;
            });

            assertThatThrownBy(deletion::get).hasCauseInstanceOf(FinanceException.CategoryInUse.class);
        } finally {
            other.shutdownNow();
        }
        assertThat(transactions.findAllBySpaceId(spaceId)).hasSize(1);
        assertNets(totals.findNetByMonth(spaceId), Map.of(YearMonth.of(2026, 1), "-30.00"));
    }

    /** Long enough for the deletion to reach the point where it waits on the uncommitted operation. */
    private static void pause() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
