package com.nido.api.finance.infrastructure.persistence.adapter;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.finance.domain.model.Category;
import com.nido.api.finance.domain.model.CreateCategoryCommand;
import com.nido.api.finance.domain.model.UpdateCategoryCommand;
import com.nido.api.finance.infrastructure.persistence.repository.FinanceCategoryJpaRepository;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestConfig
class CategoryRepositoryAdapterIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired CategoryRepositoryAdapter adapter;
    @Autowired FinanceCategoryJpaRepository jpaRepository;
    @Autowired SpaceJpaRepository spaceJpaRepository;
    @Autowired UserIdentityJpaRepository userJpaRepository;
    @Autowired TransactionRepositoryAdapter transactionAdapter;

    private UUID spaceId;

    @BeforeEach
    void setUp() {
        spaceJpaRepository.deleteAll();
        userJpaRepository.deleteAll();

        UserIdentityEntity user = new UserIdentityEntity();
        user.setUsername("alice");
        user.setEmail("alice@test.com");
        user.setRole(Role.USER);
        userJpaRepository.saveAndFlush(user);

        SpaceEntity space = new SpaceEntity();
        space.setType(SpaceType.SHARED);
        space.setName("Chez Valentin");
        space.setAccent("#c17a5c");
        space.setGlyph("🏡");
        spaceId = spaceJpaRepository.saveAndFlush(space).getId();
    }

    @Test
    void a_default_category_stores_its_label_in_clear_text() {
        Category created = adapter.create(new CreateCategoryCommand(spaceId, "Alimentation", "#f59e0b", "Utensils"), true);

        assertThat(created.label()).isEqualTo("Alimentation");
        assertThat(created.isDefault()).isTrue();
        assertThat(jpaRepository.findById(created.id()).orElseThrow().getLabel()).isEqualTo("Alimentation");
        assertThat(jpaRepository.findById(created.id()).orElseThrow().getLabelEncrypted()).isNull();
    }

    @Test
    void a_custom_category_stores_its_label_encrypted_at_rest() {
        Category created = adapter.create(new CreateCategoryCommand(spaceId, "Ma catégorie perso", "#ec4899", "Star"), false);

        assertThat(created.label()).isEqualTo("Ma catégorie perso");
        assertThat(created.isDefault()).isFalse();
        String rawStoredValue = jpaRepository.findById(created.id()).orElseThrow().getLabelEncrypted();
        assertThat(rawStoredValue).isNotNull().doesNotContain("Ma catégorie perso");
        assertThat(jpaRepository.findById(created.id()).orElseThrow().getLabel()).isNull();
    }

    @Test
    void update_re_encrypts_the_new_label_for_a_custom_category() {
        Category created = adapter.create(new CreateCategoryCommand(spaceId, "Ancien nom", "#ec4899", "Star"), false);

        Category updated = adapter.update(new UpdateCategoryCommand(created.id(), spaceId, "Nouveau nom", "#22c55e", "Heart"));

        assertThat(updated.label()).isEqualTo("Nouveau nom");
        assertThat(updated.color()).isEqualTo("#22c55e");
        assertThat(adapter.findById(created.id()).orElseThrow().label()).isEqualTo("Nouveau nom");
    }

    @Test
    void existsBySpaceId_and_findBySpaceId_reflect_what_was_created() {
        assertThat(adapter.existsBySpaceId(spaceId)).isFalse();

        adapter.create(new CreateCategoryCommand(spaceId, "Alimentation", "#f59e0b", "Utensils"), true);

        assertThat(adapter.existsBySpaceId(spaceId)).isTrue();
        assertThat(adapter.findBySpaceId(spaceId)).hasSize(1);
    }

    @Test
    void isReferencedByTransactions_is_false_when_nothing_references_the_category() {
        Category created = adapter.create(new CreateCategoryCommand(spaceId, "Alimentation", "#f59e0b", "Utensils"), true);

        assertThat(adapter.isReferencedByTransactions(created.id())).isFalse();
    }

    @Test
    void isReferencedByTransactions_is_true_once_a_transaction_uses_the_category() {
        Category created = adapter.create(new CreateCategoryCommand(spaceId, "Alimentation", "#f59e0b", "Utensils"), true);
        transactionAdapter.create(new com.nido.api.finance.domain.model.CreateTransactionCommand(
            spaceId, "Courses", new java.math.BigDecimal("10.00"), com.nido.api.finance.domain.model.TransactionType.EXPENSE,
            created.id(), java.time.LocalDate.of(2026, 1, 1), null, java.util.List.of(), null), java.util.List.of());

        assertThat(adapter.isReferencedByTransactions(created.id())).isTrue();
    }

    @Test
    void delete_removes_the_category() {
        Category created = adapter.create(new CreateCategoryCommand(spaceId, "Alimentation", "#f59e0b", "Utensils"), true);

        adapter.delete(created.id());

        assertThat(adapter.findById(created.id())).isEmpty();
    }
}
