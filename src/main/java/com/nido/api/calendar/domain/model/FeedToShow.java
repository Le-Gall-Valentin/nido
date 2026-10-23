package com.nido.api.calendar.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * What a calendar read needs of a calendar the space follows: which one, its colour, and when its file was downloaded —
 * the file kept in memory is read again only after that changes. Never its address, which often holds a secret, nor its
 * name, both sealed and shown elsewhere.
 */
public record FeedToShow(UUID id, String color, Instant contentFetchedAt) {}
