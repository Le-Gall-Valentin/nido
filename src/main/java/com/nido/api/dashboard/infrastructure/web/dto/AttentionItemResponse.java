package com.nido.api.dashboard.infrastructure.web.dto;

import com.nido.api.dashboard.domain.model.AttentionItem;
import com.nido.api.space.domain.model.SpaceRole;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** One "À traiter" item, with a {@code kind} discriminator the client switches on. */
public sealed interface AttentionItemResponse {

    String kind();

    String severity();

    record OverdueTasks(String kind, String severity, int count, List<String> titles) implements AttentionItemResponse {}

    record BudgetOverrun(String kind, String severity, UUID categoryId, String label, BigDecimal spent,
                         BigDecimal limit) implements AttentionItemResponse {}

    record Debt(String kind, String severity, UUID toMemberId, BigDecimal amount) implements AttentionItemResponse {}

    record Invitation(String kind, String severity, UUID invitationId, String spaceName, String spaceGlyph,
                      String spaceAccent, SpaceRole role, String invitedByUsername, Instant expiresAt)
        implements AttentionItemResponse {}

    static AttentionItemResponse from(AttentionItem item) {
        String kind = item.kind().name();
        String severity = item.severity().name();
        return switch (item) {
            case AttentionItem.OverdueTasks overdue ->
                new OverdueTasks(kind, severity, overdue.count(), overdue.titles());
            case AttentionItem.BudgetOverrun overrun ->
                new BudgetOverrun(kind, severity, overrun.categoryId(), overrun.label(), overrun.spent(), overrun.limit());
            case AttentionItem.Debt debt ->
                new Debt(kind, severity, debt.toMemberId(), debt.amount());
            case AttentionItem.Invitation invitation ->
                new Invitation(kind, severity, invitation.invitationId(), invitation.spaceName(), invitation.spaceGlyph(),
                    invitation.spaceAccent(), invitation.role(), invitation.invitedByUsername(), invitation.expiresAt());
        };
    }
}
