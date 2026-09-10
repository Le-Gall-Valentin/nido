package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.GetBalancesUseCase;
import com.nido.api.finance.domain.model.BalanceCalculator;
import com.nido.api.finance.domain.model.Balances;
import com.nido.api.finance.domain.port.out.SettlementRecordRepository;
import com.nido.api.finance.domain.port.out.TransactionRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;

@ApplicationService
public class GetBalancesHandler implements GetBalancesUseCase {

    private final TransactionRepository transactionRepository;
    private final SettlementRecordRepository settlementRecordRepository;

    public GetBalancesHandler(TransactionRepository transactionRepository, SettlementRecordRepository settlementRecordRepository) {
        this.transactionRepository = transactionRepository;
        this.settlementRecordRepository = settlementRecordRepository;
    }

    @Override
    public Balances getBalances(SpaceMembership caller) {
        return BalanceCalculator.calculate(
            transactionRepository.findSplitsBySpaceId(caller.spaceId()),
            settlementRecordRepository.findBySpaceId(caller.spaceId()));
    }
}
