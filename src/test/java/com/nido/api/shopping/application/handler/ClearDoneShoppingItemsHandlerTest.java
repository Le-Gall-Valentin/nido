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
class ClearDoneShoppingItemsHandlerTest {

    @Mock ShoppingItemRepository itemRepository;
    private ClearDoneShoppingItemsHandler handler;
    private final UUID spaceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new ClearDoneShoppingItemsHandler(itemRepository);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    @Test
    void a_member_can_clear_done_items_in_their_space() {
        handler.clearDone(spaceId, membership(SpaceRole.MEMBER));

        verify(itemRepository).deleteDoneBySpaceId(spaceId);
    }

    @Test
    void a_viewer_cannot_clear_done_items() {
        assertThatThrownBy(() -> handler.clearDone(spaceId, membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
        verify(itemRepository, never()).deleteDoneBySpaceId(spaceId);
    }
}
