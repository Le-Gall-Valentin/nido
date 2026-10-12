package com.nido.api.finance.infrastructure.persistence.adapter;

import com.nido.api.TestSpaces;
import com.nido.api.finance.domain.model.Contribution;
import com.nido.api.finance.domain.model.ContributionInput;
import com.nido.api.finance.domain.model.CreateCategoryCommand;
import com.nido.api.finance.domain.model.CreateTransactionCommand;
import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.finance.domain.model.TransactionType;
import com.nido.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.infrastructure.sealing.SpaceSealers;
import com.nido.api.shared.model.Role;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.space.infrastructure.persistence.entity.SpaceEntity;
import com.nido.api.space.infrastructure.persistence.repository.SpaceJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A shared space with two members and an expense category, and the writes the month-totals tests make in it. Each
 * subclass carries {@code @IntegrationTestConfig} itself: the annotation is not inherited.
 */
abstract class RunningTotalsITSupport {

    @Autowired RunningTotalsRepositoryAdapter totals;
    @Autowired MonthTotalsWriter writer;
    @Autowired TransactionRepositoryAdapter transactions;
    @Autowired SettlementRecordRepositoryAdapter settlements;
    @Autowired CategoryRepositoryAdapter categories;
    @Autowired SpaceJpaRepository spaceJpa;
    @Autowired UserIdentityJpaRepository userJpa;
    @Autowired SpaceSealers sealers;
    @Autowired JdbcTemplate jdbc;

    UUID spaceId;
    UUID aliceId;
    UUID bobId;
    UUID categoryId;

    @BeforeEach
    void setUpSpace() {
        spaceJpa.deleteAll();
        userJpa.deleteAll();
        spaceId = space("Chez nous");
        aliceId = user("alice");
        bobId = user("bob");
        categoryId = categories.create(new CreateCategoryCommand(spaceId, "Courses", "#f59e0b", "Utensils", TransactionType.EXPENSE), true).id();
    }

    Transaction create(String label, String amount, TransactionType type, LocalDate date, UUID payerId, List<Contribution> shares) {
        return transactions.create(new CreateTransactionCommand(spaceId, label, new BigDecimal(amount), type, categoryId, date, payerId,
            shares.stream().map(c -> new ContributionInput(c.memberId(), c.shareAmount())).toList(), null), shares);
    }

    static List<Contribution> half(UUID first, UUID second, String total) {
        BigDecimal half = new BigDecimal(total).divide(BigDecimal.valueOf(2));
        return List.of(new Contribution(first, half), new Contribution(second, half));
    }

    static <K> void assertNets(Map<K, BigDecimal> actual, Map<K, String> expected) {
        assertThat(actual).containsOnlyKeys(expected.keySet());
        expected.forEach((key, value) -> assertThat(actual.get(key)).as(String.valueOf(key)).isEqualByComparingTo(value));
    }

    UUID space(String name) {
        SpaceEntity space = new SpaceEntity();
        space.setType(SpaceType.SHARED);
        TestSpaces.name(space, name);
        space.setAccent("#c17a5c");
        space.setGlyph("🏡");
        return spaceJpa.saveAndFlush(space).getId();
    }

    UUID user(String username) {
        UserIdentityEntity user = new UserIdentityEntity();
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setRole(Role.USER);
        return userJpa.saveAndFlush(user).getId();
    }
}
