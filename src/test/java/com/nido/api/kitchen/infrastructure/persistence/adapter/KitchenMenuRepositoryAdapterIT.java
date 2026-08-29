package com.nido.api.kitchen.infrastructure.persistence.adapter;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.kitchen.domain.model.AddMenuEntryCommand;
import com.nido.api.kitchen.domain.model.MenuEntry;
import com.nido.api.kitchen.domain.model.RecipeCategory;
import com.nido.api.kitchen.infrastructure.persistence.entity.RecipeEntity;
import com.nido.api.kitchen.infrastructure.persistence.repository.RecipeJpaRepository;
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

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestConfig
class KitchenMenuRepositoryAdapterIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired KitchenMenuRepositoryAdapter adapter;
    @Autowired SpaceJpaRepository spaceJpaRepository;
    @Autowired RecipeJpaRepository recipeJpaRepository;

    private UUID spaceId;
    private UUID recipeId;

    @BeforeEach
    void setUp() {
        SpaceEntity space = new SpaceEntity();
        space.setType(SpaceType.SHARED);
        space.setName("Chez Valentin");
        space.setAccent("#c17a5c");
        space.setGlyph("🏡");
        spaceId = spaceJpaRepository.saveAndFlush(space).getId();

        RecipeEntity recipe = new RecipeEntity();
        recipe.setSpaceId(spaceId);
        recipe.setName("Pâtes bolognaise");
        recipe.setCategory(RecipeCategory.PLAT);
        recipe.setMinutes(35);
        recipe.setReferencePortions(4);
        recipe.setFavorite(false);
        recipeId = recipeJpaRepository.saveAndFlush(recipe).getId();
    }

    @Test
    void add_assigns_incrementing_position_within_the_same_date() {
        LocalDate monday = LocalDate.of(2026, 9, 7);

        MenuEntry first = adapter.add(new AddMenuEntryCommand(spaceId, monday, recipeId, 4));
        MenuEntry second = adapter.add(new AddMenuEntryCommand(spaceId, monday, recipeId, 2));

        assertThat(first.position()).isZero();
        assertThat(second.position()).isEqualTo(1);
    }

    @Test
    void add_never_assigns_the_same_position_twice_for_concurrent_inserts() throws Exception {
        LocalDate monday = LocalDate.of(2026, 9, 7);
        int concurrentAdds = 5;
        CyclicBarrier barrier = new CyclicBarrier(concurrentAdds);
        ExecutorService pool = Executors.newFixedThreadPool(concurrentAdds);
        try {
            List<Future<MenuEntry>> futures = List.of(
                pool.submit(() -> addAfterBarrier(barrier, spaceId, monday)),
                pool.submit(() -> addAfterBarrier(barrier, spaceId, monday)),
                pool.submit(() -> addAfterBarrier(barrier, spaceId, monday)),
                pool.submit(() -> addAfterBarrier(barrier, spaceId, monday)),
                pool.submit(() -> addAfterBarrier(barrier, spaceId, monday)));

            Set<Integer> positions = new HashSet<>();
            for (Future<MenuEntry> future : futures) {
                positions.add(future.get().position());
            }

            assertThat(positions).hasSize(concurrentAdds).containsExactlyInAnyOrder(0, 1, 2, 3, 4);
        } finally {
            pool.shutdown();
        }
    }

    private MenuEntry addAfterBarrier(CyclicBarrier barrier, UUID spaceId, LocalDate date) throws Exception {
        barrier.await();
        return adapter.add(new AddMenuEntryCommand(spaceId, date, recipeId, 4));
    }

    @Test
    void findBySpaceIdAndDateRange_only_returns_entries_in_range() {
        adapter.add(new AddMenuEntryCommand(spaceId, LocalDate.of(2026, 9, 7), recipeId, 4));
        adapter.add(new AddMenuEntryCommand(spaceId, LocalDate.of(2026, 9, 20), recipeId, 4));

        List<MenuEntry> found = adapter.findBySpaceIdAndDateRange(
            spaceId, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 14));

        assertThat(found).hasSize(1);
        assertThat(found.get(0).date()).isEqualTo(LocalDate.of(2026, 9, 7));
    }

    @Test
    void updatePortions_changes_the_portions() {
        MenuEntry entry = adapter.add(new AddMenuEntryCommand(spaceId, LocalDate.of(2026, 9, 7), recipeId, 4));

        adapter.updatePortions(entry.id(), 6);

        assertThat(adapter.findById(entry.id())).get().extracting(MenuEntry::portions).isEqualTo(6);
    }

    @Test
    void remove_deletes_the_entry() {
        MenuEntry entry = adapter.add(new AddMenuEntryCommand(spaceId, LocalDate.of(2026, 9, 7), recipeId, 4));

        adapter.remove(entry.id());

        assertThat(adapter.findById(entry.id())).isEmpty();
    }

    @Test
    void lastPlannedOnBySpace_returns_the_most_recent_date_per_recipe() {
        adapter.add(new AddMenuEntryCommand(spaceId, LocalDate.of(2026, 9, 7), recipeId, 4));
        adapter.add(new AddMenuEntryCommand(spaceId, LocalDate.of(2026, 9, 21), recipeId, 4));

        Map<UUID, LocalDate> lastPlanned = adapter.lastPlannedOnBySpace(spaceId);

        assertThat(lastPlanned).containsEntry(recipeId, LocalDate.of(2026, 9, 21));
    }

    @Test
    void deleting_the_recipe_cascades_its_menu_entries() {
        MenuEntry entry = adapter.add(new AddMenuEntryCommand(spaceId, LocalDate.of(2026, 9, 7), recipeId, 4));

        recipeJpaRepository.deleteById(recipeId);
        recipeJpaRepository.flush();

        assertThat(adapter.findById(entry.id())).isEmpty();
    }
}
