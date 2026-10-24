package com.nido.api.finance.application.service;

import com.nido.api.finance.domain.model.OpeningBalance;
import com.nido.api.finance.domain.model.SettlementAuthor;
import com.nido.api.finance.domain.model.SettlementRecord;
import com.nido.api.finance.domain.model.Standing;
import com.nido.api.finance.domain.model.SuggestedTransfer;
import com.nido.api.finance.domain.port.out.FinanceNotificationPort;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.Addressee;
import com.nido.api.space.domain.model.SpaceMembership;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Who hears about the balances of a space. A repayment is a matter between two members: only the side that did not
 * record it hears of it, with what is left between them. The opening balance moves every balance of the space: every
 * member but the author hears of it — unless it was set again as it was.
 *
 * <p>Called by the handlers inside their transaction, after their change is written.
 */
@ApplicationService
public class BalanceNotifier {

    private final FinanceAudience audience;
    private final FinanceNotificationPort notifications;

    public BalanceNotifier(FinanceAudience audience, FinanceNotificationPort notifications) {
        this.audience = audience;
        this.notifications = notifications;
    }

    /**
     * @param remaining what the balances still suggest between the two once this repayment is in, either way; empty
     *                  when they are even
     */
    public void settlementRecorded(SpaceMembership actor, SettlementRecord record, Optional<SuggestedTransfer> remaining) {
        boolean debtorActs = record.fromMemberId().equals(actor.userId());
        UUID other = debtorActs ? record.toMemberId() : record.fromMemberId();
        Standing standing = remaining
            .map(transfer -> transfer.fromMemberId().equals(other) ? Standing.YOU_OWE : Standing.OWED_TO_YOU)
            .orElse(Standing.EVEN);
        BigDecimal left = remaining.map(SuggestedTransfer::amount).orElse(BigDecimal.ZERO);
        audience.of(actor).ifPresent(heard -> heard.readers(List.of(other)).forEach(reader ->
            notifications.settlementRecorded(heard.gesture(), debtorActs ? SettlementAuthor.DEBTOR : SettlementAuthor.CREDITOR,
                record.amount(), record.date(), standing, left, reader)));
    }

    public void openingBalanceSet(SpaceMembership actor, Optional<OpeningBalance> before, OpeningBalance after) {
        boolean unchanged = before
            .filter(previous -> previous.amount().compareTo(after.amount()) == 0 && previous.asOf().equals(after.asOf()))
            .isPresent();
        if (!unchanged) {
            audience.of(actor).ifPresent(heard -> {
                List<Addressee> readers = heard.everyoneButTheActor();
                if (!readers.isEmpty()) {
                    notifications.openingBalanceSet(heard.gesture(), after.amount(), after.asOf(), readers);
                }
            });
        }
    }

    /** @param before the opening balance there was, read before it was removed */
    public void openingBalanceCleared(SpaceMembership actor, Optional<OpeningBalance> before) {
        if (before.isPresent()) {
            audience.of(actor).ifPresent(heard -> {
                List<Addressee> readers = heard.everyoneButTheActor();
                if (!readers.isEmpty()) {
                    notifications.openingBalanceCleared(heard.gesture(), readers);
                }
            });
        }
    }
}
