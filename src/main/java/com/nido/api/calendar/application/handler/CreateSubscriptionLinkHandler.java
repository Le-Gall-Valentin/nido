package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.port.in.CreateSubscriptionLinkUseCase;
import com.nido.api.calendar.domain.model.CalendarDigests;
import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.CalendarSourceType;
import com.nido.api.calendar.domain.model.CreateSubscriptionLinkCommand;
import com.nido.api.calendar.domain.model.CreatedSubscriptionLink;
import com.nido.api.calendar.domain.model.SubscriptionTokens;
import com.nido.api.calendar.domain.port.out.SubscriptionLinkRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

/** Any member may have addresses, a viewer included: an address shows what its owner already sees. */
@ApplicationService
public class CreateSubscriptionLinkHandler implements CreateSubscriptionLinkUseCase {

    static final int MAX_PER_PERSON_AND_SPACE = 10;

    private final SubscriptionLinkRepository links;

    public CreateSubscriptionLinkHandler(SubscriptionLinkRepository links) {
        this.links = links;
    }

    @Override
    @Transactional
    public CreatedSubscriptionLink create(CreateSubscriptionLinkCommand command, SpaceMembership caller) {
        var sources = CalendarSourceType.checkExportable(command.sources());
        if (links.countBySpaceAndUser(caller.spaceId(), caller.userId()) >= MAX_PER_PERSON_AND_SPACE) {
            throw new CalendarException.TooManySubscriptionLinks(MAX_PER_PERSON_AND_SPACE);
        }
        String token = SubscriptionTokens.newToken();
        return new CreatedSubscriptionLink(links.create(caller.spaceId(), caller.userId(), command.label().strip(), sources,
            command.onlyMine(), CalendarDigests.sha256Hex(token)), token);
    }
}
