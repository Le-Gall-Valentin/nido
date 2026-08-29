package com.nido.api.kitchen.application.handler;

import com.nido.api.kitchen.domain.model.MenuEntry;
import com.nido.api.kitchen.domain.model.MenuEntryView;
import com.nido.api.kitchen.domain.model.Recipe;
import com.nido.api.kitchen.domain.model.RecipeCategory;
import com.nido.api.kitchen.domain.port.out.MenuRepository;
import com.nido.api.kitchen.domain.port.out.RecipeRepository;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListMenuEntriesHandlerTest {

    @Mock MenuRepository menuRepository;
    @Mock RecipeRepository recipeRepository;
    private ListMenuEntriesHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID recipeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new ListMenuEntriesHandler(menuRepository, recipeRepository);
    }

    @Test
    void pairs_each_entry_with_its_recipe() {
        LocalDate from = LocalDate.of(2026, 9, 7);
        LocalDate to = LocalDate.of(2026, 9, 13);
        MenuEntry entry = new MenuEntry(UUID.randomUUID(), spaceId, from, recipeId, 4, 0);
        Recipe recipe = new Recipe(recipeId, spaceId, "Pâtes bolognaise", RecipeCategory.PLAT, 35, 4,
            false, List.of(), List.of(), Instant.now(), Instant.now());
        when(menuRepository.findBySpaceIdAndDateRange(spaceId, from, to)).thenReturn(List.of(entry));
        when(recipeRepository.findByIds(List.of(recipeId))).thenReturn(List.of(recipe));
        SpaceMembership caller = new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.VIEWER, Instant.now());

        List<MenuEntryView> result = handler.list(caller, from, to);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).recipe()).isEqualTo(recipe);
    }
}
