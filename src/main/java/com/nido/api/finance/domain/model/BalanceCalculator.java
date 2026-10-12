package com.nido.api.finance.domain.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Computes each member's net balance for a space (positive = is owed money, negative = owes money) from its shared
 * transactions — expense or income, folded in opposite directions — and its settlements, then proposes the smallest set
 * of transfers that would bring every member back to zero: a classic greedy largest-creditor / largest-debtor match,
 * repeated until every net is settled.
 *
 * <p>{@link #fold} is additive: the nets of several months, added up, are the net of all of them. That is what lets
 * MonthTotals keep a month's fold, and the balances read a dozen of them a year instead of every operation.
 */
public final class BalanceCalculator {

    private BalanceCalculator() {}

    /**
     * Each member's net over these transactions and settlements, unrounded. Every member they touch has an entry, even
     * one that comes back to zero: the balances list that member as settled.
     */
    public static Map<UUID, BigDecimal> fold(List<SplitTransaction> transactions, List<SettlementRecord> settlements) {
        Map<UUID, BigDecimal> net = new LinkedHashMap<>();
        for (SplitTransaction t : transactions) {
            // An operation nobody shares moves no member's balance; the space's net counts it, not this fold.
            if (t.contributors().isEmpty() || t.payerId() == null) {
                continue;
            }
            // An expense and an income of the same shape are mirror images, so one direction
            // covers both. Alice fronting 40 of a bill leaves the others owing her their share;
            // Alice receiving 300 of a refund leaves her owing them theirs. Folding an income as
            // though it were an expense does not merely get the amount wrong — it names the wrong
            // person as the one who should pay.
            BigDecimal direction = t.type() == TransactionType.INCOME ? BigDecimal.ONE.negate() : BigDecimal.ONE;
            net.merge(t.payerId(), t.amount().multiply(direction), BigDecimal::add);
            for (Contribution c : t.contributors()) {
                net.merge(c.memberId(), c.shareAmount().multiply(direction).negate(), BigDecimal::add);
            }
        }
        for (SettlementRecord s : settlements) {
            net.merge(s.fromMemberId(), s.amount(), BigDecimal::add);
            net.merge(s.toMemberId(), s.amount().negate(), BigDecimal::add);
        }
        return net;
    }

    /** Rounds each member's net to the cent and proposes the transfers that settle them. */
    public static Balances fromNets(Map<UUID, BigDecimal> nets) {
        Map<UUID, BigDecimal> net = new LinkedHashMap<>();
        nets.forEach((memberId, amount) -> net.put(memberId, amount.setScale(2, java.math.RoundingMode.HALF_UP)));
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
