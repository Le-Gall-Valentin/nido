package com.nido.api.kitchen.application.handler;

import com.nido.api.kitchen.domain.model.KitchenException;
import com.nido.api.kitchen.domain.model.MenuEntry;
import com.nido.api.kitchen.domain.port.out.MenuRepository;
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
class RemoveMenuEntryHandlerTest {

    @Mock MenuRepository menuRepository;
    private RemoveMenuEntryHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID entryId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new RemoveMenuEntryHandler(menuRepository);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    private MenuEntry entry(UUID inSpace) {
        return new MenuEntry(entryId, inSpace, LocalDate.of(2026, 9, 7), UUID.randomUUID(), 4, 0);
    }

    @Test
    void a_member_can_remove_an_entry_in_their_space() {
        when(menuRepository.findById(entryId)).thenReturn(Optional.of(entry(spaceId)));

        handler.remove(entryId, membership(SpaceRole.MEMBER));

        verify(menuRepository).remove(entryId);
    }

    @Test
    void an_entry_in_another_space_is_not_found() {
        when(menuRepository.findById(entryId)).thenReturn(Optional.of(entry(UUID.randomUUID())));

        assertThatThrownBy(() -> handler.remove(entryId, membership(SpaceRole.MEMBER)))
            .isInstanceOf(KitchenException.MenuEntryNotFound.class);
    }
}
