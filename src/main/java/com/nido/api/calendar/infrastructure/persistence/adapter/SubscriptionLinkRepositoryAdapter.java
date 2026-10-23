package com.nido.api.calendar.infrastructure.persistence.adapter;

import com.nido.api.calendar.domain.model.CalendarSourceType;
import com.nido.api.calendar.domain.model.SubscriptionLink;
import com.nido.api.calendar.domain.port.out.SubscriptionLinkRepository;
import com.nido.api.calendar.infrastructure.persistence.entity.CalendarSubscriptionLinkEntity;
import com.nido.api.calendar.infrastructure.persistence.repository.CalendarSubscriptionLinkJpaRepository;
import com.nido.api.infrastructure.sealing.SpaceSealer;
import com.nido.api.infrastructure.sealing.SpaceSealers;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class SubscriptionLinkRepositoryAdapter implements SubscriptionLinkRepository {

    private final CalendarSubscriptionLinkJpaRepository links;
    private final SpaceSealers sealers;
    private final CalendarAdvisoryLocks locks;

    public SubscriptionLinkRepositoryAdapter(CalendarSubscriptionLinkJpaRepository links, SpaceSealers sealers,
                                             CalendarAdvisoryLocks locks) {
        this.links = links;
        this.sealers = sealers;
        this.locks = locks;
    }

    @Override
    @Transactional
    public Optional<SubscriptionLink> createUnlessHolding(int most, UUID spaceId, UUID userId, String label,
                                                          Set<CalendarSourceType> sources, boolean onlyMine, String tokenHash) {
        locks.lock("subscription-links:" + spaceId + ":" + userId);
        if (links.countBySpaceIdAndUserId(spaceId, userId) >= most) {
            return Optional.empty();
        }
        CalendarSubscriptionLinkEntity entity = new CalendarSubscriptionLinkEntity();
        entity.setSpaceId(spaceId);
        entity.setUserId(userId);
        entity.setLabelEncrypted(sealers.forSpace(spaceId).seal(CalendarSubscriptionLinkEntity.LABEL, entity.getId(), label));
        entity.setTokenHash(tokenHash);
        entity.setSources(sources.stream().sorted().map(Enum::name).collect(Collectors.joining(",")));
        entity.setOnlyMine(onlyMine);
        return Optional.of(toDomain(links.saveAndFlush(entity)));
    }

    @Override
    public List<SubscriptionLink> findBySpaceAndUser(UUID spaceId, UUID userId) {
        return links.findBySpaceIdAndUserIdOrderByCreatedAt(spaceId, userId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<SubscriptionLink> findByTokenHash(String tokenHash) {
        return links.findByTokenHash(tokenHash).map(this::toDomain);
    }

    @Override
    @Transactional
    public boolean deleteOwned(UUID linkId, UUID spaceId, UUID userId) {
        return links.deleteOwned(linkId, spaceId, userId) > 0;
    }

    @Override
    @Transactional
    public void deleteBySpaceAndUser(UUID spaceId, UUID userId) {
        links.deleteBySpaceAndUser(spaceId, userId);
    }

    @Override
    @Transactional
    public void markRead(UUID linkId, Instant at) {
        links.markRead(linkId, at);
    }

    private SubscriptionLink toDomain(CalendarSubscriptionLinkEntity e) {
        SpaceSealer sealer = sealers.forSpace(e.getSpaceId());
        Set<CalendarSourceType> sources = EnumSet.noneOf(CalendarSourceType.class);
        Arrays.stream(e.getSources().split(",")).map(CalendarSourceType::valueOf).forEach(sources::add);
        return new SubscriptionLink(e.getId(), e.getSpaceId(), e.getUserId(),
            sealer.open(CalendarSubscriptionLinkEntity.LABEL, e.getId(), e.getLabelEncrypted()),
            sources, e.isOnlyMine(), e.getCreatedAt(), e.getLastReadAt());
    }
}
