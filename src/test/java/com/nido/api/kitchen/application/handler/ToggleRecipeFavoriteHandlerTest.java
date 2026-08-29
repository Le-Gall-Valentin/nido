package com.nido.api.kitchen.application.handler;

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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ToggleRecipeFavoriteHandlerTest {

    @Mock RecipeRepository recipeRepository;
    private ToggleRecipeFavoriteHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID recipeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new ToggleRecipeFavoriteHandler(recipeRepository);
    }

    private Recipe recipe(boolean favorite) {
        return new Recipe(recipeId, spaceId, "Pâtes bolognaise", "Un classique.", RecipeCategory.PLAT, 35, 4,
            favorite, List.of(), List.of(), "Se congèle bien.", Instant.now(), Instant.now());
    }

    @Test
    void flips_a_non_favorite_recipe_to_favorite_reading_the_recipe_only_once() {
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe(false)));
        SpaceMembership caller = new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.MEMBER, Instant.now());

        Recipe result = handler.toggleFavorite(recipeId, caller);

        verify(recipeRepository, times(1)).findById(recipeId);
        verify(recipeRepository).setFavorite(recipeId, true);
        assertThat(result.favorite()).isTrue();
        assertThat(result.description()).isEqualTo("Un classique.");
        assertThat(result.note()).isEqualTo("Se congèle bien.");
    }
}
