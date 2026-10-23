package com.nido.api.finance.application.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.nido.api.finance.domain.model.Gesture;
import com.nido.api.space.application.port.in.GetSpaceUseCase;
import com.nido.api.space.application.port.in.ListSpaceMembersUseCase;
import com.nido.api.space.domain.model.Addressee;
import com.nido.api.space.domain.model.SpaceDetailView;
import com.nido.api.space.domain.model.SpaceMemberView;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/** Who in a space can be told about a gesture in its finances, and by which name. */
@ExtendWith(MockitoExtension.class)
class FinanceAudienceTest {

    @Mock ListSpaceMembersUseCase members;
    @Mock GetSpaceUseCase spaces;

    private final UUID spaceId = UUID.randomUUID();
    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();
    private final UUID carol = UUID.randomUUID();
    private final UUID formerMember = UUID.randomUUID();
    private final SpaceMembership aliceActs = new SpaceMembership(UUID.randomUUID(), spaceId, alice, SpaceRole.MEMBER, Instant.now());
    private final ListAppender<ILoggingEvent> logged = new ListAppender<>();

    private FinanceAudience audience;

    @BeforeEach
    void setUp() {
        logged.start();
        ((Logger) LoggerFactory.getLogger(FinanceAudience.class)).addAppender(logged);
        lenient().when(spaces.get(spaceId, aliceActs)).thenReturn(new SpaceDetailView(spaceId, SpaceType.SHARED, "Chez nous",
            null, "#c17a5c", "🏡", ZoneId.of("Europe/Paris"), SpaceRole.MEMBER, 3));
        audience = new FinanceAudience(members, spaces);
    }

    @AfterEach
    void stopListening() {
        ((Logger) LoggerFactory.getLogger(FinanceAudience.class)).detachAppender(logged);
    }

    private static SpaceMemberView member(UUID id, String name) {
        return new SpaceMemberView(id, name, name == null ? null : name + "@test.local", SpaceRole.MEMBER, Instant.now());
    }

    @Test
    void the_gesture_names_the_space_and_its_author() {
        when(members.list(aliceActs)).thenReturn(List.of(member(alice, "alice"), member(bob, "bob")));

        assertThat(audience.of(aliceActs).orElseThrow().gesture()).isEqualTo(new Gesture(spaceId, "Chez nous", alice, "alice"));
    }

    @Test
    void readers_are_named_members_other_than_the_actor() {
        when(members.list(aliceActs)).thenReturn(List.of(member(alice, "alice"), member(bob, "bob"), member(carol, "carol")));

        Audience heard = audience.of(aliceActs).orElseThrow();

        assertThat(heard.readers(List.of(alice, bob, formerMember, bob))).containsExactly(new Addressee(bob, "bob"));
        assertThat(heard.everyoneButTheActor()).containsExactly(new Addressee(bob, "bob"), new Addressee(carol, "carol"));
    }

    @Test
    void an_anonymised_member_is_never_a_reader_and_has_no_name() {
        when(members.list(aliceActs)).thenReturn(List.of(member(alice, "alice"), member(bob, null)));

        Audience heard = audience.of(aliceActs).orElseThrow();

        assertThat(heard.readers(List.of(bob))).isEmpty();
        assertThat(heard.everyoneButTheActor()).isEmpty();
        assertThat(heard.nameOf(bob)).isNull();
        assertThat(heard.nameOf(formerMember)).isNull();
        assertThat(heard.nameOf(alice)).isEqualTo("alice");
    }

    @Test
    void an_actor_without_a_name_tells_nobody_and_warns_with_ids_only() {
        when(members.list(aliceActs)).thenReturn(List.of(member(alice, null), member(bob, "bob")));

        Optional<Audience> heard = audience.of(aliceActs);

        assertThat(heard).isEmpty();
        assertThat(logged.list).singleElement().satisfies(event -> {
            assertThat(event.getFormattedMessage()).contains(alice.toString(), spaceId.toString()).doesNotContain("bob");
        });
    }
}
