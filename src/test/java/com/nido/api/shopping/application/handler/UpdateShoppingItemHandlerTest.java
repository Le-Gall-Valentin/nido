package com.nido.api.shopping.application.handler;

import com.nido.api.shopping.domain.model.ShoppingCategory;
import com.nido.api.shopping.domain.model.ShoppingException;
import com.nido.api.shopping.domain.model.ShoppingItem;
import com.nido.api.shopping.domain.model.UpdateShoppingItemCommand;
import com.nido.api.shopping.domain.port.out.ShoppingCategoryRepository;
import com.nido.api.shopping.domain.port.out.ShoppingItemRepository;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateShoppingItemHandlerTest {

    @Mock ShoppingItemRepository itemRepository;
    @Mock ShoppingCategoryRepository categoryRepository;
    private UpdateShoppingItemHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID itemId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new UpdateShoppingItemHandler(itemRepository, categoryRepository);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    private ShoppingItem item(UUID inSpace) {
        return new ShoppingItem(itemId, inSpace, categoryId, "Pâtes", "500 g", false, 0);
    }

    @Test
    void a_member_can_update_an_item_in_their_space() {
        UpdateShoppingItemCommand command = new UpdateShoppingItemCommand(itemId, spaceId, categoryId, "Pâtes complètes", "1 kg");
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item(spaceId)));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(new ShoppingCategory(categoryId, spaceId, "Épicerie", 0, false)));
        ShoppingItem updated = new ShoppingItem(itemId, spaceId, categoryId, "Pâtes complètes", "1 kg", false, 0);
        when(itemRepository.update(command)).thenReturn(updated);

        ShoppingItem result = handler.update(command, membership(SpaceRole.MEMBER));

        assertThat(result).isEqualTo(updated);
    }

    @Test
    void an_item_from_another_space_is_not_found() {
        UpdateShoppingItemCommand command = new UpdateShoppingItemCommand(itemId, spaceId, categoryId, "Pâtes complètes", "1 kg");
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item(UUID.randomUUID())));

        assertThatThrownBy(() -> handler.update(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(ShoppingException.ItemNotFound.class);
    }

    @Test
    void a_target_category_from_another_space_is_not_found() {
        UpdateShoppingItemCommand command = new UpdateShoppingItemCommand(itemId, spaceId, categoryId, "Pâtes complètes", "1 kg");
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item(spaceId)));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(new ShoppingCategory(categoryId, UUID.randomUUID(), "Épicerie", 0, false)));

        assertThatThrownBy(() -> handler.update(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(ShoppingException.CategoryNotFound.class);
    }

    @Test
    void a_viewer_cannot_update_an_item() {
        UpdateShoppingItemCommand command = new UpdateShoppingItemCommand(itemId, spaceId, categoryId, "Pâtes complètes", "1 kg");

        assertThatThrownBy(() -> handler.update(command, membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }
}
