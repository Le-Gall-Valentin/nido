package com.nido.api.shopping.application.handler;

import com.nido.api.shopping.domain.model.RenameShoppingCategoryCommand;
import com.nido.api.shopping.domain.model.ShoppingCategory;
import com.nido.api.shopping.domain.model.ShoppingException;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RenameShoppingCategoryHandlerTest {

    @Mock ShoppingCategoryRepository categoryRepository;
    private RenameShoppingCategoryHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new RenameShoppingCategoryHandler(categoryRepository);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    private ShoppingCategory category(UUID inSpace) {
        return new ShoppingCategory(categoryId, inSpace, "Épicerie", 0, false);
    }

    @Test
    void a_member_can_rename_a_category_in_their_space() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category(spaceId)));
        ShoppingCategory renamed = new ShoppingCategory(categoryId, spaceId, "Épicerie fine", 0, false);
        when(categoryRepository.rename(categoryId, "Épicerie fine")).thenReturn(renamed);

        ShoppingCategory result = handler.rename(
            new RenameShoppingCategoryCommand(categoryId, spaceId, "Épicerie fine"), membership(SpaceRole.MEMBER));

        assertThat(result).isEqualTo(renamed);
    }

    @Test
    void a_category_from_another_space_is_not_found() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category(UUID.randomUUID())));

        assertThatThrownBy(() -> handler.rename(
            new RenameShoppingCategoryCommand(categoryId, spaceId, "Épicerie fine"), membership(SpaceRole.MEMBER)))
            .isInstanceOf(ShoppingException.CategoryNotFound.class);
    }

    @Test
    void a_viewer_cannot_rename_a_category() {
        assertThatThrownBy(() -> handler.rename(
            new RenameShoppingCategoryCommand(categoryId, spaceId, "Épicerie fine"), membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }
}
