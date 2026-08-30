package com.nido.api.identity.infrastructure.security;

import com.nido.api.space.application.port.in.CreatePersonalSpaceUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PersonalSpaceInitAdapterTest {

    @Mock CreatePersonalSpaceUseCase createPersonalSpaceUseCase;

    @Test
    void initForUser_delegatesToCreatePersonalSpaceUseCase() {
        PersonalSpaceInitAdapter adapter = new PersonalSpaceInitAdapter(createPersonalSpaceUseCase);
        UUID userId = UUID.randomUUID();

        adapter.initForUser(userId);

        verify(createPersonalSpaceUseCase).createFor(userId);
    }
}
