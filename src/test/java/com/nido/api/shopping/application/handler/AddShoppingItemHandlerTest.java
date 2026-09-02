package com.nido.api.shopping.application.handler;

import com.nido.api.shared.model.MeasurementUnit;
import com.nido.api.shopping.domain.model.AddShoppingItemCommand;
import com.nido.api.shopping.domain.model.ShoppingCategory;
import com.nido.api.shopping.domain.model.ShoppingException;
import com.nido.api.shopping.domain.model.ShoppingItem;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddShoppingItemHandlerTest {

    @Mock ShoppingItemRepository itemRepository;
    @Mock ShoppingCategoryRepository categoryRepository;
    private AddShoppingItemHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new AddShoppingItemHandler(itemRepository, categoryRepository);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    @Test
    void a_member_can_add_an_item_to_a_category_in_their_space() {
        AddShoppingItemCommand command = new AddShoppingItemCommand(spaceId, categoryId, "Pâtes", new BigDecimal("500"), MeasurementUnit.GRAM);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(new ShoppingCategory(categoryId, spaceId, "Épicerie", 0, false)));
        ShoppingItem created = new ShoppingItem(UUID.randomUUID(), spaceId, categoryId, "Pâtes", new BigDecimal("500"), MeasurementUnit.GRAM, false, 0);
        when(itemRepository.add(command)).thenReturn(created);

        ShoppingItem result = handler.add(command, membership(SpaceRole.MEMBER));

        assertThat(result).isEqualTo(created);
    }

    @Test
    void a_category_from_another_space_is_not_found() {
        AddShoppingItemCommand command = new AddShoppingItemCommand(spaceId, categoryId, "Pâtes", null, null);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(new ShoppingCategory(categoryId, UUID.randomUUID(), "Épicerie", 0, false)));

        assertThatThrownBy(() -> handler.add(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(ShoppingException.CategoryNotFound.class);
    }

    @Test
    void a_viewer_cannot_add_an_item() {
        AddShoppingItemCommand command = new AddShoppingItemCommand(spaceId, categoryId, "Pâtes", null, null);

        assertThatThrownBy(() -> handler.add(command, membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }
}
