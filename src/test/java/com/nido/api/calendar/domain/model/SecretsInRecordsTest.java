package com.nido.api.calendar.domain.model;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** A record is printed in logs and in failing tests: one holding a secret address never prints it. */
class SecretsInRecordsTest {

    @Test
    void aFollowedCalendarNeverPrintsItsAddress() {
        ExternalFeed feed = new ExternalFeed(UUID.randomUUID(), UUID.randomUUID(), "Club",
            URI.create("https://calendar.google.com/calendar/ical/private-s3cr3t/basic.ics"), null, UUID.randomUUID(),
            Instant.EPOCH, Instant.EPOCH, Instant.EPOCH, null, null, null);

        assertThat(feed.toString()).doesNotContain("s3cr3t").contains("calendar.google.com");
        assertThat(new FollowExternalFeedCommand("https://x.example/?token=s3cr3t", "Club", null).toString())
            .doesNotContain("s3cr3t");
    }

    @Test
    void anAddressJustGivenNeverPrintsItsToken() {
        SubscriptionLink link = new SubscriptionLink(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Téléphone",
            Set.of(CalendarSourceType.EVENT), false, Instant.EPOCH, null);

        assertThat(CreatedSubscriptionLink.at("https://nido.example", link, "s3cr3t").toString()).doesNotContain("s3cr3t");
    }
}
