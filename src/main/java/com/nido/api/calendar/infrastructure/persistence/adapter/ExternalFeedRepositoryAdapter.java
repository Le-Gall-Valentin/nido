package com.nido.api.calendar.infrastructure.persistence.adapter;

import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.ExternalFeed;
import com.nido.api.calendar.domain.model.FeedError;
import com.nido.api.calendar.domain.model.FeedToShow;
import com.nido.api.calendar.domain.port.out.ExternalFeedContentRepository;
import com.nido.api.calendar.domain.port.out.ExternalFeedRefreshRepository;
import com.nido.api.calendar.domain.port.out.ExternalFeedRepository;
import com.nido.api.calendar.infrastructure.persistence.entity.CalendarExternalFeedContentEntity;
import com.nido.api.calendar.infrastructure.persistence.entity.CalendarExternalFeedEntity;
import com.nido.api.calendar.infrastructure.persistence.repository.CalendarExternalFeedContentJpaRepository;
import com.nido.api.calendar.infrastructure.persistence.repository.CalendarExternalFeedJpaRepository;
import com.nido.api.infrastructure.sealing.SpaceSealer;
import com.nido.api.infrastructure.sealing.SpaceSealers;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The calendars followed, their files apart (a calendar's file is read only when not in memory already). Each write
 * changes only the columns it is about (the entity's DynamicUpdate), so that a rename and a refresh at once both stand.
 */
@Component
public class ExternalFeedRepositoryAdapter
    implements ExternalFeedRepository, ExternalFeedRefreshRepository, ExternalFeedContentRepository {

    private final CalendarExternalFeedJpaRepository feeds;
    private final CalendarExternalFeedContentJpaRepository contents;
    private final SpaceSealers sealers;
    private final CalendarAdvisoryLocks locks;

    public ExternalFeedRepositoryAdapter(CalendarExternalFeedJpaRepository feeds,
                                         CalendarExternalFeedContentJpaRepository contents, SpaceSealers sealers,
                                         CalendarAdvisoryLocks locks) {
        this.feeds = feeds;
        this.contents = contents;
        this.sealers = sealers;
        this.locks = locks;
    }

    @Override
    @Transactional
    public Optional<ExternalFeed> createUnlessFollowing(int most, UUID spaceId, String name, URI url, String color,
                                                        UUID createdBy, String content, String etag, String lastModified,
                                                        Instant fetchedAt) {
        locks.lock("external-feeds:" + spaceId);
        if (feeds.countBySpaceId(spaceId) >= most) {
            return Optional.empty();
        }
        SpaceSealer sealer = sealers.forSpace(spaceId);
        CalendarExternalFeedEntity feed = new CalendarExternalFeedEntity();
        feed.setSpaceId(spaceId);
        feed.setNameEncrypted(sealer.seal(CalendarExternalFeedEntity.NAME, feed.getId(), name));
        feed.setUrlEncrypted(sealer.seal(CalendarExternalFeedEntity.URL, feed.getId(), url.toString()));
        feed.setColor(color);
        feed.setCreatedBy(createdBy);
        feed.setContentFetchedAt(fetchedAt);
        feed.setLastAttemptAt(fetchedAt);
        feed.setEtag(etag);
        feed.setLastModified(lastModified);
        CalendarExternalFeedEntity saved = feeds.saveAndFlush(feed);
        CalendarExternalFeedContentEntity row = new CalendarExternalFeedContentEntity();
        row.setFeedId(saved.getId());
        row.setContentEncrypted(sealer.seal(CalendarExternalFeedContentEntity.CONTENT, row.getId(), PackedContent.pack(content)));
        contents.saveAndFlush(row);
        return Optional.of(toDomain(saved));
    }

    @Override
    public List<ExternalFeed> findBySpaceId(UUID spaceId) {
        return feeds.findBySpaceIdOrderByCreatedAt(spaceId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<ExternalFeed> findById(UUID feedId) {
        return feeds.findById(feedId).map(this::toDomain);
    }

    @Override
    public long countBySpaceId(UUID spaceId) {
        return feeds.countBySpaceId(spaceId);
    }

    @Override
    @Transactional
    public void update(UUID feedId, String name, String color) {
        CalendarExternalFeedEntity feed = feeds.findById(feedId).orElseThrow(CalendarException.ExternalFeedNotFound::new);
        feed.setNameEncrypted(sealers.forSpace(feed.getSpaceId()).seal(CalendarExternalFeedEntity.NAME, feed.getId(), name));
        feed.setColor(color);
        feeds.saveAndFlush(feed);
    }

    @Override
    @Transactional
    public void delete(UUID feedId) {
        feeds.deleteById(feedId); // its content goes with it: ON DELETE CASCADE
    }

    @Override
    public List<FeedToShow> findShownBySpaceId(UUID spaceId) {
        return feeds.findShownBySpaceId(spaceId);
    }

    @Override
    public String loadContent(UUID feedId) {
        CalendarExternalFeedEntity feed = feeds.findById(feedId).orElseThrow(CalendarException.ExternalFeedNotFound::new);
        CalendarExternalFeedContentEntity row = contents.findByFeedId(feedId).orElseThrow(CalendarException.ExternalFeedNotFound::new);
        return PackedContent.unpack(sealers.forSpace(feed.getSpaceId())
            .open(CalendarExternalFeedContentEntity.CONTENT, row.getId(), row.getContentEncrypted()));
    }

    @Override
    @Transactional
    public void recordSuccess(UUID feedId, String content, String etag, String lastModified, Instant at) {
        CalendarExternalFeedEntity feed = feeds.findById(feedId).orElseThrow(CalendarException.ExternalFeedNotFound::new);
        CalendarExternalFeedContentEntity row = contents.findByFeedId(feedId).orElseThrow(CalendarException.ExternalFeedNotFound::new);
        row.setContentEncrypted(sealers.forSpace(feed.getSpaceId())
            .seal(CalendarExternalFeedContentEntity.CONTENT, row.getId(), PackedContent.pack(content)));
        contents.saveAndFlush(row);
        feed.setContentFetchedAt(at);
        feed.setLastAttemptAt(at);
        feed.setLastError(null);
        feed.setEtag(etag);
        feed.setLastModified(lastModified);
        feeds.saveAndFlush(feed);
    }

    @Override
    @Transactional
    public void recordNotModified(UUID feedId, Instant at) {
        CalendarExternalFeedEntity feed = feeds.findById(feedId).orElseThrow(CalendarException.ExternalFeedNotFound::new);
        feed.setLastAttemptAt(at);
        feed.setLastError(null);
        feeds.saveAndFlush(feed);
    }

    @Override
    @Transactional
    public void recordFailure(UUID feedId, FeedError error, Instant at) {
        CalendarExternalFeedEntity feed = feeds.findById(feedId).orElseThrow(CalendarException.ExternalFeedNotFound::new);
        feed.setLastAttemptAt(at);
        feed.setLastError(error.code());
        feeds.saveAndFlush(feed);
    }

    @Override
    public List<UUID> findDueIds(Instant before, int limit) {
        return feeds.findIdsAttemptedBefore(before, PageRequest.of(0, limit));
    }

    @Override
    @Transactional
    public boolean claimAttempt(UUID feedId, Instant at, Instant lastBefore) {
        return feeds.claimAttempt(feedId, at, lastBefore) == 1;
    }

    @Override
    @Transactional
    public void markAttempt(UUID feedId, Instant at) {
        CalendarExternalFeedEntity feed = feeds.findById(feedId).orElseThrow(CalendarException.ExternalFeedNotFound::new);
        feed.setLastAttemptAt(at);
        feeds.saveAndFlush(feed);
    }

    private ExternalFeed toDomain(CalendarExternalFeedEntity e) {
        SpaceSealer sealer = sealers.forSpace(e.getSpaceId());
        return new ExternalFeed(e.getId(), e.getSpaceId(),
            sealer.open(CalendarExternalFeedEntity.NAME, e.getId(), e.getNameEncrypted()),
            URI.create(sealer.open(CalendarExternalFeedEntity.URL, e.getId(), e.getUrlEncrypted())),
            e.getColor(), e.getCreatedBy(), e.getCreatedAt(), e.getContentFetchedAt(), e.getLastAttemptAt(),
            e.getLastError() == null ? null : FeedError.parse(e.getLastError()), e.getEtag(), e.getLastModified());
    }
}
