package com.nido.api.finance.infrastructure.persistence.adapter;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.TestSpaces;
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
import com.nido.api.finance.domain.model.UpdateRecurringSeriesCommand;
import com.nido.api.finance.infrastructure.persistence.entity.FinanceRecurringSeriesContributorEntity;
import com.nido.api.finance.infrastructure.persistence.entity.FinanceRecurringSeriesEntity;
import com.nido.api.finance.infrastructure.persistence.repository.FinanceRecurringSeriesJpaRepository;
import com.nido.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.infrastructure.sealing.SealedValueRejected;
import com.nido.api.infrastructure.sealing.SpaceSealer;
import com.nido.api.infrastructure.sealing.SpaceSealers;
import com.nido.api.shared.model.Role;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.space.infrastructure.persistence.entity.SpaceEntity;
import com.nido.api.space.infrastructure.persistence.repository.SpaceJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTestConfig
class RecurringTransactionSeriesRepositoryAdapterIT {

    @Autowired RecurringTransactionSeriesRepositoryAdapter adapter;
    @Autowired TransactionRepositoryAdapter transactionAdapter;
    @Autowired FinanceRecurringSeriesJpaRepository jpaRepository;
    @Autowired SpaceJpaRepository spaceJpaRepository;
    @Autowired UserIdentityJpaRepository userJpaRepository;
    @Autowired CategoryRepositoryAdapter categoryAdapter;
    @Autowired SpaceSealers sealers;
    @Autowired JdbcTemplate jdbc;

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
        TestSpaces.name(space, "Chez Valentin");
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

    private RecurringTransactionSeries rentSharedByAliceAndBob() {
        return adapter.create(new CreateRecurringSeriesCommand(
            spaceId, "Loyer", new BigDecimal("800.00"), TransactionType.EXPENSE, categoryId, aliceId,
            List.of(new ContributionInput(aliceId, null), new ContributionInput(bobId, null)),
            RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 1, 1), null),
            List.of(new Contribution(aliceId, new BigDecimal("400.00")), new Contribution(bobId, new BigDecimal("400.00"))));
    }

    @Test
    void the_label_the_amount_and_the_shares_are_stored_sealed_to_their_rows() {
        RecurringTransactionSeries rent = rentSharedByAliceAndBob();
        SpaceSealer sealer = sealers.forSpace(spaceId);

        String label = jdbc.queryForObject("SELECT label_encrypted FROM finance_recurring_transaction_series WHERE id = ?",
            String.class, rent.id());
        String amount = jdbc.queryForObject("SELECT amount_encrypted FROM finance_recurring_transaction_series WHERE id = ?",
            String.class, rent.id());
        assertThat(sealer.open(FinanceRecurringSeriesEntity.LABEL, rent.id(), label)).isEqualTo("Loyer");
        assertThat(sealer.open(FinanceRecurringSeriesEntity.AMOUNT, rent.id(), amount)).isEqualTo("800.00");
        assertThat(jdbc.query("SELECT id, share_amount_encrypted FROM finance_recurring_series_contributors WHERE series_id = ?",
                (rs, rowNum) -> sealer.open(FinanceRecurringSeriesContributorEntity.SHARE_AMOUNT, rs.getObject("id", UUID.class),
                    rs.getString("share_amount_encrypted")), rent.id()))
            .containsExactly("400.00", "400.00");
    }

    @Test
    void a_share_copied_from_another_contributor_is_refused_even_when_it_is_the_same_amount() {
        RecurringTransactionSeries rent = rentSharedByAliceAndBob();
        jdbc.update("""
            UPDATE finance_recurring_series_contributors SET share_amount_encrypted =
              (SELECT share_amount_encrypted FROM finance_recurring_series_contributors WHERE series_id = ? AND user_id = ?)
            WHERE series_id = ? AND user_id = ?""", rent.id(), aliceId, rent.id(), bobId);

        assertThatThrownBy(() -> adapter.findById(rent.id())).isInstanceOf(SealedValueRejected.class);
    }

    @Test
    void create_persists_a_yearly_recurring_series() {
        // chk_finance_recurring_series_interval_type originally only allowed
        // DAILY/WEEKLY/MONTHLY even though RecurrenceInterval always had YEARLY too.
        RecurringTransactionSeries created = adapter.create(new CreateRecurringSeriesCommand(
            spaceId, "VPS OVH", new BigDecimal("55.01"), TransactionType.EXPENSE, categoryId, aliceId,
            List.of(), RecurrenceInterval.YEARLY, 1, LocalDate.of(2025, 12, 13), null), List.of());

        assertThat(created.intervalType()).isEqualTo(RecurrenceInterval.YEARLY);
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

    @Test
    void an_updated_series_reads_back_its_new_label_and_amount() {
        RecurringTransactionSeries created = adapter.create(new CreateRecurringSeriesCommand(
            spaceId, "Loyer", new BigDecimal("800.00"), TransactionType.EXPENSE, categoryId, aliceId,
            List.of(new ContributionInput(aliceId, null)), RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 1, 1), null),
            List.of(new Contribution(aliceId, new BigDecimal("800.00"))));

        adapter.update(new UpdateRecurringSeriesCommand(created.id(), spaceId, "Loyer 2027", new BigDecimal("825.00"),
            TransactionType.EXPENSE, categoryId, aliceId, List.of(new ContributionInput(aliceId, null)),
            RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 1, 1), null),
            List.of(new Contribution(aliceId, new BigDecimal("825.00"))));

        RecurringTransactionSeries reread = adapter.findById(created.id()).orElseThrow();
        assertThat(reread.label()).isEqualTo("Loyer 2027");
        assertThat(reread.amount()).isEqualByComparingTo("825.00");
        assertThat(reread.contributors()).extracting(Contribution::shareAmount).containsExactly(new BigDecimal("825.00"));
    }

    @Test
    void an_update_seals_under_the_space_of_the_series_whatever_the_command_says() {
        RecurringTransactionSeries created = adapter.create(new CreateRecurringSeriesCommand(
            spaceId, "Loyer", new BigDecimal("800.00"), TransactionType.EXPENSE, categoryId, aliceId,
            List.of(new ContributionInput(aliceId, null)), RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 1, 1), null),
            List.of(new Contribution(aliceId, new BigDecimal("800.00"))));

        adapter.update(new UpdateRecurringSeriesCommand(created.id(), anotherSpace(), "Loyer 2027", new BigDecimal("825.00"),
            TransactionType.EXPENSE, categoryId, aliceId, List.of(new ContributionInput(aliceId, null)),
            RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 1, 1), null),
            List.of(new Contribution(aliceId, new BigDecimal("825.00"))));

        assertThat(adapter.findById(created.id()).orElseThrow())
            .extracting(RecurringTransactionSeries::label, s -> s.amount().toPlainString()).containsExactly("Loyer 2027", "825.00");
    }

    private UUID anotherSpace() {
        SpaceEntity other = new SpaceEntity();
        other.setType(SpaceType.SHARED);
        TestSpaces.name(other, "Autre groupe");
        other.setAccent("#c17a5c");
        other.setGlyph("🏡");
        return spaceJpaRepository.saveAndFlush(other).getId();
    }
}
