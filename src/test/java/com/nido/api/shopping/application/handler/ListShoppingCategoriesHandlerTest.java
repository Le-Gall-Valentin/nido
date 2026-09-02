package com.nido.api.shopping.application.handler;

import com.nido.api.shopping.domain.model.ShoppingCategory;
import com.nido.api.shopping.domain.port.out.ShoppingCategoryRepository;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListShoppingCategoriesHandlerTest {

    @Mock ShoppingCategoryRepository categoryRepository;
    private ListShoppingCategoriesHandler handler;
    private final UUID spaceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new ListShoppingCategoriesHandler(categoryRepository);
    }

    private SpaceMembership membership() {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.MEMBER, Instant.now());
    }

    @Test
    void seeds_the_exact_default_category_names_in_order_the_first_time_a_space_has_none() {
        when(categoryRepository.existsBySpaceId(spaceId)).thenReturn(false);
        List<ShoppingCategory> seeded = List.of(new ShoppingCategory(UUID.randomUUID(), spaceId, "Maison & divers", 7, true));
        when(categoryRepository.findBySpaceId(spaceId)).thenReturn(seeded);

        List<ShoppingCategory> result = handler.list(membership());

        ArgumentCaptor<String> names = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Boolean> fallbacks = ArgumentCaptor.forClass(Boolean.class);
        verify(categoryRepository, times(8)).create(eq(spaceId), names.capture(), fallbacks.capture());

        List<String> expectedNames = new ArrayList<>(DefaultShoppingCategorySeeder.DEFAULT_CATEGORY_NAMES);
        expectedNames.add(DefaultShoppingCategorySeeder.DEFAULT_FALLBACK_CATEGORY_NAME);
        assertThat(names.getAllValues()).containsExactlyElementsOf(expectedNames);
        assertThat(fallbacks.getAllValues()).containsExactly(false, false, false, false, false, false, false, true);
        assertThat(result).isEqualTo(seeded);
    }

    @Test
    void does_not_reseed_a_space_that_already_has_categories() {
        when(categoryRepository.existsBySpaceId(spaceId)).thenReturn(true);
        when(categoryRepository.findBySpaceId(spaceId)).thenReturn(List.of());

        handler.list(membership());

        verify(categoryRepository, never()).create(any(), any(), anyBoolean());
    }
}
