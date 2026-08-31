package com.nido.api.shopping.application.handler;

import com.nido.api.shopping.domain.model.ShoppingCategory;
import com.nido.api.shopping.domain.port.out.ShoppingCategoryRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateShoppingCategoryHandlerTest {

    @Mock ShoppingCategoryRepository categoryRepository;
    private CreateShoppingCategoryHandler handler;
    private final UUID spaceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new CreateShoppingCategoryHandler(categoryRepository);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    @Test
    void a_member_can_create_a_category() {
        ShoppingCategory created = new ShoppingCategory(UUID.randomUUID(), spaceId, "Épicerie", 0, false);
        when(categoryRepository.create(spaceId, "Épicerie", false)).thenReturn(created);

        ShoppingCategory result = handler.create(spaceId, "Épicerie", membership(SpaceRole.MEMBER));

        assertThat(result).isEqualTo(created);
    }

    @Test
    void a_viewer_cannot_create_a_category() {
        assertThatThrownBy(() -> handler.create(spaceId, "Épicerie", membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }
}
