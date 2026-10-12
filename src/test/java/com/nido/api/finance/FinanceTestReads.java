package com.nido.api.finance;

import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.finance.domain.port.out.TransactionRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Reads a test needs and the application does not. */
public final class FinanceTestReads {

    private static final LocalDate FIRST_DAY = LocalDate.of(1900, 1, 1);
    private static final LocalDate LAST_DAY = LocalDate.of(2999, 12, 31);

    private FinanceTestReads() {}

    /** Every operation of the space, whatever its date, opened through the port as a page opens it: a refused value throws. */
    public static List<Transaction> allTransactions(TransactionRepository transactions, UUID spaceId) {
        return transactions.findBySpaceIdAndDateBetween(spaceId, FIRST_DAY, LAST_DAY);
    }
}
