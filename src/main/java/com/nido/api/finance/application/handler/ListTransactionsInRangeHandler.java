package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.ListTransactionsInRangeUseCase;
import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.finance.domain.port.out.TransactionRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@ApplicationService
public class ListTransactionsInRangeHandler implements ListTransactionsInRangeUseCase {

    private final TransactionRepository transactionRepository;

    public ListTransactionsInRangeHandler(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> list(SpaceMembership caller, LocalDate from, LocalDate to) {
        return transactionRepository.findBySpaceIdAndDateBetween(caller.spaceId(), from, to);
    }
}
