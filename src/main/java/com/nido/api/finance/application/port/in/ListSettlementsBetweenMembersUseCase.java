package com.nido.api.finance.application.port.in;

import com.nido.api.finance.domain.model.SettlementRecord;
import com.nido.api.space.domain.model.SpaceMembership;

import java.util.List;
import java.util.UUID;

public interface ListSettlementsBetweenMembersUseCase {
    List<SettlementRecord> list(UUID memberAId, UUID memberBId, SpaceMembership caller);
}
