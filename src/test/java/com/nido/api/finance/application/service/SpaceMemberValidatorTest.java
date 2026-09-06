package com.nido.api.finance.application.service;

import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.port.out.SpaceMembershipPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpaceMemberValidatorTest {

    @Mock SpaceMembershipPort spaceMembershipPort;
    private SpaceMemberValidator validator;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID memberId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        validator = new SpaceMemberValidator(spaceMembershipPort);
    }

    @Test
    void does_nothing_when_the_member_belongs_to_the_space() {
        when(spaceMembershipPort.find(spaceId, memberId)).thenReturn(
            Optional.of(new SpaceMembership(UUID.randomUUID(), spaceId, memberId, SpaceRole.MEMBER, Instant.now())));

        assertThatCode(() -> validator.ensureMember(spaceId, memberId)).doesNotThrowAnyException();
    }

    @Test
    void rejects_a_member_id_that_does_not_belong_to_the_space() {
        when(spaceMembershipPort.find(spaceId, memberId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> validator.ensureMember(spaceId, memberId))
            .isInstanceOf(FinanceException.MemberNotInSpace.class);
    }
}
