package com.nido.api.shopping.infrastructure.persistence.adapter;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.shopping.domain.model.ShoppingCategory;
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
class ShoppingCategoryRepositoryAdapterIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired ShoppingCategoryRepositoryAdapter adapter;
    @Autowired SpaceJpaRepository spaceJpaRepository;

    private UUID spaceId;

    @BeforeEach
    void setUp() {
        SpaceEntity space = new SpaceEntity();
        space.setType(SpaceType.SHARED);
        space.setName("Chez Valentin");
        space.setAccent("#c17a5c");
        space.setGlyph("🏡");
        spaceId = spaceJpaRepository.saveAndFlush(space).getId();
    }

    @Test
    void create_assigns_incrementing_position_within_the_space() {
        ShoppingCategory first = adapter.create(spaceId, "Épicerie", false);
        ShoppingCategory second = adapter.create(spaceId, "Frais", false);

        assertThat(first.position()).isZero();
        assertThat(second.position()).isEqualTo(1);
    }

    @Test
    void findBySpaceId_orders_by_position() {
        adapter.create(spaceId, "Épicerie", false);
        adapter.create(spaceId, "Frais", false);

        assertThat(adapter.findBySpaceId(spaceId)).extracting(ShoppingCategory::name)
            .containsExactly("Épicerie", "Frais");
    }

    @Test
    void existsBySpaceId_is_false_until_a_category_is_created() {
        assertThat(adapter.existsBySpaceId(spaceId)).isFalse();

        adapter.create(spaceId, "Épicerie", false);

        assertThat(adapter.existsBySpaceId(spaceId)).isTrue();
    }

    @Test
    void rename_changes_the_name() {
        ShoppingCategory created = adapter.create(spaceId, "Épicerie", false);

        adapter.rename(created.id(), "Épicerie fine");

        assertThat(adapter.findById(created.id())).get().extracting(ShoppingCategory::name).isEqualTo("Épicerie fine");
    }

    @Test
    void delete_removes_the_category() {
        ShoppingCategory created = adapter.create(spaceId, "Épicerie", false);

        adapter.delete(created.id());

        assertThat(adapter.findById(created.id())).isEmpty();
    }

    @Test
    void deleting_the_space_cascades_its_categories() {
        ShoppingCategory created = adapter.create(spaceId, "Épicerie", false);

        spaceJpaRepository.deleteById(spaceId);
        spaceJpaRepository.flush();

        assertThat(adapter.findById(created.id())).isEmpty();
    }
}
