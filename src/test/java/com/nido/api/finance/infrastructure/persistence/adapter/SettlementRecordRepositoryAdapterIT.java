package com.nido.api.finance.infrastructure.persistence.adapter;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.finance.domain.model.CreateSettlementCommand;
import com.nido.api.finance.domain.model.SettlementRecord;
import com.nido.api.finance.infrastructure.persistence.repository.FinanceSettlementRecordJpaRepository;
import com.nido.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.shared.model.Role;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.space.infrastructure.persistence.entity.SpaceEntity;
import com.nido.api.space.infrastructure.persistence.repository.SpaceJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestConfig
class SettlementRecordRepositoryAdapterIT {

    @Autowired SettlementRecordRepositoryAdapter adapter;
    @Autowired FinanceSettlementRecordJpaRepository jpaRepository;
    @Autowired SpaceJpaRepository spaceJpaRepository;
    @Autowired UserIdentityJpaRepository userJpaRepository;

    private UUID spaceId;
    private UUID aliceId;
    private UUID bobId;

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
    }

    private UUID saveUser(String username) {
        UserIdentityEntity user = new UserIdentityEntity();
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setRole(Role.USER);
        return userJpaRepository.saveAndFlush(user).getId();
    }

    @Test
    void create_persists_the_settlement_with_an_encrypted_amount() {
        SettlementRecord created = adapter.create(new CreateSettlementCommand(spaceId, bobId, aliceId, new BigDecimal("20.00"), LocalDate.of(2026, 1, 2)));

        assertThat(created.amount()).isEqualByComparingTo("20.00");
        assertThat(created.fromMemberId()).isEqualTo(bobId);
        assertThat(created.toMemberId()).isEqualTo(aliceId);
        String rawAmount = jpaRepository.findById(created.id()).orElseThrow().getAmountEncrypted();
        assertThat(rawAmount).doesNotContain("20.00");
    }

    @Test
    void findBySpaceId_returns_every_settlement_in_the_space() {
        adapter.create(new CreateSettlementCommand(spaceId, bobId, aliceId, new BigDecimal("20.00"), LocalDate.of(2026, 1, 2)));
        adapter.create(new CreateSettlementCommand(spaceId, aliceId, bobId, new BigDecimal("5.00"), LocalDate.of(2026, 1, 3)));

        List<SettlementRecord> found = adapter.findBySpaceId(spaceId);

        assertThat(found).hasSize(2);
    }
    // ─── B7 : le filtre par paire est passé du Java au SQL ─────────────────

    @Test
    void findBetweenMembers_returns_both_directions_newest_first_and_nothing_else() {
        // The handler used to load every settlement in the space — decrypting each amount — and
        // filter in Java. Moving that to SQL moved the behaviour with it, so it is verified here
        // against real SQL rather than against a mock that would agree with anything.
        UUID carolId = saveUser("carol");
        adapter.create(new CreateSettlementCommand(spaceId, bobId, aliceId, new BigDecimal("20.00"), LocalDate.of(2026, 1, 5)));
        adapter.create(new CreateSettlementCommand(spaceId, aliceId, bobId, new BigDecimal("30.00"), LocalDate.of(2026, 2, 1)));
        adapter.create(new CreateSettlementCommand(spaceId, aliceId, carolId, new BigDecimal("50.00"), LocalDate.of(2026, 3, 1)));

        List<SettlementRecord> between = adapter.findBetweenMembers(spaceId, aliceId, bobId);

        assertThat(between)
            .as("both directions, newest first, and the settlement with a third member left out")
            .extracting(SettlementRecord::amount)
            .containsExactly(new BigDecimal("30.00"), new BigDecimal("20.00"));
    }

    @Test
    void findBetweenMembers_does_not_care_which_way_round_the_pair_is_given() {
        adapter.create(new CreateSettlementCommand(spaceId, bobId, aliceId, new BigDecimal("20.00"), LocalDate.of(2026, 1, 5)));

        assertThat(adapter.findBetweenMembers(spaceId, aliceId, bobId))
            .hasSameSizeAs(adapter.findBetweenMembers(spaceId, bobId, aliceId))
            .hasSize(1);
    }

    @Test
    void findBetweenMembers_ignores_an_identical_pair_in_another_space() {
        SpaceEntity other = new SpaceEntity();
        other.setType(SpaceType.SHARED);
        other.setName("Ailleurs");
        other.setAccent("#c17a5c");
        other.setGlyph("🏠");
        UUID otherSpaceId = spaceJpaRepository.saveAndFlush(other).getId();
        adapter.create(new CreateSettlementCommand(otherSpaceId, bobId, aliceId, new BigDecimal("99.00"), LocalDate.of(2026, 1, 5)));

        assertThat(adapter.findBetweenMembers(spaceId, aliceId, bobId)).isEmpty();
    }
}
