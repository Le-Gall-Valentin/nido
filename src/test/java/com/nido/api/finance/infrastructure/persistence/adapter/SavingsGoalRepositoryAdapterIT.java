package com.nido.api.finance.infrastructure.persistence.adapter;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.finance.domain.model.AddSavingsContributionCommand;
import com.nido.api.finance.domain.model.CreateSavingsGoalCommand;
import com.nido.api.finance.domain.model.SavingsGoal;
import com.nido.api.finance.domain.model.UpdateSavingsGoalCommand;
import com.nido.api.finance.infrastructure.persistence.repository.FinanceSavingsGoalJpaRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestConfig
class SavingsGoalRepositoryAdapterIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired SavingsGoalRepositoryAdapter adapter;
    @Autowired FinanceSavingsGoalJpaRepository jpaRepository;
    @Autowired SpaceJpaRepository spaceJpaRepository;
    @Autowired UserIdentityJpaRepository userJpaRepository;

    private UUID spaceId;
    private UUID aliceId;

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

        UserIdentityEntity user = new UserIdentityEntity();
        user.setUsername("alice");
        user.setEmail("alice@test.com");
        user.setRole(Role.USER);
        aliceId = userJpaRepository.saveAndFlush(user).getId();
    }

    @Test
    void create_persists_the_goal_with_an_encrypted_name_and_target() {
        SavingsGoal created = adapter.create(new CreateSavingsGoalCommand(spaceId, "Vacances d'été", new BigDecimal("2000.00"), LocalDate.of(2026, 7, 1)));

        assertThat(created.name()).isEqualTo("Vacances d'été");
        assertThat(created.targetAmount()).isEqualByComparingTo("2000.00");
        String rawName = jpaRepository.findById(created.id()).orElseThrow().getNameEncrypted();
        assertThat(rawName).doesNotContain("Vacances");
    }

    @Test
    void update_changes_the_name_and_target_amount() {
        SavingsGoal created = adapter.create(new CreateSavingsGoalCommand(spaceId, "Ancien nom", new BigDecimal("1000.00"), null));

        SavingsGoal updated = adapter.update(new UpdateSavingsGoalCommand(created.id(), spaceId, "Nouveau nom", new BigDecimal("1500.00"), LocalDate.of(2026, 12, 1)));

        assertThat(updated.name()).isEqualTo("Nouveau nom");
        assertThat(updated.targetAmount()).isEqualByComparingTo("1500.00");
    }

    @Test
    void addContribution_persists_an_encrypted_contribution_linked_to_the_goal() {
        SavingsGoal created = adapter.create(new CreateSavingsGoalCommand(spaceId, "Vacances", new BigDecimal("2000.00"), null));

        adapter.addContribution(new AddSavingsContributionCommand(created.id(), spaceId, aliceId, new BigDecimal("100.00"), LocalDate.of(2026, 1, 5)));

        assertThat(adapter.findContributionsByGoalId(created.id())).hasSize(1);
        assertThat(adapter.findContributionsByGoalId(created.id()).get(0).amount()).isEqualByComparingTo("100.00");
    }

    @Test
    void findBySpaceId_and_delete_behave_as_expected() {
        SavingsGoal created = adapter.create(new CreateSavingsGoalCommand(spaceId, "Vacances", new BigDecimal("2000.00"), null));

        assertThat(adapter.findBySpaceId(spaceId)).hasSize(1);

        adapter.delete(created.id());

        assertThat(adapter.findById(created.id())).isEmpty();
    }
}
