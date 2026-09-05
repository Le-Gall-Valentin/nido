package com.nido.api.finance.domain.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Computes each member's net balance for a space (positive = is owed money,
 * negative = owes money) from every shared transaction and every recorded
 * settlement, then proposes the smallest set of transfers that would bring
 * every member back to zero — a classic greedy largest-creditor /
 * largest-debtor match, repeated until every net is settled.
 */
public final class BalanceCalculator {

    private BalanceCalculator() {}

    public static Balances calculate(List<Transaction> transactions, List<SettlementRecord> settlements) {
        Map<UUID, BigDecimal> net = new LinkedHashMap<>();
        for (Transaction t : transactions) {
            if (t.contributors().isEmpty() || t.payerId() == null) {
                continue;
            }
            net.merge(t.payerId(), t.amount(), BigDecimal::add);
            for (Contribution c : t.contributors()) {
                net.merge(c.memberId(), c.shareAmount().negate(), BigDecimal::add);
            }
        }
        for (SettlementRecord s : settlements) {
            net.merge(s.fromMemberId(), s.amount(), BigDecimal::add);
            net.merge(s.toMemberId(), s.amount().negate(), BigDecimal::add);
        }
        net.replaceAll((memberId, amount) -> amount.setScale(2, java.math.RoundingMode.HALF_UP));

        List<MemberBalance> netByMember = net.entrySet().stream()
            .map(e -> new MemberBalance(e.getKey(), e.getValue())).toList();
        return new Balances(netByMember, settleDebts(net));
    }

    private static List<SuggestedTransfer> settleDebts(Map<UUID, BigDecimal> net) {
        List<Map.Entry<UUID, BigDecimal>> creditors = new ArrayList<>();
        List<Map.Entry<UUID, BigDecimal>> debtors = new ArrayList<>();
        for (Map.Entry<UUID, BigDecimal> e : net.entrySet()) {
            if (e.getValue().compareTo(BigDecimal.ZERO) > 0) creditors.add(e);
            else if (e.getValue().compareTo(BigDecimal.ZERO) < 0) debtors.add(e);
        }
        creditors.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        debtors.sort((a, b) -> a.getValue().compareTo(b.getValue()));

        List<SuggestedTransfer> transfers = new ArrayList<>();
        int ci = 0, di = 0;
        Map<UUID, BigDecimal> remaining = new LinkedHashMap<>();
        creditors.forEach(e -> remaining.put(e.getKey(), e.getValue()));
        debtors.forEach(e -> remaining.put(e.getKey(), e.getValue()));

        while (ci < creditors.size() && di < debtors.size()) {
            UUID creditor = creditors.get(ci).getKey();
            UUID debtor = debtors.get(di).getKey();
            BigDecimal owed = remaining.get(creditor);
            BigDecimal owes = remaining.get(debtor).negate();
            BigDecimal transfer = owed.min(owes);
            if (transfer.compareTo(BigDecimal.ZERO) > 0) {
                transfers.add(new SuggestedTransfer(debtor, creditor, transfer));
            }
            remaining.put(creditor, owed.subtract(transfer));
            remaining.put(debtor, remaining.get(debtor).add(transfer));
            if (remaining.get(creditor).compareTo(BigDecimal.ZERO) == 0) ci++;
            if (remaining.get(debtor).compareTo(BigDecimal.ZERO) == 0) di++;
        }
        return transfers;
    }
}
