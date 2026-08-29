package com.nido.api.kitchen.application.handler;

import com.nido.api.kitchen.domain.model.KitchenException;
import com.nido.api.kitchen.domain.model.MenuEntry;
import com.nido.api.kitchen.domain.model.UpdateMenuEntryPortionsCommand;
import com.nido.api.kitchen.domain.port.out.MenuRepository;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateMenuEntryPortionsHandlerTest {

    @Mock MenuRepository menuRepository;
    private UpdateMenuEntryPortionsHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID entryId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new UpdateMenuEntryPortionsHandler(menuRepository);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    private MenuEntry entry(UUID inSpace) {
        return new MenuEntry(entryId, inSpace, LocalDate.of(2026, 9, 7), UUID.randomUUID(), 4, 0);
    }

    @Test
    void a_member_can_update_the_portions_of_an_entry_in_their_space() {
        UpdateMenuEntryPortionsCommand command = new UpdateMenuEntryPortionsCommand(entryId, spaceId, 6);
        when(menuRepository.findById(entryId)).thenReturn(Optional.of(entry(spaceId)));

        handler.updatePortions(command, membership(SpaceRole.MEMBER));

        verify(menuRepository).updatePortions(entryId, 6);
    }

    @Test
    void an_entry_in_another_space_is_not_found() {
        UpdateMenuEntryPortionsCommand command = new UpdateMenuEntryPortionsCommand(entryId, spaceId, 6);
        when(menuRepository.findById(entryId)).thenReturn(Optional.of(entry(UUID.randomUUID())));

        assertThatThrownBy(() -> handler.updatePortions(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(KitchenException.MenuEntryNotFound.class);
    }

    @Test
    void a_command_for_another_space_is_rejected_before_touching_the_entry() {
        UpdateMenuEntryPortionsCommand command = new UpdateMenuEntryPortionsCommand(entryId, UUID.randomUUID(), 6);

        assertThatThrownBy(() -> handler.updatePortions(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(SpaceException.NotAMember.class);
    }

    @Test
    void a_viewer_cannot_update_portions() {
        // ensureCanWrite() throws before findById is ever reached, so no stub is needed here.
        UpdateMenuEntryPortionsCommand command = new UpdateMenuEntryPortionsCommand(entryId, spaceId, 6);

        assertThatThrownBy(() -> handler.updatePortions(command, membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }
}
