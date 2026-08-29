package com.nido.api.kitchen.application.handler;

import com.nido.api.kitchen.domain.model.AddMenuEntryCommand;
import com.nido.api.kitchen.domain.model.KitchenException;
import com.nido.api.kitchen.domain.model.MenuEntry;
import com.nido.api.kitchen.domain.model.MenuEntryView;
import com.nido.api.kitchen.domain.model.Recipe;
import com.nido.api.kitchen.domain.model.RecipeCategory;
import com.nido.api.kitchen.domain.port.out.MenuRepository;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddMenuEntryHandlerTest {

    @Mock MenuRepository menuRepository;
    @Mock RecipeRepository recipeRepository;
    private AddMenuEntryHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID recipeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new AddMenuEntryHandler(menuRepository, recipeRepository);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    private Recipe recipe(UUID inSpace) {
        return new Recipe(recipeId, inSpace, "Pâtes bolognaise", RecipeCategory.PLAT, 35, 4,
            false, List.of(), List.of(), Instant.now(), Instant.now());
    }

    @Test
    void a_member_can_plan_a_recipe_from_their_own_space() {
        AddMenuEntryCommand command = new AddMenuEntryCommand(spaceId, LocalDate.of(2026, 9, 7), recipeId, 4);
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe(spaceId)));
        MenuEntry entry = new MenuEntry(UUID.randomUUID(), spaceId, LocalDate.of(2026, 9, 7), recipeId, 4, 0);
        when(menuRepository.add(command)).thenReturn(entry);

        MenuEntryView result = handler.add(command, membership(SpaceRole.MEMBER));

        assertThat(result.entry()).isEqualTo(entry);
        assertThat(result.recipe().id()).isEqualTo(recipeId);
    }

    @Test
    void a_recipe_from_another_space_cannot_be_planned() {
        AddMenuEntryCommand command = new AddMenuEntryCommand(spaceId, LocalDate.of(2026, 9, 7), recipeId, 4);
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe(UUID.randomUUID())));

        assertThatThrownBy(() -> handler.add(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(KitchenException.RecipeNotFound.class);
    }

    @Test
    void a_viewer_cannot_plan_a_meal() {
        AddMenuEntryCommand command = new AddMenuEntryCommand(spaceId, LocalDate.of(2026, 9, 7), recipeId, 4);

        assertThatThrownBy(() -> handler.add(command, membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }
}
