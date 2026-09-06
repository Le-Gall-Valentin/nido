package com.nido.api.finance.infrastructure.web.dto;

import com.nido.api.finance.domain.model.Balances;

import java.util.List;

public record BalancesResponse(List<MemberBalanceResponse> netByMember, List<SuggestedTransferResponse> suggestedTransfers) {
    public static BalancesResponse from(Balances b) {
        return new BalancesResponse(b.netByMember().stream().map(MemberBalanceResponse::from).toList(),
            b.suggestedTransfers().stream().map(SuggestedTransferResponse::from).toList());
    }
}
