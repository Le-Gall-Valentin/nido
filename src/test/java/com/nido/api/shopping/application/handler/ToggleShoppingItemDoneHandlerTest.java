package com.nido.api.shopping.application.handler;

import com.nido.api.shopping.domain.model.ShoppingException;
import com.nido.api.shopping.domain.model.ShoppingItem;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ToggleShoppingItemDoneHandlerTest {

    @Mock ShoppingItemRepository itemRepository;
    private ToggleShoppingItemDoneHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID itemId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new ToggleShoppingItemDoneHandler(itemRepository);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    private ShoppingItem item(UUID inSpace) {
        return new ShoppingItem(itemId, inSpace, UUID.randomUUID(), "Pâtes", null, false, 0);
    }

    @Test
    void a_member_can_toggle_an_item_in_their_space() {
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item(spaceId)));

        handler.toggle(itemId, spaceId, membership(SpaceRole.MEMBER));

        verify(itemRepository).toggleDone(itemId);
    }

    @Test
    void an_item_from_another_space_is_not_found() {
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item(UUID.randomUUID())));

        assertThatThrownBy(() -> handler.toggle(itemId, spaceId, membership(SpaceRole.MEMBER)))
            .isInstanceOf(ShoppingException.ItemNotFound.class);
    }

    @Test
    void a_viewer_cannot_toggle_an_item() {
        assertThatThrownBy(() -> handler.toggle(itemId, spaceId, membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }
}
