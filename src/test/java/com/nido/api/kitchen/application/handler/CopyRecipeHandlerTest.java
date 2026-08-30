package com.nido.api.kitchen.application.handler;

import com.nido.api.kitchen.domain.model.CreateRecipeCommand;
import com.nido.api.kitchen.domain.model.KitchenException;
import com.nido.api.kitchen.domain.model.MeasurementUnit;
import com.nido.api.kitchen.domain.model.Recipe;
import com.nido.api.kitchen.domain.model.RecipeCategory;
import com.nido.api.kitchen.domain.model.RecipeIngredient;
import com.nido.api.kitchen.domain.port.out.RecipeRepository;
import com.nido.api.space.application.port.in.ResolveMembershipUseCase;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CopyRecipeHandlerTest {

    @Mock RecipeRepository recipeRepository;
    @Mock ResolveMembershipUseCase resolveMembershipUseCase;
    private CopyRecipeHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID destinationSpaceId = UUID.randomUUID();
    private final UUID recipeId = UUID.randomUUID();
    private final UUID callerUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new CopyRecipeHandler(recipeRepository, resolveMembershipUseCase);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, callerUserId, role, Instant.now());
    }

    private SpaceMembership destinationMembership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), destinationSpaceId, callerUserId, role, Instant.now());
    }

    private Recipe recipe(UUID inSpace) {
        return new Recipe(recipeId, inSpace, "Pâtes bolognaise", "Un classique.", RecipeCategory.PLAT, 35, 4,
            true, List.of(new RecipeIngredient("Pâtes", BigDecimal.valueOf(500), MeasurementUnit.GRAM)),
            List.of("Cuire les pâtes."), "Se congèle bien.", Instant.now(), Instant.now());
    }

    @Test
    void a_viewer_can_copy_a_recipe_into_a_space_where_they_can_write() {
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe(spaceId)));
        when(resolveMembershipUseCase.resolve(destinationSpaceId, callerUserId)).thenReturn(destinationMembership(SpaceRole.MEMBER));
        Recipe created = recipe(destinationSpaceId);
        when(recipeRepository.create(new CreateRecipeCommand(destinationSpaceId, "Pâtes bolognaise", "Un classique.",
            RecipeCategory.PLAT, 35, 4, List.of(new RecipeIngredient("Pâtes", BigDecimal.valueOf(500), MeasurementUnit.GRAM)),
            List.of("Cuire les pâtes."), "Se congèle bien."))).thenReturn(created);

        Recipe result = handler.copy(recipeId, destinationSpaceId, membership(SpaceRole.VIEWER));

        assertThat(result).isEqualTo(created);
    }

    @Test
    void a_recipe_from_another_space_is_not_found() {
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe(UUID.randomUUID())));

        assertThatThrownBy(() -> handler.copy(recipeId, destinationSpaceId, membership(SpaceRole.MEMBER)))
            .isInstanceOf(KitchenException.RecipeNotFound.class);
    }

    @Test
    void a_missing_recipe_is_not_found() {
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.copy(recipeId, destinationSpaceId, membership(SpaceRole.MEMBER)))
            .isInstanceOf(KitchenException.RecipeNotFound.class);
    }

    @Test
    void copying_into_the_same_space_is_rejected_before_touching_the_repository() {
        // The same-space check runs on the two ids alone, before any lookup — no
        // findById stub needed here.
        assertThatThrownBy(() -> handler.copy(recipeId, spaceId, membership(SpaceRole.MEMBER)))
            .isInstanceOf(KitchenException.SameSpaceTransfer.class);
    }

    @Test
    void copying_into_a_space_the_caller_does_not_belong_to_is_not_found() {
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe(spaceId)));
        when(resolveMembershipUseCase.resolve(destinationSpaceId, callerUserId)).thenThrow(new SpaceException.NotAMember());

        assertThatThrownBy(() -> handler.copy(recipeId, destinationSpaceId, membership(SpaceRole.MEMBER)))
            .isInstanceOf(SpaceException.NotAMember.class);
    }

    @Test
    void copying_into_a_space_where_the_caller_can_only_view_is_rejected() {
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe(spaceId)));
        when(resolveMembershipUseCase.resolve(destinationSpaceId, callerUserId)).thenReturn(destinationMembership(SpaceRole.VIEWER));

        assertThatThrownBy(() -> handler.copy(recipeId, destinationSpaceId, membership(SpaceRole.MEMBER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }
}
