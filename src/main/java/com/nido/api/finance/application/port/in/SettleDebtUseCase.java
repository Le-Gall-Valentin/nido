package com.nido.api.finance.application.port.in;

import com.nido.api.finance.domain.model.CreateSettlementCommand;
import com.nido.api.finance.domain.model.SettlementRecord;
import com.nido.api.space.domain.model.SpaceMembership;

public interface SettleDebtUseCase {
    SettlementRecord settle(CreateSettlementCommand command, SpaceMembership caller);
}
