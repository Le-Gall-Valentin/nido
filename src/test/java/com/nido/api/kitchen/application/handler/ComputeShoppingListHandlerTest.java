package com.nido.api.kitchen.application.handler;

import com.nido.api.kitchen.domain.model.MeasurementUnit;
import com.nido.api.kitchen.domain.model.MenuEntry;
import com.nido.api.kitchen.domain.model.Recipe;
import com.nido.api.kitchen.domain.model.RecipeCategory;
import com.nido.api.kitchen.domain.model.RecipeIngredient;
import com.nido.api.kitchen.domain.model.ShoppingListLine;
import com.nido.api.kitchen.domain.port.out.MenuRepository;
import com.nido.api.kitchen.domain.port.out.RecipeRepository;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComputeShoppingListHandlerTest {

    @Mock MenuRepository menuRepository;
    @Mock RecipeRepository recipeRepository;
    private ComputeShoppingListHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID recipeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new ComputeShoppingListHandler(menuRepository, recipeRepository);
    }

    @Test
    void scales_each_entrys_ingredients_by_its_own_portions_before_merging() {
        LocalDate monday = LocalDate.of(2026, 9, 7);
        LocalDate tuesday = LocalDate.of(2026, 9, 8);
        Recipe recipe = new Recipe(recipeId, spaceId, "Pâtes bolognaise", null, RecipeCategory.PLAT, 35, 4,
            false, List.of(new RecipeIngredient("Pâtes", BigDecimal.valueOf(400), MeasurementUnit.GRAM)),
            List.of(), null, Instant.now(), Instant.now());
        MenuEntry mondayEntry = new MenuEntry(UUID.randomUUID(), spaceId, monday, recipeId, 4, 0);
        MenuEntry tuesdayEntry = new MenuEntry(UUID.randomUUID(), spaceId, tuesday, recipeId, 2, 0);
        when(menuRepository.findBySpaceIdAndDateRange(spaceId, monday, tuesday))
            .thenReturn(List.of(mondayEntry, tuesdayEntry));
        when(recipeRepository.findByIds(List.of(recipeId))).thenReturn(List.of(recipe));
        SpaceMembership caller = new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.VIEWER, Instant.now());

        List<ShoppingListLine> result = handler.compute(caller, monday, tuesday);

        assertThat(result).hasSize(1);
        // 400g at 4 portions (unscaled) + 200g at 2 portions (half) = 600g.
        assertThat(result.get(0).quantity()).isEqualByComparingTo("600");
    }
}
