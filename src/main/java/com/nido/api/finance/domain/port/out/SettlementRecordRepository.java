package com.nido.api.finance.domain.port.out;

import com.nido.api.finance.domain.model.CreateSettlementCommand;
import com.nido.api.finance.domain.model.SettlementRecord;

import java.util.List;
import java.util.UUID;

public interface SettlementRecordRepository {
    List<SettlementRecord> findBySpaceId(UUID spaceId);
    SettlementRecord create(CreateSettlementCommand command);
}
