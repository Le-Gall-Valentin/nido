package com.nido.api.finance.infrastructure.persistence.adapter;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.TestSpaces;
import com.nido.api.finance.domain.model.AddSavingsContributionCommand;
import com.nido.api.finance.domain.model.CreateSavingsGoalCommand;
import com.nido.api.finance.domain.model.SavingsGoal;
import com.nido.api.finance.domain.model.UpdateSavingsGoalCommand;
import com.nido.api.finance.infrastructure.persistence.entity.FinanceSavingsContributionEntity;
import com.nido.api.finance.infrastructure.persistence.entity.FinanceSavingsGoalEntity;
import com.nido.api.finance.infrastructure.persistence.repository.FinanceSavingsGoalJpaRepository;
import com.nido.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestConfig
class SavingsGoalRepositoryAdapterIT {

    @Autowired SavingsGoalRepositoryAdapter adapter;
    @Autowired FinanceSavingsGoalJpaRepository jpaRepository;
    @Autowired SpaceJpaRepository spaceJpaRepository;
    @Autowired UserIdentityJpaRepository userJpaRepository;
    @Autowired SpaceSealers sealers;
    @Autowired JdbcTemplate jdbc;

    private UUID spaceId;
    private UUID aliceId;

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

        UserIdentityEntity user = new UserIdentityEntity();
        user.setUsername("alice");
        user.setEmail("alice@test.com");
        user.setRole(Role.USER);
        aliceId = userJpaRepository.saveAndFlush(user).getId();
    }

    @Test
    void create_persists_the_goal_with_an_encrypted_name_and_target() {
        SavingsGoal created = adapter.create(new CreateSavingsGoalCommand(spaceId, "Vacances d'été", new BigDecimal("2000.00"), LocalDate.of(2026, 7, 1), "#5c7a58", "🎯"));

        assertThat(created.name()).isEqualTo("Vacances d'été");
        assertThat(created.targetAmount()).isEqualByComparingTo("2000.00");
        String rawName = jpaRepository.findById(created.id()).orElseThrow().getNameEncrypted();
        assertThat(rawName).doesNotContain("Vacances");
    }

    @Test
    void the_name_the_target_and_each_contribution_are_stored_sealed_to_their_rows() {
        SavingsGoal goal = adapter.create(new CreateSavingsGoalCommand(spaceId, "Vacances", new BigDecimal("2000.00"), null, "#5c7a58", "🎯"));
        adapter.addContribution(new AddSavingsContributionCommand(goal.id(), spaceId, aliceId, new BigDecimal("100.00"), LocalDate.of(2026, 1, 5)));
        adapter.addContribution(new AddSavingsContributionCommand(goal.id(), spaceId, aliceId, new BigDecimal("40.00"), LocalDate.of(2026, 2, 5)));
        SpaceSealer sealer = sealers.forSpace(spaceId);

        String name = jdbc.queryForObject("SELECT name_encrypted FROM finance_savings_goals WHERE id = ?", String.class, goal.id());
        String target = jdbc.queryForObject("SELECT target_amount_encrypted FROM finance_savings_goals WHERE id = ?", String.class, goal.id());
        assertThat(sealer.open(FinanceSavingsGoalEntity.NAME, goal.id(), name)).isEqualTo("Vacances");
        assertThat(sealer.open(FinanceSavingsGoalEntity.TARGET_AMOUNT, goal.id(), target)).isEqualTo("2000.00");
        assertThat(jdbc.query("SELECT id, amount_encrypted FROM finance_savings_contributions WHERE goal_id = ?",
                (rs, rowNum) -> sealer.open(FinanceSavingsContributionEntity.AMOUNT, rs.getObject("id", UUID.class),
                    rs.getString("amount_encrypted")), goal.id()))
            .containsExactlyInAnyOrder("100.00", "40.00");
    }

    @Test
    void update_changes_the_name_and_target_amount() {
        SavingsGoal created = adapter.create(new CreateSavingsGoalCommand(spaceId, "Ancien nom", new BigDecimal("1000.00"), null, "#5c7a58", "🎯"));

        SavingsGoal updated = adapter.update(new UpdateSavingsGoalCommand(created.id(), spaceId, "Nouveau nom", new BigDecimal("1500.00"), LocalDate.of(2026, 12, 1), "#c17a5c", "🏖️"));

        assertThat(updated.name()).isEqualTo("Nouveau nom");
        assertThat(updated.targetAmount()).isEqualByComparingTo("1500.00");
        assertThat(updated.color()).isEqualTo("#c17a5c");
        assertThat(updated.glyph()).isEqualTo("🏖️");
    }

    @Test
    void addContribution_persists_an_encrypted_contribution_linked_to_the_goal() {
        SavingsGoal created = adapter.create(new CreateSavingsGoalCommand(spaceId, "Vacances", new BigDecimal("2000.00"), null, "#5c7a58", "🎯"));

        adapter.addContribution(new AddSavingsContributionCommand(created.id(), spaceId, aliceId, new BigDecimal("100.00"), LocalDate.of(2026, 1, 5)));

        assertThat(adapter.findContributionsByGoalId(created.id())).hasSize(1);
        assertThat(adapter.findContributionsByGoalId(created.id()).get(0).amount()).isEqualByComparingTo("100.00");
    }

    @Test
    void findContributionsByGoalId_returns_them_most_recent_first() {
        SavingsGoal created = adapter.create(new CreateSavingsGoalCommand(spaceId, "Vacances", new BigDecimal("2000.00"), null, "#5c7a58", "🎯"));
        adapter.addContribution(new AddSavingsContributionCommand(created.id(), spaceId, aliceId, new BigDecimal("50.00"), LocalDate.of(2026, 1, 1)));
        adapter.addContribution(new AddSavingsContributionCommand(created.id(), spaceId, aliceId, new BigDecimal("75.00"), LocalDate.of(2026, 2, 1)));
        adapter.addContribution(new AddSavingsContributionCommand(created.id(), spaceId, aliceId, new BigDecimal("25.00"), LocalDate.of(2026, 1, 15)));

        assertThat(adapter.findContributionsByGoalId(created.id()))
            .extracting(c -> c.date())
            .containsExactly(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 1));
    }

    @Test
    void findBySpaceId_and_delete_behave_as_expected() {
        SavingsGoal created = adapter.create(new CreateSavingsGoalCommand(spaceId, "Vacances", new BigDecimal("2000.00"), null, "#5c7a58", "🎯"));

        assertThat(adapter.findBySpaceId(spaceId)).hasSize(1);

        adapter.delete(created.id());

        assertThat(adapter.findById(created.id())).isEmpty();
    }

    @Test
    void an_update_and_a_contribution_seal_under_the_space_of_the_goal_whatever_the_command_says() {
        SavingsGoal goal = adapter.create(new CreateSavingsGoalCommand(spaceId, "Vacances", new BigDecimal("2000.00"), null, "#5c7a58", "🎯"));
        UUID elsewhere = anotherSpace();

        adapter.update(new UpdateSavingsGoalCommand(goal.id(), elsewhere, "Vacances d'été", new BigDecimal("2500.00"), null, "#5c7a58", "🎯"));
        adapter.addContribution(new AddSavingsContributionCommand(goal.id(), elsewhere, aliceId, new BigDecimal("50.00"), LocalDate.of(2026, 1, 15)));

        assertThat(adapter.findById(goal.id()).orElseThrow())
            .extracting(SavingsGoal::name, g -> g.targetAmount().toPlainString()).containsExactly("Vacances d'été", "2500.00");
        assertThat(adapter.findContributionsByGoalId(goal.id())).extracting(c -> c.amount().toPlainString()).containsExactly("50.00");
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
