package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.SettleDebtUseCase;
import com.nido.api.finance.application.service.BalanceNotifier;
import com.nido.api.finance.application.service.SpaceMemberValidator;
import com.nido.api.finance.domain.model.BalanceCalculator;
import com.nido.api.finance.domain.model.Balances;
import com.nido.api.finance.domain.model.CreateSettlementCommand;
import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.model.SettlementRecord;
import com.nido.api.finance.domain.model.SuggestedTransfer;
import com.nido.api.finance.domain.port.out.RunningTotalsRepository;
import com.nido.api.finance.domain.port.out.SettlementRecordRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@ApplicationService
public class SettleDebtHandler implements SettleDebtUseCase {

    private final SettlementRecordRepository settlementRecordRepository;
    private final RunningTotalsRepository runningTotals;
    private final SpaceMemberValidator spaceMemberValidator;
    private final BalanceNotifier notifier;

    public SettleDebtHandler(
            SettlementRecordRepository settlementRecordRepository, RunningTotalsRepository runningTotals,
            SpaceMemberValidator spaceMemberValidator, BalanceNotifier notifier) {
        this.settlementRecordRepository = settlementRecordRepository;
        this.runningTotals = runningTotals;
        this.spaceMemberValidator = spaceMemberValidator;
        this.notifier = notifier;
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
        spaceMemberValidator.ensureMember(command.spaceId(), command.fromMemberId());
        spaceMemberValidator.ensureMember(command.spaceId(), command.toMemberId());
        // Serializes concurrent settlements between this pair — without it, two requests could
        // both recompute the same remaining debt and each record a settlement against it,
        // together exceeding what's actually owed (confirmed by firing concurrent requests
        // against a real debt before this lock existed).
        settlementRecordRepository.lockForSettlement(command.spaceId(), command.fromMemberId(), command.toMemberId());
        // The real debt, recomputed server-side rather than trusted from the client — the
        // frontend already caps this the same way, but only as a UX nicety; this is the
        // actual guarantee, using the same suggested-transfer amount the balances view offers.
        BigDecimal owed = realDebt(command.spaceId(), command.fromMemberId(), command.toMemberId());
        if (command.amount().compareTo(owed) > 0) {
            throw new FinanceException.SettlementExceedsDebt();
        }
        SettlementRecord created = settlementRecordRepository.create(command);
        // Read again once the repayment is in: writing it refreshed the month totals the balances are read from.
        notifier.settlementRecorded(caller, created,
            remainingBetween(command.spaceId(), command.fromMemberId(), command.toMemberId()));
        return created;
    }

    /** What the balances view still suggests between these two members, whichever of them owes it. */
    private Optional<SuggestedTransfer> remainingBetween(UUID spaceId, UUID one, UUID other) {
        return BalanceCalculator.fromNets(runningTotals.findNetByMember(spaceId)).suggestedTransfers().stream()
            .filter(t -> t.fromMemberId().equals(one) && t.toMemberId().equals(other)
                || t.fromMemberId().equals(other) && t.toMemberId().equals(one))
            .findFirst();
    }

    private BigDecimal realDebt(UUID spaceId, UUID fromMemberId, UUID toMemberId) {
        Balances balances = BalanceCalculator.fromNets(runningTotals.findNetByMember(spaceId));
        return balances.suggestedTransfers().stream()
            .filter(t -> t.fromMemberId().equals(fromMemberId) && t.toMemberId().equals(toMemberId))
            .map(SuggestedTransfer::amount)
            .findFirst()
            .orElse(BigDecimal.ZERO);
    }
}
