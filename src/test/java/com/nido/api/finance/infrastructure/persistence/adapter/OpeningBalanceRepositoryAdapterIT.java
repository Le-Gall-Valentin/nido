package com.nido.api.finance.infrastructure.persistence.adapter;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.finance.domain.model.OpeningBalance;
import com.nido.api.finance.infrastructure.persistence.entity.FinanceOpeningBalanceEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestConfig
class OpeningBalanceRepositoryAdapterIT extends RunningTotalsITSupport {

    @Autowired OpeningBalanceRepositoryAdapter adapter;

    @Test
    void saves_finds_replaces_and_removes_the_opening_balance_of_a_space() {
        adapter.save(new OpeningBalance(spaceId, new BigDecimal("1200.00"), LocalDate.of(2026, 9, 30)));
        adapter.save(new OpeningBalance(spaceId, new BigDecimal("-80.25"), LocalDate.of(2026, 10, 1)));

        assertThat(adapter.findBySpaceId(spaceId)).hasValueSatisfying(found -> {
            assertThat(found.amount()).isEqualByComparingTo("-80.25");
            assertThat(found.asOf()).isEqualTo(LocalDate.of(2026, 10, 1));
        });
        assertThat(jdbc.queryForObject("SELECT count(*) FROM finance_opening_balances WHERE space_id = ?", Integer.class, spaceId)).isEqualTo(1);

        adapter.deleteBySpaceId(spaceId);

        assertThat(adapter.findBySpaceId(spaceId)).isEmpty();
    }

    @Test
    void the_amount_is_sealed_to_its_row() {
        adapter.save(new OpeningBalance(spaceId, new BigDecimal("1200.00"), LocalDate.of(2026, 9, 30)));

        Map<String, Object> row = jdbc.queryForMap("SELECT id, amount_encrypted FROM finance_opening_balances WHERE space_id = ?", spaceId);
        assertThat((String) row.get("amount_encrypted")).startsWith("v3:").doesNotContain("1200.00");
        assertThat(sealers.forSpace(spaceId).open(FinanceOpeningBalanceEntity.AMOUNT, (UUID) row.get("id"), (String) row.get("amount_encrypted")))
            .isEqualTo("1200.00");
    }

    @Test
    void two_first_saves_at_the_same_time_both_succeed_and_leave_one_opening_balance() throws Exception {
        // A double click on Save sends two first saves at once: both find nothing, and both would insert.
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<OpeningBalance>> saves = List.of(
                pool.submit(() -> { start.await(); return adapter.save(new OpeningBalance(spaceId, new BigDecimal("100.00"), LocalDate.of(2026, 9, 30))); }),
                pool.submit(() -> { start.await(); return adapter.save(new OpeningBalance(spaceId, new BigDecimal("100.00"), LocalDate.of(2026, 9, 30))); }));
            start.countDown();
            for (Future<OpeningBalance> save : saves) {
                save.get();
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(jdbc.queryForObject("SELECT count(*) FROM finance_opening_balances WHERE space_id = ?", Integer.class, spaceId)).isEqualTo(1);
        assertThat(adapter.findBySpaceId(spaceId)).hasValueSatisfying(found -> assertThat(found.amount()).isEqualByComparingTo("100.00"));
    }
}
