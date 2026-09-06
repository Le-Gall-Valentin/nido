package com.nido.api.finance.domain.model;

import java.util.List;

public record Balances(List<MemberBalance> netByMember, List<SuggestedTransfer> suggestedTransfers) {}
