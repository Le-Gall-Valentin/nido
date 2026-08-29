package com.nido.api.kitchen.application.handler;

import com.nido.api.kitchen.domain.model.KitchenException;
import com.nido.api.kitchen.domain.model.Recipe;
import com.nido.api.kitchen.domain.model.RecipeCategory;
import com.nido.api.kitchen.domain.model.UpdateRecipeCommand;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateRecipeHandlerTest {

    @Mock RecipeRepository recipeRepository;
    private UpdateRecipeHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID recipeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new UpdateRecipeHandler(recipeRepository);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    private Recipe existing(UUID inSpace) {
        return new Recipe(recipeId, inSpace, "Pâtes bolognaise", RecipeCategory.PLAT, 35, 4,
            false, List.of(), List.of(), Instant.now(), Instant.now());
    }

    private UpdateRecipeCommand command() {
        return new UpdateRecipeCommand(recipeId, spaceId, "Pâtes bolo maison", RecipeCategory.PLAT, 40, 4,
            List.of(), List.of());
    }

    @Test
    void a_member_can_update_a_recipe_in_their_space() {
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(existing(spaceId)));
        Recipe updated = existing(spaceId);
        when(recipeRepository.update(command())).thenReturn(updated);

        assertThat(handler.update(command(), membership(SpaceRole.MEMBER))).isEqualTo(updated);
    }

    @Test
    void a_viewer_cannot_update_a_recipe() {
        // ensureCanWrite() runs before the recipe is fetched, same order as space's own
        // Command-based write handlers (e.g. ChangeMemberRoleHandler) — no findById stub needed.
        assertThatThrownBy(() -> handler.update(command(), membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }

    @Test
    void a_recipe_belonging_to_another_space_is_not_found() {
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(existing(UUID.randomUUID())));

        assertThatThrownBy(() -> handler.update(command(), membership(SpaceRole.MEMBER)))
            .isInstanceOf(KitchenException.RecipeNotFound.class);
    }
}
