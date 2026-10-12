package com.nido.api.finance;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.TestSpaces;
import com.nido.api.finance.application.port.in.GetBalancesUseCase;
import com.nido.api.finance.application.port.in.GetFinanceStatsUseCase;
import com.nido.api.finance.application.port.in.GetProjectionUseCase;
import com.nido.api.finance.domain.model.CreateCategoryCommand;
import com.nido.api.finance.domain.model.TransactionType;
import com.nido.api.finance.infrastructure.persistence.adapter.CategoryRepositoryAdapter;
import com.nido.api.finance.infrastructure.persistence.entity.FinanceSettlementRecordEntity;
import com.nido.api.finance.infrastructure.persistence.entity.FinanceTransactionContributorEntity;
import com.nido.api.finance.infrastructure.persistence.entity.FinanceTransactionEntity;
import com.nido.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.infrastructure.sealing.SpaceSealer;
import com.nido.api.infrastructure.sealing.SpaceSealers;
import com.nido.api.shared.model.Role;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.space.infrastructure.persistence.entity.SpaceEntity;
import com.nido.api.space.infrastructure.persistence.repository.SpaceJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Not a test: a measurement, run by hand with {@code -Dit.test=RunningBalanceBenchmark}; its name keeps surefire and
 * failsafe from picking it up. Seeds twenty years of a household straight into the tables — a hundred operations a
 * month, a third of them shared between two members, a settlement a month — sealed as the adapters seal them, then
 * times the reads a finance page makes. It calls the use cases only, so the same file measures the code before and
 * after the running balance.
 */
@IntegrationTestConfig
class RunningBalanceBenchmark {

    private static final int YEARS = 20;
    private static final int OPERATIONS_PER_MONTH = 100;
    private static final int RUNS = 5;

    @Autowired GetFinanceStatsUseCase stats;
    @Autowired GetProjectionUseCase projection;
    @Autowired GetBalancesUseCase balances;
    @Autowired SpaceJpaRepository spaces;
    @Autowired UserIdentityJpaRepository users;
    @Autowired CategoryRepositoryAdapter categories;
    @Autowired SpaceSealers sealers;
    @Autowired JdbcTemplate jdbc;

    @AfterEach
    void cleanUp() {
        spaces.deleteAll();
        users.deleteAll();
    }

    @Test
    void twenty_years_of_a_household() {
        spaces.deleteAll();
        users.deleteAll();
        UUID alice = user("alice");
        UUID bob = user("bob");
        UUID spaceId = space();
        UUID expenses = categories.create(
            new CreateCategoryCommand(spaceId, "Courses", "#f59e0b", "Utensils", TransactionType.EXPENSE), true).id();
        UUID incomes = categories.create(
            new CreateCategoryCommand(spaceId, "Salaire", "#22c55e", "Wallet", TransactionType.INCOME), true).id();
        int seeded = seed(spaceId, alice, bob, expenses, incomes);
        SpaceMembership caller = new SpaceMembership(UUID.randomUUID(), spaceId, alice, SpaceRole.OWNER, Instant.now());
        YearMonth current = YearMonth.now();

        System.out.printf("%n=== %d opérations sur %d ans ===%n", seeded, YEARS);
        measure("stats, mois en cours", () -> stats.getStats(current, caller));
        measure("stats, il y a 10 ans", () -> stats.getStats(current.minusYears(10), caller));
        measure("stats, dans 3 mois", () -> stats.getStats(current.plusMonths(3), caller));
        measure("projection, mois en cours", () -> projection.getProjection(current, caller));
        measure("remboursements", () -> balances.getBalances(caller));
    }

    private void measure(String label, Runnable read) {
        long first = millis(read);
        long[] next = new long[RUNS];
        for (int i = 0; i < RUNS; i++) {
            next[i] = millis(read);
        }
        Arrays.sort(next);
        System.out.printf("%-28s première : %6d ms   médiane des %d suivantes : %6d ms%n", label, first, RUNS, next[RUNS / 2]);
    }

    private static long millis(Runnable read) {
        long start = System.nanoTime();
        read.run();
        return (System.nanoTime() - start) / 1_000_000;
    }

    private int seed(UUID spaceId, UUID alice, UUID bob, UUID expenses, UUID incomes) {
        SpaceSealer sealer = sealers.forSpace(spaceId);
        Random random = new Random(42);
        List<Object[]> transactions = new ArrayList<>();
        List<Object[]> shares = new ArrayList<>();
        List<Object[]> settlements = new ArrayList<>();
        for (YearMonth month = YearMonth.now().minusYears(YEARS); !month.isAfter(YearMonth.now()); month = month.plusMonths(1)) {
            for (int i = 0; i < OPERATIONS_PER_MONTH; i++) {
                UUID id = UUID.randomUUID();
                boolean income = i == 0;
                BigDecimal amount = income ? new BigDecimal("2500.00") : BigDecimal.valueOf(100 + random.nextInt(9900), 2);
                LocalDate date = month.atDay(1 + random.nextInt(month.lengthOfMonth()));
                boolean shared = !income && i % 3 == 0;
                UUID payer = shared || income ? (random.nextBoolean() ? alice : bob) : null;
                transactions.add(new Object[] {id, spaceId, sealer.seal(FinanceTransactionEntity.LABEL, id, "Opération " + i),
                    sealer.seal(FinanceTransactionEntity.AMOUNT, id, amount.toPlainString()), income ? "INCOME" : "EXPENSE",
                    income ? incomes : expenses, date, payer});
                if (shared) {
                    BigDecimal half = amount.divide(BigDecimal.valueOf(2), 2, RoundingMode.DOWN);
                    shares.add(share(sealer, id, alice, half));
                    shares.add(share(sealer, id, bob, amount.subtract(half)));
                }
            }
            UUID settlementId = UUID.randomUUID();
            settlements.add(new Object[] {settlementId, spaceId, bob, alice,
                sealer.seal(FinanceSettlementRecordEntity.AMOUNT, settlementId, "10.00"), month.atDay(28)});
        }
        jdbc.batchUpdate("""
            INSERT INTO finance_transactions (id, space_id, label_encrypted, amount_encrypted, type, category_id, date, payer_id, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, now())""", transactions);
        jdbc.batchUpdate("""
            INSERT INTO finance_transaction_contributors (id, transaction_id, user_id, share_amount_encrypted)
            VALUES (?, ?, ?, ?)""", shares);
        jdbc.batchUpdate("""
            INSERT INTO finance_settlement_records (id, space_id, from_user_id, to_user_id, amount_encrypted, settled_date)
            VALUES (?, ?, ?, ?, ?, ?)""", settlements);
        return transactions.size();
    }

    private static Object[] share(SpaceSealer sealer, UUID transactionId, UUID memberId, BigDecimal amount) {
        UUID id = UUID.randomUUID();
        return new Object[] {id, transactionId, memberId,
            sealer.seal(FinanceTransactionContributorEntity.SHARE_AMOUNT, id, amount.toPlainString())};
    }

    private UUID space() {
        SpaceEntity space = new SpaceEntity();
        space.setType(SpaceType.SHARED);
        TestSpaces.name(space, "Vingt ans");
        space.setAccent("#c17a5c");
        space.setGlyph("🏡");
        return spaces.saveAndFlush(space).getId();
    }

    private UUID user(String username) {
        UserIdentityEntity user = new UserIdentityEntity();
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setRole(Role.USER);
        return users.saveAndFlush(user).getId();
    }
}
