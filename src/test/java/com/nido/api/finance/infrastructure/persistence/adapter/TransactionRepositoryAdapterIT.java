package com.nido.api.finance.infrastructure.persistence.adapter;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.finance.domain.model.Category;
import com.nido.api.finance.domain.model.Contribution;
import com.nido.api.finance.domain.model.ContributionInput;
import com.nido.api.finance.domain.model.CreateCategoryCommand;
import com.nido.api.finance.domain.model.CreateTransactionCommand;
import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.finance.domain.model.TransactionType;
import com.nido.api.finance.domain.model.UpdateTransactionCommand;
import com.nido.api.finance.infrastructure.persistence.repository.FinanceTransactionJpaRepository;
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
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestConfig
class TransactionRepositoryAdapterIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired TransactionRepositoryAdapter adapter;
    @Autowired FinanceTransactionJpaRepository jpaRepository;
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
        // finance_transactions.category_id has a foreign key onto finance_categories(id) —
        // a real category row is required.
        Category category = categoryAdapter.create(new CreateCategoryCommand(spaceId, "Alimentation", "#f59e0b", "Utensils", TransactionType.EXPENSE), true);
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
    void create_persists_the_transaction_with_its_contributors_and_encrypts_label_and_amount() {
        // Contributors passed here are already resolved (as CreateTransactionHandler would do
        // via ContributionSplitter.resolve before calling this adapter) — this test verifies
        // persistence of a resolved list, not the split math itself (see ContributionSplitterTest).
        Transaction created = adapter.create(new CreateTransactionCommand(spaceId, "Courses Carrefour", new BigDecimal("45.30"),
            TransactionType.EXPENSE, categoryId, LocalDate.of(2026, 1, 15), aliceId,
            List.of(new ContributionInput(aliceId, null), new ContributionInput(bobId, null)), null),
            List.of(new Contribution(aliceId, new BigDecimal("22.65")), new Contribution(bobId, new BigDecimal("22.65"))));

        assertThat(created.label()).isEqualTo("Courses Carrefour");
        assertThat(created.amount()).isEqualByComparingTo("45.30");
        assertThat(created.contributors()).extracting("memberId").containsExactlyInAnyOrder(aliceId, bobId);
        String rawLabel = jpaRepository.findById(created.id()).orElseThrow().getLabelEncrypted();
        String rawAmount = jpaRepository.findById(created.id()).orElseThrow().getAmountEncrypted();
        assertThat(rawLabel).doesNotContain("Courses Carrefour");
        assertThat(rawAmount).doesNotContain("45.30");
    }

    @Test
    void update_replaces_the_contributor_list_and_re_encrypts_the_new_values() {
        Transaction created = adapter.create(new CreateTransactionCommand(spaceId, "T", new BigDecimal("10.00"),
            TransactionType.EXPENSE, categoryId, LocalDate.of(2026, 1, 1), aliceId,
            List.of(new ContributionInput(aliceId, null)), null),
            List.of(new Contribution(aliceId, new BigDecimal("10.00"))));

        Transaction updated = adapter.update(new UpdateTransactionCommand(created.id(), spaceId, "T modifié",
            new BigDecimal("20.00"), TransactionType.EXPENSE, categoryId, LocalDate.of(2026, 1, 2), bobId, List.of()), List.of());

        assertThat(updated.label()).isEqualTo("T modifié");
        assertThat(updated.amount()).isEqualByComparingTo("20.00");
        assertThat(updated.payerId()).isEqualTo(bobId);
        assertThat(updated.contributors()).isEmpty();
    }

    @Test
    void findBySpaceIdAndMonth_only_returns_transactions_within_that_month() {
        adapter.create(new CreateTransactionCommand(spaceId, "Janvier", new BigDecimal("10.00"),
            TransactionType.EXPENSE, categoryId, LocalDate.of(2026, 1, 15), null, List.of(), null), List.of());
        adapter.create(new CreateTransactionCommand(spaceId, "Février", new BigDecimal("10.00"),
            TransactionType.EXPENSE, categoryId, LocalDate.of(2026, 2, 1), null, List.of(), null), List.of());

        List<Transaction> januaryTransactions = adapter.findBySpaceIdAndMonth(spaceId, YearMonth.of(2026, 1));

        assertThat(januaryTransactions).extracting(Transaction::label).containsExactly("Janvier");
    }

    @Test
    void findAllBySpaceId_returns_every_transaction_regardless_of_month() {
        adapter.create(new CreateTransactionCommand(spaceId, "Janvier", new BigDecimal("10.00"),
            TransactionType.EXPENSE, categoryId, LocalDate.of(2026, 1, 15), null, List.of(), null), List.of());
        adapter.create(new CreateTransactionCommand(spaceId, "Février", new BigDecimal("10.00"),
            TransactionType.EXPENSE, categoryId, LocalDate.of(2026, 2, 1), null, List.of(), null), List.of());

        assertThat(adapter.findAllBySpaceId(spaceId)).hasSize(2);
    }

    @Test
    void delete_removes_the_transaction() {
        Transaction created = adapter.create(new CreateTransactionCommand(spaceId, "T", new BigDecimal("10.00"),
            TransactionType.EXPENSE, categoryId, LocalDate.of(2026, 1, 1), null, List.of(), null), List.of());

        adapter.delete(created.id());

        assertThat(adapter.findById(created.id())).isEmpty();
    }
}
