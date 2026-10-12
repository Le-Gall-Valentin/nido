package com.nido.api.finance.infrastructure.persistence.repository;

import java.util.UUID;

/** JPQL projection: how much one month moved one member's balance, still sealed, with the id of its row. */
public record MemberNetRow(UUID id, UUID memberId, String netEncrypted) {}
