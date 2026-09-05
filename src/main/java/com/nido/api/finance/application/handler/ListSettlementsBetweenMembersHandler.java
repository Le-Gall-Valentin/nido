package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.ListSettlementsBetweenMembersUseCase;
import com.nido.api.finance.domain.model.SettlementRecord;
import com.nido.api.finance.domain.port.out.SettlementRecordRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;

import java.util.Comparator;
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
        return settlementRecordRepository.findBySpaceId(caller.spaceId()).stream()
            .filter(s -> isBetween(s, memberAId, memberBId))
            .sorted(Comparator.comparing(SettlementRecord::date).reversed())
            .toList();
    }

    private boolean isBetween(SettlementRecord settlement, UUID memberAId, UUID memberBId) {
        return (settlement.fromMemberId().equals(memberAId) && settlement.toMemberId().equals(memberBId))
            || (settlement.fromMemberId().equals(memberBId) && settlement.toMemberId().equals(memberAId));
    }
}
