package com.nido.api.kitchen.application.handler;

import com.nido.api.kitchen.domain.model.Recipe;
import com.nido.api.kitchen.domain.model.RecipeCategory;
import com.nido.api.kitchen.domain.model.RecipeSummaryView;
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
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListRecipesHandlerTest {

    @Mock RecipeRepository recipeRepository;
    @Mock MenuRepository menuRepository;
    private ListRecipesHandler handler;
    private final UUID spaceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new ListRecipesHandler(recipeRepository, menuRepository);
    }

    @Test
    void attaches_the_last_planned_date_when_known() {
        Recipe planned = new Recipe(UUID.randomUUID(), spaceId, "Pâtes bolognaise", RecipeCategory.PLAT, 35, 4,
            false, List.of(), List.of(), Instant.now(), Instant.now());
        Recipe neverPlanned = new Recipe(UUID.randomUUID(), spaceId, "Curry", RecipeCategory.VEGETARIAN, 30, 4,
            false, List.of(), List.of(), Instant.now(), Instant.now());
        when(recipeRepository.findBySpaceId(spaceId)).thenReturn(List.of(planned, neverPlanned));
        when(menuRepository.lastPlannedOnBySpace(spaceId)).thenReturn(Map.of(planned.id(), LocalDate.of(2026, 9, 7)));
        SpaceMembership caller = new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.VIEWER, Instant.now());

        List<RecipeSummaryView> result = handler.list(caller);

        assertThat(result).extracting(RecipeSummaryView::lastPlannedOn)
            .containsExactly(LocalDate.of(2026, 9, 7), null);
    }
}
