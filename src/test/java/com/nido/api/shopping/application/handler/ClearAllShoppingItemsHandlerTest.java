package com.nido.api.shopping.application.handler;

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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ClearAllShoppingItemsHandlerTest {

    @Mock ShoppingItemRepository itemRepository;
    private ClearAllShoppingItemsHandler handler;
    private final UUID spaceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new ClearAllShoppingItemsHandler(itemRepository);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    @Test
    void a_member_can_clear_all_items_in_their_space() {
        handler.clearAll(spaceId, membership(SpaceRole.MEMBER));

        verify(itemRepository).deleteAllBySpaceId(spaceId);
    }

    @Test
    void a_viewer_cannot_clear_all_items() {
        assertThatThrownBy(() -> handler.clearAll(spaceId, membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
        verify(itemRepository, never()).deleteAllBySpaceId(spaceId);
    }
}
