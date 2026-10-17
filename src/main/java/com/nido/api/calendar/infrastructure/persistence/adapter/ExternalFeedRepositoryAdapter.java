package com.nido.api.calendar.infrastructure.persistence.adapter;

import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.ExternalFeed;
import com.nido.api.calendar.domain.model.FeedError;
import com.nido.api.calendar.domain.port.out.ExternalFeedRepository;
import com.nido.api.calendar.infrastructure.persistence.entity.CalendarExternalFeedContentEntity;
import com.nido.api.calendar.infrastructure.persistence.entity.CalendarExternalFeedEntity;
import com.nido.api.calendar.infrastructure.persistence.repository.CalendarExternalFeedContentJpaRepository;
import com.nido.api.calendar.infrastructure.persistence.repository.CalendarExternalFeedJpaRepository;
import com.nido.api.infrastructure.sealing.SpaceSealer;
import com.nido.api.infrastructure.sealing.SpaceSealers;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ExternalFeedRepositoryAdapter implements ExternalFeedRepository {

    private final CalendarExternalFeedJpaRepository feeds;
    private final CalendarExternalFeedContentJpaRepository contents;
    private final SpaceSealers sealers;

    public ExternalFeedRepositoryAdapter(CalendarExternalFeedJpaRepository feeds,
                                         CalendarExternalFeedContentJpaRepository contents, SpaceSealers sealers) {
        this.feeds = feeds;
        this.contents = contents;
        this.sealers = sealers;
    }

    @Override
    @Transactional
    public ExternalFeed create(UUID spaceId, String name, URI url, String color, UUID createdBy,
                               String content, String etag, String lastModified, Instant fetchedAt) {
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
        return toDomain(saved);
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
    public List<ExternalFeed> findAttemptedBefore(Instant before) {
        return feeds.findByLastAttemptAtBefore(before).stream().map(this::toDomain).toList();
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
