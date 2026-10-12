package com.nido.api.finance.domain.port.out;

import com.nido.api.finance.domain.model.OpeningBalance;

import java.util.Optional;
import java.util.UUID;

/** One per space at most: saving another replaces it. */
public interface OpeningBalanceRepository {
    Optional<OpeningBalance> findBySpaceId(UUID spaceId);
    OpeningBalance save(OpeningBalance openingBalance);
    void deleteBySpaceId(UUID spaceId);
}
