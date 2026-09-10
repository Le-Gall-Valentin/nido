package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.ListSettlementsBetweenMembersUseCase;
import com.nido.api.finance.domain.model.SettlementRecord;
import com.nido.api.finance.domain.port.out.SettlementRecordRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;

import java.util.List;
import java.util.UUID;

@ApplicationService
public class ListSettlementsBetweenMembersHandler implements ListSettlementsBetweenMembersUseCase {

    private final SettlementRecordRepository settlementRecordRepository;

    public ListSettlementsBetweenMembersHandler(SettlementRecordRepository settlementRecordRepository) {
        this.settlementRecordRepository = settlementRecordRepository;
    }

    @Override
    public List<SettlementRecord> list(UUID memberAId, UUID memberBId, SpaceMembership caller) {
        // Filtered and ordered by the database. This used to load every settlement in the space
        // — decrypting each amount — to keep the handful concerning one pair.
        return settlementRecordRepository.findBetweenMembers(caller.spaceId(), memberAId, memberBId);
    }
}
