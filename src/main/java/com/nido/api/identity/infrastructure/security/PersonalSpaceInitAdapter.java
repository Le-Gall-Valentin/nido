package com.nido.api.identity.infrastructure.security;

import com.nido.api.identity.domain.port.out.PersonalSpaceInitPort;
import com.nido.api.space.application.port.in.CreatePersonalSpaceUseCase;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PersonalSpaceInitAdapter implements PersonalSpaceInitPort {

    private final CreatePersonalSpaceUseCase createPersonalSpaceUseCase;

    public PersonalSpaceInitAdapter(CreatePersonalSpaceUseCase createPersonalSpaceUseCase) {
        this.createPersonalSpaceUseCase = createPersonalSpaceUseCase;
    }

    @Override
    public void initForUser(UUID userId) {
        createPersonalSpaceUseCase.createFor(userId);
    }
}
