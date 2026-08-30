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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MoveRecipeHandlerTest {

    @Mock RecipeRepository recipeRepository;
    @Mock ResolveMembershipUseCase resolveMembershipUseCase;
    private MoveRecipeHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID destinationSpaceId = UUID.randomUUID();
    private final UUID recipeId = UUID.randomUUID();
    private final UUID callerUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new MoveRecipeHandler(recipeRepository, resolveMembershipUseCase);
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
    void a_member_can_move_a_recipe_deleting_it_from_the_source() {
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe(spaceId)));
        when(resolveMembershipUseCase.resolve(destinationSpaceId, callerUserId)).thenReturn(destinationMembership(SpaceRole.MEMBER));
        Recipe created = recipe(destinationSpaceId);
        when(recipeRepository.create(new CreateRecipeCommand(destinationSpaceId, "Pâtes bolognaise", "Un classique.",
            RecipeCategory.PLAT, 35, 4, List.of(new RecipeIngredient("Pâtes", BigDecimal.valueOf(500), MeasurementUnit.GRAM)),
            List.of("Cuire les pâtes."), "Se congèle bien."))).thenReturn(created);

        Recipe result = handler.move(recipeId, destinationSpaceId, membership(SpaceRole.MEMBER));

        assertThat(result).isEqualTo(created);
        verify(recipeRepository).delete(recipeId);
    }

    @Test
    void a_viewer_cannot_move_a_recipe() {
        // ensureCanWrite() throws before the recipe is ever fetched — no stub needed.
        assertThatThrownBy(() -> handler.move(recipeId, destinationSpaceId, membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }

    @Test
    void a_recipe_from_another_space_is_not_found() {
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe(UUID.randomUUID())));

        assertThatThrownBy(() -> handler.move(recipeId, destinationSpaceId, membership(SpaceRole.MEMBER)))
            .isInstanceOf(KitchenException.RecipeNotFound.class);
        verify(recipeRepository, never()).delete(recipeId);
    }

    @Test
    void moving_into_the_same_space_is_rejected_before_touching_the_repository() {
        assertThatThrownBy(() -> handler.move(recipeId, spaceId, membership(SpaceRole.MEMBER)))
            .isInstanceOf(KitchenException.SameSpaceTransfer.class);
        verify(recipeRepository, never()).delete(recipeId);
    }

    @Test
    void moving_into_a_space_the_caller_does_not_belong_to_is_not_found() {
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe(spaceId)));
        when(resolveMembershipUseCase.resolve(destinationSpaceId, callerUserId)).thenThrow(new SpaceException.NotAMember());

        assertThatThrownBy(() -> handler.move(recipeId, destinationSpaceId, membership(SpaceRole.MEMBER)))
            .isInstanceOf(SpaceException.NotAMember.class);
        verify(recipeRepository, never()).delete(recipeId);
    }

    @Test
    void moving_into_a_space_where_the_caller_can_only_view_is_rejected() {
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe(spaceId)));
        when(resolveMembershipUseCase.resolve(destinationSpaceId, callerUserId)).thenReturn(destinationMembership(SpaceRole.VIEWER));

        assertThatThrownBy(() -> handler.move(recipeId, destinationSpaceId, membership(SpaceRole.MEMBER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
        verify(recipeRepository, never()).delete(recipeId);
    }
}
