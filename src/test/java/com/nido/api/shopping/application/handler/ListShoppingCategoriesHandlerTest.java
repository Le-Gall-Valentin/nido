package com.nido.api.shopping.application.handler;

import com.nido.api.shopping.domain.model.ShoppingCategory;
import com.nido.api.shopping.domain.port.out.ShoppingCategoryRepository;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
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
    void seeds_default_categories_the_first_time_a_space_has_none() {
        when(categoryRepository.existsBySpaceId(spaceId)).thenReturn(false);
        List<ShoppingCategory> seeded = List.of(new ShoppingCategory(UUID.randomUUID(), spaceId, "Maison & divers", 7, true));
        when(categoryRepository.findBySpaceId(spaceId)).thenReturn(seeded);

        List<ShoppingCategory> result = handler.list(membership());

        InOrder order = inOrder(categoryRepository);
        order.verify(categoryRepository, times(7)).create(eq(spaceId), anyString(), eq(false));
        order.verify(categoryRepository).create(spaceId, "Maison & divers", true);
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
