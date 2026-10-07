package com.nido.api.shopping.infrastructure.persistence.adapter;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.TestSpaces;
import com.nido.api.infrastructure.sealing.SealedValueRejected;
import com.nido.api.infrastructure.sealing.SpaceSealers;
import com.nido.api.shopping.domain.model.ShoppingCategory;
import com.nido.api.shopping.infrastructure.persistence.entity.ShoppingCategoryEntity;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.space.infrastructure.persistence.entity.SpaceEntity;
import com.nido.api.space.infrastructure.persistence.repository.SpaceJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTestConfig
class ShoppingCategoryRepositoryAdapterIT {

    @Autowired ShoppingCategoryRepositoryAdapter adapter;
    @Autowired SpaceJpaRepository spaceJpaRepository;
    @Autowired JdbcTemplate jdbc;
    @Autowired SpaceSealers sealers;

    private UUID spaceId;

    @BeforeEach
    void setUp() {
        SpaceEntity space = new SpaceEntity();
        space.setType(SpaceType.SHARED);
        TestSpaces.name(space, "Chez Valentin");
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

    @Test
    void the_name_is_stored_encrypted_with_the_key_of_its_space_and_renaming_re_encrypts_it() {
        ShoppingCategory created = adapter.create(spaceId, "Fruits & légumes", false);

        adapter.rename(created.id(), "Primeur 🍎");

        String stored = jdbc.queryForObject("SELECT name_encrypted FROM shopping_categories WHERE id = ?", String.class, created.id());
        assertThat(stored).startsWith("v2:").doesNotContain("Primeur");
        assertThat(sealers.forSpace(spaceId).open(ShoppingCategoryEntity.NAME, created.id(), stored)).isEqualTo("Primeur 🍎");
        assertThat(adapter.findById(created.id()).orElseThrow().name()).isEqualTo("Primeur 🍎");
    }

    @Test
    void a_name_copied_from_another_aisle_is_refused() {
        ShoppingCategory fruits = adapter.create(spaceId, "Fruits", false);
        ShoppingCategory bakery = adapter.create(spaceId, "Boulangerie", false);
        jdbc.update("UPDATE shopping_categories SET name_encrypted = (SELECT name_encrypted FROM shopping_categories WHERE id = ?) WHERE id = ?",
            fruits.id(), bakery.id());

        assertThatThrownBy(() -> adapter.findById(bakery.id())).isInstanceOf(SealedValueRejected.class);
    }
}
