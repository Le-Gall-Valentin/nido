package com.nido.api.finance.application.handler;

import com.nido.api.finance.domain.model.Category;
import com.nido.api.finance.domain.model.CreateCategoryCommand;
import com.nido.api.finance.domain.port.out.CategoryRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateCategoryHandlerTest {

    @Mock CategoryRepository categoryRepository;
    private CreateCategoryHandler handler;
    private final UUID spaceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new CreateCategoryHandler(categoryRepository);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    @Test
    void a_member_can_create_a_custom_category() {
        CreateCategoryCommand command = new CreateCategoryCommand(spaceId, "Animaux", "#a3e635", "PawPrint");
        Category created = new Category(UUID.randomUUID(), spaceId, "Animaux", "#a3e635", "PawPrint", false);
        when(categoryRepository.existsBySpaceId(spaceId)).thenReturn(true);
        when(categoryRepository.create(eq(command), eq(false))).thenReturn(created);

        Category result = handler.create(command, membership(SpaceRole.MEMBER));

        assertThat(result).isEqualTo(created);
    }

    @Test
    void a_viewer_cannot_create_a_category() {
        CreateCategoryCommand command = new CreateCategoryCommand(spaceId, "Animaux", "#a3e635", "PawPrint");

        assertThatThrownBy(() -> handler.create(command, membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }

    @Test
    void creating_a_category_for_another_space_than_the_callers_is_rejected() {
        CreateCategoryCommand command = new CreateCategoryCommand(UUID.randomUUID(), "Animaux", "#a3e635", "PawPrint");

        assertThatThrownBy(() -> handler.create(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(SpaceException.NotAMember.class);
    }
}
