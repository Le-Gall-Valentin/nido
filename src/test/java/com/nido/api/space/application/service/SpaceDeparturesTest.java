package com.nido.api.space.application.service;

import com.nido.api.space.domain.port.out.SpaceMemberDepartureHook;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SpaceDeparturesTest {

    @Test
    void every_module_that_cares_hears_of_the_departure() {
        SpaceMemberDepartureHook calendar = mock(SpaceMemberDepartureHook.class);
        SpaceMemberDepartureHook another = mock(SpaceMemberDepartureHook.class);
        UUID spaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        new SpaceDepartures(List.of(calendar, another)).departed(spaceId, userId);

        verify(calendar).departed(spaceId, userId);
        verify(another).departed(spaceId, userId);
    }
}
