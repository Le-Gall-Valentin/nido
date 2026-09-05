package com.nido.api.finance.application.handler;

import com.nido.api.finance.domain.model.Category;
import com.nido.api.finance.domain.model.UpdateCategoryCommand;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateCategoryHandlerTest {

    @Mock CategoryRepository categoryRepository;
    private UpdateCategoryHandler handler;
    private final UUID spaceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new UpdateCategoryHandler(categoryRepository);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    @Test
    void a_member_can_rename_a_category() {
        UpdateCategoryCommand command = new UpdateCategoryCommand(UUID.randomUUID(), spaceId, "Nouveau nom", "#22c55e", "Heart");
        Category updated = new Category(command.categoryId(), spaceId, "Nouveau nom", "#22c55e", "Heart", false);
        when(categoryRepository.update(command)).thenReturn(updated);

        Category result = handler.update(command, membership(SpaceRole.MEMBER));

        assertThat(result).isEqualTo(updated);
    }

    @Test
    void a_viewer_cannot_update_a_category() {
        UpdateCategoryCommand command = new UpdateCategoryCommand(UUID.randomUUID(), spaceId, "Nouveau nom", "#22c55e", "Heart");

        assertThatThrownBy(() -> handler.update(command, membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }
}
