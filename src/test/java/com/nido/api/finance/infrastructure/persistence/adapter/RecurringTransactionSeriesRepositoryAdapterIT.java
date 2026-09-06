package com.nido.api.finance.infrastructure.persistence.adapter;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.finance.domain.model.Category;
import com.nido.api.finance.domain.model.Contribution;
import com.nido.api.finance.domain.model.ContributionInput;
import com.nido.api.finance.domain.model.CreateCategoryCommand;
import com.nido.api.finance.domain.model.CreateRecurringSeriesCommand;
import com.nido.api.finance.domain.model.CreateTransactionCommand;
import com.nido.api.finance.domain.model.RecurrenceInterval;
import com.nido.api.finance.domain.model.RecurringTransactionSeries;
import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.finance.domain.model.TransactionType;
import com.nido.api.finance.infrastructure.persistence.repository.FinanceRecurringSeriesJpaRepository;
import com.nido.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.shared.model.Role;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.space.infrastructure.persistence.entity.SpaceEntity;
import com.nido.api.space.infrastructure.persistence.repository.SpaceJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestConfig
class RecurringTransactionSeriesRepositoryAdapterIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired RecurringTransactionSeriesRepositoryAdapter adapter;
    @Autowired TransactionRepositoryAdapter transactionAdapter;
    @Autowired FinanceRecurringSeriesJpaRepository jpaRepository;
    @Autowired SpaceJpaRepository spaceJpaRepository;
    @Autowired UserIdentityJpaRepository userJpaRepository;
    @Autowired CategoryRepositoryAdapter categoryAdapter;

    private UUID spaceId;
    private UUID aliceId;
    private UUID bobId;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        spaceJpaRepository.deleteAll();
        userJpaRepository.deleteAll();

        SpaceEntity space = new SpaceEntity();
        space.setType(SpaceType.SHARED);
        space.setName("Chez Valentin");
        space.setAccent("#c17a5c");
        space.setGlyph("🏡");
        spaceId = spaceJpaRepository.saveAndFlush(space).getId();

        aliceId = saveUser("alice");
        bobId = saveUser("bob");
        // finance_recurring_transaction_series.category_id has a foreign key onto
        // finance_categories(id) — a real category row is required.
        Category category = categoryAdapter.create(new CreateCategoryCommand(spaceId, "Logement", "#f59e0b", "Home", TransactionType.EXPENSE), true);
        categoryId = category.id();
    }

    private UUID saveUser(String username) {
        UserIdentityEntity user = new UserIdentityEntity();
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setRole(Role.USER);
        return userJpaRepository.saveAndFlush(user).getId();
    }

    @Test
    void create_persists_the_series_with_its_contributors_and_encrypts_label_and_amount() {
        // Contributors passed here are already resolved (as CreateRecurringSeriesHandler would
        // do via ContributionSplitter.resolve before calling this adapter) — this test verifies
        // persistence of a resolved list, not the split math itself (see ContributionSplitterTest).
        RecurringTransactionSeries created = adapter.create(new CreateRecurringSeriesCommand(
            spaceId, "Loyer", new BigDecimal("800.00"), TransactionType.EXPENSE, categoryId, aliceId,
            List.of(new ContributionInput(aliceId, null), new ContributionInput(bobId, null)),
            RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 1, 1), null),
            List.of(new Contribution(aliceId, new BigDecimal("400.00")), new Contribution(bobId, new BigDecimal("400.00"))));

        assertThat(created.label()).isEqualTo("Loyer");
        assertThat(created.amount()).isEqualByComparingTo("800.00");
        assertThat(created.contributors()).extracting("memberId").containsExactlyInAnyOrder(aliceId, bobId);
        assertThat(created.lastMaterializedDate()).isNull();
        String rawLabel = jpaRepository.findById(created.id()).orElseThrow().getLabelEncrypted();
        assertThat(rawLabel).doesNotContain("Loyer");
    }

    @Test
    void advanceLastMaterializedDate_updates_only_that_column() {
        RecurringTransactionSeries created = adapter.create(new CreateRecurringSeriesCommand(
            spaceId, "Loyer", new BigDecimal("800.00"), TransactionType.EXPENSE, categoryId, aliceId,
            List.of(), RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 1, 1), null), List.of());

        RecurringTransactionSeries advanced = adapter.advanceLastMaterializedDate(created.id(), LocalDate.of(2026, 1, 1));

        assertThat(advanced.lastMaterializedDate()).isEqualTo(LocalDate.of(2026, 1, 1));
    }

    @Test
    void findActiveBySpaceId_excludes_series_whose_end_date_has_passed() {
        adapter.create(new CreateRecurringSeriesCommand(spaceId, "Actif", new BigDecimal("10.00"), TransactionType.EXPENSE,
            categoryId, null, List.of(), RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 1, 1), null), List.of());
        adapter.create(new CreateRecurringSeriesCommand(spaceId, "Expiré", new BigDecimal("10.00"), TransactionType.EXPENSE,
            categoryId, null, List.of(), RecurrenceInterval.MONTHLY, 1, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 6, 1)), List.of());

        List<RecurringTransactionSeries> active = adapter.findActiveBySpaceId(spaceId, LocalDate.of(2026, 6, 1));

        assertThat(active).extracting(RecurringTransactionSeries::label).containsExactly("Actif");
    }

    @Test
    void delete_removes_the_series() {
        RecurringTransactionSeries created = adapter.create(new CreateRecurringSeriesCommand(
            spaceId, "Loyer", new BigDecimal("800.00"), TransactionType.EXPENSE, categoryId, aliceId,
            List.of(), RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 1, 1), null), List.of());

        adapter.delete(created.id());

        assertThat(adapter.findById(created.id())).isEmpty();
    }

    @Test
    void delete_detaches_its_past_materialized_transactions_instead_of_deleting_them() {
        RecurringTransactionSeries created = adapter.create(new CreateRecurringSeriesCommand(
            spaceId, "Loyer", new BigDecimal("800.00"), TransactionType.EXPENSE, categoryId, aliceId,
            List.of(), RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 1, 1), null), List.of());
        Transaction materialized = transactionAdapter.create(new CreateTransactionCommand(
            spaceId, "Loyer", new BigDecimal("800.00"), TransactionType.EXPENSE, categoryId,
            LocalDate.of(2026, 1, 1), aliceId, List.of(), created.id()), List.of());

        adapter.delete(created.id());

        Transaction survived = transactionAdapter.findById(materialized.id()).orElseThrow();
        assertThat(survived.recurringSeriesId()).isNull();
    }
}
