package com.nido.api.space.application.handler;

import com.nido.api.space.domain.model.Space;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.space.domain.port.out.SpaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetSpaceTodayHandlerTest {

    @Mock SpaceRepository spaceRepository;

    private final UUID spaceId = UUID.randomUUID();

    private LocalDate todayAt(String instant, String zone) {
        Clock clock = Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(new Space(
            spaceId, SpaceType.SHARED, "Chez Valentin", null, "#c17a5c", "🏠", null,
            ZoneId.of(zone), Instant.parse("2026-01-01T00:00:00Z"))));
        return new GetSpaceTodayHandler(spaceRepository, clock).today(spaceId);
    }

    @Test
    void a_household_east_of_the_server_is_already_tomorrow() {
        // 23:30 UTC is half past one in the morning in Paris — the household turned the page an
        // hour and a half ago while the server is still on the previous day. Everything that asks
        // what is due today gets the wrong answer for those two hours, every single night.
        assertThat(todayAt("2026-09-12T23:30:00Z", "Europe/Paris")).isEqualTo(LocalDate.of(2026, 9, 13));
    }

    @Test
    void a_household_west_of_the_server_is_still_yesterday() {
        // The mirror image, and the one that shows up as a task marked late while its evening is
        // still going: 01:30 UTC is half past nine the previous evening in Toronto.
        assertThat(todayAt("2026-09-12T01:30:00Z", "America/Toronto")).isEqualTo(LocalDate.of(2026, 9, 11));
    }

    @Test
    void the_server_own_zone_is_never_the_answer() {
        // Same instant, two households, two different days. No single server-side date can be
        // right for both, which is the whole reason this lookup exists.
        assertThat(todayAt("2026-09-12T23:30:00Z", "Europe/Paris"))
            .isNotEqualTo(LocalDate.ofInstant(Instant.parse("2026-09-12T23:30:00Z"), ZoneOffset.UTC));
    }
}
