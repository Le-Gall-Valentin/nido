package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.SettleDebtUseCase;
import com.nido.api.finance.domain.model.CreateSettlementCommand;
import com.nido.api.finance.domain.model.FinanceException;
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
        // Deliberately not ensureCanWrite(): settling a debt is a personal matter between the
        // debtor and the creditor, not a space-wide write privilege — a VIEWER involved in the
        // debt may still settle it, and a third member with full write access may not settle a
        // debt that isn't theirs.
        if (!caller.userId().equals(command.fromMemberId()) && !caller.userId().equals(command.toMemberId())) {
            throw new FinanceException.NotAPartyToSettlement();
        }
        return settlementRecordRepository.create(command);
    }
}
