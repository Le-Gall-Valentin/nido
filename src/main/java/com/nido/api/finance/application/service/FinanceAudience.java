package com.nido.api.finance.application.service;

import com.nido.api.finance.domain.model.Gesture;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.port.in.GetSpaceUseCase;
import com.nido.api.space.application.port.in.ListSpaceMembersUseCase;
import com.nido.api.space.domain.model.SpaceMemberView;
import com.nido.api.space.domain.model.SpaceMembership;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Who in a space can hear about a gesture in its finances: its members and their names, in one call across to the
 * space module, and the space's own name. Both are asked as the author, whatever their role — a read-only member
 * settling a debt is told nothing it may not see.
 *
 * <p>An author who can no longer be named tells nobody, with a warning naming the accounts by id — never an
 * exception, which would undo the gesture it is about.
 */
@ApplicationService
public class FinanceAudience {

    private static final Logger log = LoggerFactory.getLogger(FinanceAudience.class);

    private final ListSpaceMembersUseCase members;
    private final GetSpaceUseCase spaces;

    public FinanceAudience(ListSpaceMembersUseCase members, GetSpaceUseCase spaces) {
        this.members = members;
        this.spaces = spaces;
    }

    public Optional<Audience> of(SpaceMembership actor) {
        Map<UUID, String> names = new LinkedHashMap<>();
        for (SpaceMemberView member : members.list(actor)) {
            if (member.username() != null) {
                names.put(member.userId(), member.username());
            }
        }
        String actorName = names.get(actor.userId());
        if (actorName == null) {
            log.warn("Finance gesture in space {} not told: its author, account {}, can no longer be named",
                actor.spaceId(), actor.userId());
            return Optional.empty();
        }
        String spaceName = spaces.get(actor.spaceId(), actor).name();
        return Optional.of(new Audience(new Gesture(actor.spaceId(), spaceName, actor.userId(), actorName), names));
    }
}
