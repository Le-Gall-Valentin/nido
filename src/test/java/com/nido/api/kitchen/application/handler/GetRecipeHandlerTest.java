package com.nido.api.kitchen.application.handler;

import com.nido.api.kitchen.domain.model.KitchenException;
import com.nido.api.kitchen.domain.model.Recipe;
import com.nido.api.kitchen.domain.model.RecipeCategory;
import com.nido.api.kitchen.domain.port.out.RecipeRepository;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetRecipeHandlerTest {

    @Mock RecipeRepository recipeRepository;
    private GetRecipeHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID recipeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new GetRecipeHandler(recipeRepository);
    }

    private SpaceMembership viewer(UUID inSpace) {
        return new SpaceMembership(UUID.randomUUID(), inSpace, UUID.randomUUID(), SpaceRole.VIEWER, Instant.now());
    }

    private Recipe recipe(UUID inSpace) {
        return new Recipe(recipeId, inSpace, "Pâtes bolognaise", RecipeCategory.PLAT, 35, 4,
            false, List.of(), List.of(), Instant.now(), Instant.now());
    }

    @Test
    void a_viewer_can_read_a_recipe_in_their_space() {
        Recipe expected = recipe(spaceId);
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(expected));

        assertThat(handler.get(recipeId, viewer(spaceId))).isEqualTo(expected);
    }

    @Test
    void a_recipe_in_another_space_is_not_found() {
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe(UUID.randomUUID())));

        assertThatThrownBy(() -> handler.get(recipeId, viewer(spaceId)))
            .isInstanceOf(KitchenException.RecipeNotFound.class);
    }
}
