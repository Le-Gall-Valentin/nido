package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.SettleDebtUseCase;
import com.nido.api.finance.domain.model.CreateSettlementCommand;
import com.nido.api.finance.domain.model.SettlementRecord;
import com.nido.api.finance.domain.port.out.SettlementRecordRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class SettleDebtHandler implements SettleDebtUseCase {

    private final SettlementRecordRepository settlementRecordRepository;

    public SettleDebtHandler(SettlementRecordRepository settlementRecordRepository) {
        this.settlementRecordRepository = settlementRecordRepository;
    }

    @Override
    @Transactional
    public SettlementRecord settle(CreateSettlementCommand command, SpaceMembership caller) {
        caller.ensureSameSpace(command.spaceId());
        caller.ensureCanWrite();
        return settlementRecordRepository.create(command);
    }
}
