package com.nido.api.calendar.application.service;

import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.space.application.port.in.GetSpaceUseCase;
import com.nido.api.space.domain.model.SpaceDetailView;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.space.domain.port.out.SpaceMembershipPort;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CalendarSpaceMemberValidatorTest {

    private final SpaceMembershipPort memberships = mock(SpaceMembershipPort.class);
    private final GetSpaceUseCase spaces = mock(GetSpaceUseCase.class);
    private final CalendarSpaceMemberValidator validator = new CalendarSpaceMemberValidator(memberships, spaces);

    private final UUID spaceId = UUID.randomUUID();
    private final SpaceMembership owner =
        new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.OWNER, Instant.now());

    private void spaceIs(SpaceType type) {
        when(spaces.get(eq(spaceId), any())).thenReturn(
            new SpaceDetailView(spaceId, type, "Espace", null, "#c17a5c", "🏡", ZoneId.of("Europe/Paris"), SpaceRole.OWNER, 1));
    }

    @Test
    void makesTheOwnerTheOnlyParticipantInAPersonalSpace_whateverWasAskedFor() {
        spaceIs(SpaceType.PERSONAL);

        assertThat(validator.participantsFor(owner, List.of())).containsExactly(owner.userId());
        assertThat(validator.participantsFor(owner, List.of(UUID.randomUUID()))).containsExactly(owner.userId());
    }

    @Test
    void keepsTheMembersPickedInASharedSpace() {
        spaceIs(SpaceType.SHARED);
        UUID partner = UUID.randomUUID();
        when(memberships.find(spaceId, partner)).thenReturn(Optional.of(
            new SpaceMembership(UUID.randomUUID(), spaceId, partner, SpaceRole.MEMBER, Instant.now())));

        assertThat(validator.participantsFor(owner, List.of(partner))).containsExactly(partner);
        assertThat(validator.participantsFor(owner, List.of())).isEmpty();
    }

    @Test
    void takesAParticipantNamedTwiceOnce() {
        spaceIs(SpaceType.SHARED);
        UUID partner = UUID.randomUUID();
        when(memberships.find(spaceId, partner)).thenReturn(Optional.of(
            new SpaceMembership(UUID.randomUUID(), spaceId, partner, SpaceRole.MEMBER, Instant.now())));

        assertThat(validator.participantsFor(owner, List.of(partner, partner))).containsExactly(partner);
    }

    @Test
    void stillRefusesAStrangerInASharedSpace() {
        spaceIs(SpaceType.SHARED);
        UUID stranger = UUID.randomUUID();
        when(memberships.find(spaceId, stranger)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> validator.participantsFor(owner, List.of(stranger)))
            .isInstanceOf(CalendarException.MemberNotInSpace.class);
    }

    @Test
    void keepsAParticipantWhoHasSinceLeftTheSpace() {
        // Who took part stays on record: someone who left the space no longer blocks editing the event.
        spaceIs(SpaceType.SHARED);
        UUID leaver = UUID.randomUUID();
        when(memberships.find(spaceId, leaver)).thenReturn(Optional.empty());

        assertThat(validator.participantsFor(owner, List.of(leaver), List.of(leaver))).containsExactly(leaver);
    }

    @Test
    void stillRefusesAStrangerAddedNextToAParticipantWhoLeft() {
        spaceIs(SpaceType.SHARED);
        UUID leaver = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();
        when(memberships.find(eq(spaceId), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> validator.participantsFor(owner, List.of(leaver, stranger), List.of(leaver)))
            .isInstanceOf(CalendarException.MemberNotInSpace.class);
    }

    @Test
    void closesJoiningAndLeavingInAPersonalSpace() {
        spaceIs(SpaceType.PERSONAL);

        assertThatThrownBy(() -> validator.ensureParticipationOpen(owner))
            .isInstanceOf(CalendarException.ParticipantsFixedInPersonalSpace.class);
    }

    @Test
    void leavesJoiningAndLeavingOpenInASharedSpace() {
        spaceIs(SpaceType.SHARED);

        assertThatCode(() -> validator.ensureParticipationOpen(owner)).doesNotThrowAnyException();
    }
}
