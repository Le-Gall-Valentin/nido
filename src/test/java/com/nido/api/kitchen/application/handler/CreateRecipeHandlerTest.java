package com.nido.api.kitchen.application.handler;

import com.nido.api.kitchen.domain.model.CreateRecipeCommand;
import com.nido.api.kitchen.domain.model.MeasurementUnit;
import com.nido.api.kitchen.domain.model.Recipe;
import com.nido.api.kitchen.domain.model.RecipeCategory;
import com.nido.api.kitchen.domain.model.RecipeIngredient;
import com.nido.api.kitchen.domain.port.out.RecipeRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateRecipeHandlerTest {

    @Mock RecipeRepository recipeRepository;
    private CreateRecipeHandler handler;
    private final UUID spaceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new CreateRecipeHandler(recipeRepository);
    }

    private SpaceMembership membership(SpaceRole role, UUID space) {
        return new SpaceMembership(UUID.randomUUID(), space, UUID.randomUUID(), role, Instant.now());
    }

    private CreateRecipeCommand command() {
        return new CreateRecipeCommand(spaceId, "Pâtes bolognaise", RecipeCategory.PLAT, 35, 4,
            List.of(new RecipeIngredient("Pâtes", BigDecimal.valueOf(500), MeasurementUnit.GRAM)), List.of());
    }

    @Test
    void a_member_can_create_a_recipe() {
        Recipe created = new Recipe(UUID.randomUUID(), spaceId, "Pâtes bolognaise", RecipeCategory.PLAT, 35, 4,
            false, List.of(), List.of(), Instant.now(), Instant.now());
        when(recipeRepository.create(command())).thenReturn(created);

        Recipe result = handler.create(command(), membership(SpaceRole.MEMBER, spaceId));

        assertThat(result).isEqualTo(created);
    }

    @Test
    void a_viewer_cannot_create_a_recipe() {
        assertThatThrownBy(() -> handler.create(command(), membership(SpaceRole.VIEWER, spaceId)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }

    @Test
    void a_command_targeting_another_space_is_rejected() {
        assertThatThrownBy(() -> handler.create(command(), membership(SpaceRole.MEMBER, UUID.randomUUID())))
            .isInstanceOf(SpaceException.NotAMember.class);
    }
}
