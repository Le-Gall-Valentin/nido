package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.port.in.CreateSubscriptionLinkUseCase;
import com.nido.api.calendar.domain.model.CalendarDigests;
import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.CalendarSourceType;
import com.nido.api.calendar.domain.model.CreateSubscriptionLinkCommand;
import com.nido.api.calendar.domain.model.CreatedSubscriptionLink;
import com.nido.api.calendar.domain.model.SubscriptionLink;
import com.nido.api.calendar.domain.port.out.PublicAddressPort;
import com.nido.api.calendar.domain.port.out.SubscriptionLinkRepository;
import com.nido.api.calendar.domain.port.out.SubscriptionTokenGeneratorPort;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

/** Any member may have addresses, a viewer included: an address shows what its owner already sees. */
@ApplicationService
public class CreateSubscriptionLinkHandler implements CreateSubscriptionLinkUseCase {

    static final int MAX_PER_PERSON_AND_SPACE = 10;

    private final SubscriptionLinkRepository links;
    private final PublicAddressPort publicAddress;
    private final SubscriptionTokenGeneratorPort tokens;

    public CreateSubscriptionLinkHandler(SubscriptionLinkRepository links, PublicAddressPort publicAddress,
                                         SubscriptionTokenGeneratorPort tokens) {
        this.links = links;
        this.publicAddress = publicAddress;
        this.tokens = tokens;
    }

    @Override
    @Transactional
    public CreatedSubscriptionLink create(CreateSubscriptionLinkCommand command, SpaceMembership caller) {
        // Asked before anything is made: an address nobody could be given must not exist.
        String publicUrl = publicAddress.publicUrl().orElseThrow(CalendarException.PublicUrlMissing::new);
        var sources = CalendarSourceType.checkExportable(command.sources());
        String token = tokens.generate();
        SubscriptionLink link = links.createUnlessHolding(MAX_PER_PERSON_AND_SPACE, caller.spaceId(), caller.userId(),
                command.label().strip(), sources, command.onlyMine(), CalendarDigests.sha256Hex(token))
            .orElseThrow(() -> new CalendarException.TooManySubscriptionLinks(MAX_PER_PERSON_AND_SPACE));
        return CreatedSubscriptionLink.at(publicUrl, link, token);
    }
}
