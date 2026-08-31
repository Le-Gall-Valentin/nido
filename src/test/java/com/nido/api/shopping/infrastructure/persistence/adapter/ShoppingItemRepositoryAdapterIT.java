package com.nido.api.shopping.infrastructure.persistence.adapter;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.shopping.domain.model.AddShoppingItemCommand;
import com.nido.api.shopping.domain.model.ShoppingItem;
import com.nido.api.shopping.domain.model.UpdateShoppingItemCommand;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestConfig
class ShoppingItemRepositoryAdapterIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired ShoppingItemRepositoryAdapter adapter;
    @Autowired ShoppingCategoryRepositoryAdapter categoryAdapter;
    @Autowired SpaceJpaRepository spaceJpaRepository;

    private UUID spaceId;
    private UUID categoryId;
    private UUID otherCategoryId;

    @BeforeEach
    void setUp() {
        SpaceEntity space = new SpaceEntity();
        space.setType(SpaceType.SHARED);
        space.setName("Chez Valentin");
        space.setAccent("#c17a5c");
        space.setGlyph("🏡");
        spaceId = spaceJpaRepository.saveAndFlush(space).getId();
        categoryId = categoryAdapter.create(spaceId, "Épicerie", false).id();
        otherCategoryId = categoryAdapter.create(spaceId, "Maison & divers", true).id();
    }

    @Test
    void add_assigns_incrementing_position_within_the_same_category() {
        ShoppingItem first = adapter.add(new AddShoppingItemCommand(spaceId, categoryId, "Pâtes", "500 g"));
        ShoppingItem second = adapter.add(new AddShoppingItemCommand(spaceId, categoryId, "Riz", null));

        assertThat(first.position()).isZero();
        assertThat(second.position()).isEqualTo(1);
        assertThat(second.quantityLabel()).isNull();
    }

    @Test
    void update_replaces_name_quantity_and_category() {
        ShoppingItem created = adapter.add(new AddShoppingItemCommand(spaceId, categoryId, "Pâtes", "500 g"));

        ShoppingItem updated = adapter.update(new UpdateShoppingItemCommand(created.id(), spaceId, otherCategoryId, "Pâtes complètes", "1 kg"));

        assertThat(updated.categoryId()).isEqualTo(otherCategoryId);
        assertThat(updated.name()).isEqualTo("Pâtes complètes");
        assertThat(updated.quantityLabel()).isEqualTo("1 kg");
    }

    @Test
    void toggleDone_flips_the_flag_each_call() {
        ShoppingItem created = adapter.add(new AddShoppingItemCommand(spaceId, categoryId, "Pâtes", null));

        adapter.toggleDone(created.id());
        assertThat(adapter.findById(created.id())).get().extracting(ShoppingItem::done).isEqualTo(true);

        adapter.toggleDone(created.id());
        assertThat(adapter.findById(created.id())).get().extracting(ShoppingItem::done).isEqualTo(false);
    }

    @Test
    void findBySpaceIdAndDoneFalse_excludes_done_items() {
        ShoppingItem pending = adapter.add(new AddShoppingItemCommand(spaceId, categoryId, "Pâtes", null));
        ShoppingItem done = adapter.add(new AddShoppingItemCommand(spaceId, categoryId, "Riz", null));
        adapter.toggleDone(done.id());

        assertThat(adapter.findBySpaceIdAndDoneFalse(spaceId)).extracting(ShoppingItem::id).containsExactly(pending.id());
    }

    @Test
    void delete_removes_the_item() {
        ShoppingItem created = adapter.add(new AddShoppingItemCommand(spaceId, categoryId, "Pâtes", null));

        adapter.delete(created.id());

        assertThat(adapter.findById(created.id())).isEmpty();
    }

    @Test
    void deleteDoneBySpaceId_only_removes_done_items() {
        ShoppingItem pending = adapter.add(new AddShoppingItemCommand(spaceId, categoryId, "Pâtes", null));
        ShoppingItem done = adapter.add(new AddShoppingItemCommand(spaceId, categoryId, "Riz", null));
        adapter.toggleDone(done.id());

        adapter.deleteDoneBySpaceId(spaceId);

        assertThat(adapter.findBySpaceId(spaceId)).extracting(ShoppingItem::id).containsExactly(pending.id());
    }

    @Test
    void deleteAllBySpaceId_removes_every_item() {
        adapter.add(new AddShoppingItemCommand(spaceId, categoryId, "Pâtes", null));
        adapter.add(new AddShoppingItemCommand(spaceId, categoryId, "Riz", null));

        adapter.deleteAllBySpaceId(spaceId);

        assertThat(adapter.findBySpaceId(spaceId)).isEmpty();
    }

    @Test
    void reassignCategory_moves_every_item_from_one_category_to_another() {
        ShoppingItem itemA = adapter.add(new AddShoppingItemCommand(spaceId, categoryId, "Pâtes", null));
        ShoppingItem itemB = adapter.add(new AddShoppingItemCommand(spaceId, categoryId, "Riz", null));

        adapter.reassignCategory(categoryId, otherCategoryId);

        assertThat(adapter.findById(itemA.id())).get().extracting(ShoppingItem::categoryId).isEqualTo(otherCategoryId);
        assertThat(adapter.findById(itemB.id())).get().extracting(ShoppingItem::categoryId).isEqualTo(otherCategoryId);
    }

    @Test
    void deleting_the_space_cascades_its_items() {
        ShoppingItem created = adapter.add(new AddShoppingItemCommand(spaceId, categoryId, "Pâtes", null));

        spaceJpaRepository.deleteById(spaceId);
        spaceJpaRepository.flush();

        assertThat(adapter.findById(created.id())).isEmpty();
    }
}
