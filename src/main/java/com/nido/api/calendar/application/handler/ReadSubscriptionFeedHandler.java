package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.port.in.ReadSubscriptionFeedUseCase;
import com.nido.api.calendar.application.service.CalendarFeedAssembler;
import com.nido.api.calendar.domain.model.CalendarDigests;
import com.nido.api.calendar.domain.model.SubscriptionLink;
import com.nido.api.calendar.domain.model.SubscriptionTokens;
import com.nido.api.calendar.domain.port.out.AccountLanguage;
import com.nido.api.calendar.domain.port.out.ActiveAccounts;
import com.nido.api.calendar.domain.port.out.IcalWriter;
import com.nido.api.calendar.domain.port.out.SubscriptionLinkRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.application.port.in.ResolveMembershipUseCase;
import com.nido.api.space.domain.model.SpaceException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * What a calendar application reads at a secret address. It reads as the address' owner reads today — their current
 * membership, not the one they had when they made it: an owner gone from the space, or whose account is deactivated, reads nothing, whatever the reason.
 * The time of the last read is written at most once an hour, so a phone polling every few minutes costs no write.
 */
@ApplicationService
public class ReadSubscriptionFeedHandler implements ReadSubscriptionFeedUseCase {

    private static final Duration READ_MARK_EVERY = Duration.ofHours(1);

    private final SubscriptionLinkRepository links;
    private final ResolveMembershipUseCase memberships;
    private final CalendarFeedAssembler assembler;
    private final IcalWriter writer;
    private final AccountLanguage language;
    private final ActiveAccounts accounts;
    private final Clock clock;

    public ReadSubscriptionFeedHandler(SubscriptionLinkRepository links, ResolveMembershipUseCase memberships,
                                       CalendarFeedAssembler assembler, IcalWriter writer, AccountLanguage language,
                                       ActiveAccounts accounts, Clock clock) {
        this.links = links;
        this.memberships = memberships;
        this.assembler = assembler;
        this.writer = writer;
        this.language = language;
        this.accounts = accounts;
        this.clock = clock;
    }

    @Override
    public Optional<String> read(String token) {
        if (!SubscriptionTokens.wellFormed(token)) {
            return Optional.empty();
        }
        Optional<SubscriptionLink> found = links.findByTokenHash(CalendarDigests.sha256Hex(token));
        if (found.isEmpty()) {
            return Optional.empty();
        }
        SubscriptionLink link = found.get();
        if (!accounts.isActive(link.userId())) {
            return Optional.empty();
        }
        SpaceMembership reader;
        try {
            reader = memberships.resolve(link.spaceId(), link.userId());
        } catch (SpaceException.NotAMember left) {
            // Its owner left the space: the address reads nothing, and is said not to exist.
            return Optional.empty();
        }
        String content = writer.write(assembler.assemble(reader, link.sources(), link.onlyMine()),
            language.of(link.userId()));
        Instant now = clock.instant();
        if (link.lastReadAt() == null || link.lastReadAt().isBefore(now.minus(READ_MARK_EVERY))) {
            links.markRead(link.id(), now);
        }
        return Optional.of(content);
    }
}
