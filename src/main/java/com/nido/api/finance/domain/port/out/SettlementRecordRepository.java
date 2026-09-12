package com.nido.api.finance.domain.port.out;

import com.nido.api.finance.domain.model.CreateSettlementCommand;
import com.nido.api.finance.domain.model.SettlementRecord;

import java.util.List;
import java.util.UUID;

public interface SettlementRecordRepository {
    List<SettlementRecord> findBySpaceId(UUID spaceId);

    /** The settlements between one pair, either direction, most recent first. */
    List<SettlementRecord> findBetweenMembers(UUID spaceId, UUID memberAId, UUID memberBId);
    SettlementRecord create(CreateSettlementCommand command);

    /**
     * Serializes concurrent settlements between the same two members: held until the
     * caller's transaction commits or rolls back, so two requests racing to settle the
     * same debt can't both recompute the same remaining amount and each record a
     * settlement against it, together exceeding what's actually owed. The two member ids
     * are order-independent — settling A→B and B→A both contend for the same lock.
     */
    void lockForSettlement(UUID spaceId, UUID memberAId, UUID memberBId);
}
