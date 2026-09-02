package com.nido.api.shopping.application.handler;

import com.nido.api.shopping.domain.model.ShoppingCategory;
import com.nido.api.shopping.domain.model.ShoppingException;
import com.nido.api.shopping.domain.port.out.ShoppingCategoryRepository;
import com.nido.api.shopping.domain.port.out.ShoppingItemRepository;
import com.nido.api.space.domain.model.SpaceException;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteShoppingCategoryHandlerTest {

    @Mock ShoppingCategoryRepository categoryRepository;
    @Mock ShoppingItemRepository itemRepository;
    private DeleteShoppingCategoryHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();
    private final UUID fallbackId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new DeleteShoppingCategoryHandler(categoryRepository, itemRepository);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    @Test
    void deleting_a_category_reassigns_its_items_to_the_fallback_category_first() {
        ShoppingCategory target = new ShoppingCategory(categoryId, spaceId, "Épicerie", 0, false);
        ShoppingCategory fallback = new ShoppingCategory(fallbackId, spaceId, "Maison & divers", 1, true);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(target));
        when(categoryRepository.findBySpaceId(spaceId)).thenReturn(List.of(target, fallback));

        handler.delete(categoryId, spaceId, membership(SpaceRole.MEMBER));

        InOrder order = inOrder(itemRepository, categoryRepository);
        order.verify(itemRepository).reassignCategory(categoryId, fallbackId);
        order.verify(categoryRepository).delete(categoryId);
    }

    @Test
    void the_fallback_category_itself_cannot_be_deleted() {
        ShoppingCategory fallback = new ShoppingCategory(categoryId, spaceId, "Maison & divers", 0, true);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(fallback));

        assertThatThrownBy(() -> handler.delete(categoryId, spaceId, membership(SpaceRole.MEMBER)))
            .isInstanceOf(ShoppingException.CannotDeleteFallbackCategory.class);
        verify(categoryRepository, never()).delete(any());
        verify(itemRepository, never()).reassignCategory(any(), any());
    }

    @Test
    void a_category_from_another_space_is_not_found() {
        ShoppingCategory target = new ShoppingCategory(categoryId, UUID.randomUUID(), "Épicerie", 0, false);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> handler.delete(categoryId, spaceId, membership(SpaceRole.MEMBER)))
            .isInstanceOf(ShoppingException.CategoryNotFound.class);
    }

    @Test
    void a_viewer_cannot_delete_a_category() {
        assertThatThrownBy(() -> handler.delete(categoryId, spaceId, membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }
}
