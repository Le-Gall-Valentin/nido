package com.nido.api.kitchen.application.handler;

import com.nido.api.kitchen.domain.model.KitchenException;
import com.nido.api.kitchen.domain.model.Recipe;
import com.nido.api.kitchen.domain.model.RecipeCategory;
import com.nido.api.kitchen.domain.port.out.RecipeRepository;
import com.nido.api.space.domain.model.SpaceException;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteRecipeHandlerTest {

    @Mock RecipeRepository recipeRepository;
    private DeleteRecipeHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID recipeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new DeleteRecipeHandler(recipeRepository);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    private Recipe existing(UUID inSpace) {
        return new Recipe(recipeId, inSpace, "Pâtes bolognaise", RecipeCategory.PLAT, 35, 4,
            false, List.of(), List.of(), Instant.now(), Instant.now());
    }

    @Test
    void a_member_can_delete_a_recipe_in_their_space() {
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(existing(spaceId)));

        handler.delete(recipeId, membership(SpaceRole.MEMBER));

        verify(recipeRepository).delete(recipeId);
    }

    @Test
    void a_missing_recipe_is_not_found() {
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.delete(recipeId, membership(SpaceRole.MEMBER)))
            .isInstanceOf(KitchenException.RecipeNotFound.class);
    }

    @Test
    void a_recipe_in_another_space_is_not_found_even_for_a_viewer() {
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(existing(UUID.randomUUID())));

        assertThatThrownBy(() -> handler.delete(recipeId, membership(SpaceRole.VIEWER)))
            .isInstanceOf(KitchenException.RecipeNotFound.class);
    }

    @Test
    void a_viewer_cannot_delete_a_recipe_in_their_own_space() {
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(existing(spaceId)));

        assertThatThrownBy(() -> handler.delete(recipeId, membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }
}
