package com.nido.api.shopping.application.handler;

import com.nido.api.shopping.domain.model.ShoppingCategory;
import com.nido.api.shopping.domain.port.out.ShoppingCategoryRepository;
import com.nido.api.space.domain.model.SpaceException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
    void a_member_can_create_a_category_in_an_already_seeded_space() {
        when(categoryRepository.existsBySpaceId(spaceId)).thenReturn(true);
        ShoppingCategory created = new ShoppingCategory(UUID.randomUUID(), spaceId, "Épicerie", 0, false);
        when(categoryRepository.create(spaceId, "Épicerie", false)).thenReturn(created);

        ShoppingCategory result = handler.create(spaceId, "Épicerie", membership(SpaceRole.MEMBER));

        assertThat(result).isEqualTo(created);
    }

    @Test
    void seeds_the_default_categories_first_when_the_space_has_none_yet() {
        when(categoryRepository.existsBySpaceId(spaceId)).thenReturn(false);
        ShoppingCategory created = new ShoppingCategory(UUID.randomUUID(), spaceId, "Bricolage", 8, false);
        // lenient(): the 7 default-category and 1 fallback create() calls happen first with
        // different arguments — only this exact call is meant to be stubbed.
        lenient().when(categoryRepository.create(spaceId, "Bricolage", false)).thenReturn(created);

        ShoppingCategory result = handler.create(spaceId, "Bricolage", membership(SpaceRole.MEMBER));

        ArgumentCaptor<String> names = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Boolean> fallbacks = ArgumentCaptor.forClass(Boolean.class);
        verify(categoryRepository, times(DefaultShoppingCategorySeeder.DEFAULT_CATEGORY_NAMES.size() + 2))
            .create(eq(spaceId), names.capture(), fallbacks.capture());

        List<String> expectedNames = new ArrayList<>(DefaultShoppingCategorySeeder.DEFAULT_CATEGORY_NAMES);
        expectedNames.add(DefaultShoppingCategorySeeder.DEFAULT_FALLBACK_CATEGORY_NAME);
        expectedNames.add("Bricolage");
        assertThat(names.getAllValues()).containsExactlyElementsOf(expectedNames);
        assertThat(fallbacks.getAllValues()).endsWith(true, false); // fallback category, then the custom one
        assertThat(result).isEqualTo(created);
    }

    @Test
    void a_viewer_cannot_create_a_category() {
        assertThatThrownBy(() -> handler.create(spaceId, "Épicerie", membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }
}
