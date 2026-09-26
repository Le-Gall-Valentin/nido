package com.nido.api.dashboard.domain.model;

import com.nido.api.space.domain.model.SpaceRole;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Something that calls for the caller's action, whatever module it comes from. */
public sealed interface AttentionItem {

    AttentionKind kind();

    Severity severity();

    /** The caller's overdue tasks — theirs or nobody's — as one item: a count and up to three titles. */
    record OverdueTasks(int count, List<String> titles) implements AttentionItem {
        public AttentionKind kind() { return AttentionKind.OVERDUE_TASKS; }
        public Severity severity() { return Severity.HIGH; }
    }

    record BudgetOverrun(UUID categoryId, String label, BigDecimal spent, BigDecimal limit) implements AttentionItem {
        public AttentionKind kind() { return AttentionKind.BUDGET_OVERRUN; }
        public Severity severity() { return Severity.HIGH; }
    }

    /** A suggested transfer whose payer is the caller. */
    record Debt(UUID toMemberId, BigDecimal amount) implements AttentionItem {
        public AttentionKind kind() { return AttentionKind.DEBT; }
        public Severity severity() { return Severity.MEDIUM; }
    }

    /** {@code invitedByUsername} is null when the inviter's account was anonymized. */
    record Invitation(UUID invitationId, String spaceName, String spaceGlyph, String spaceAccent,
                      SpaceRole role, String invitedByUsername, Instant expiresAt) implements AttentionItem {
        public AttentionKind kind() { return AttentionKind.INVITATION; }
        public Severity severity() { return Severity.INFO; }
    }
}
