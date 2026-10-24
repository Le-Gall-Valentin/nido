package com.nido.api.finance.application.service;

import com.nido.api.finance.domain.model.Involved;
import com.nido.api.finance.domain.model.Involvement;
import com.nido.api.finance.domain.model.RecurringTransactionSeries;
import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.finance.domain.port.out.FinanceNotificationPort;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedSet;
import java.util.UUID;

/**
 * Who hears about an operation or a recurring transaction: whoever it concerns — the member who paid or received it,
 * and those who share it — and never its author. A change tells whoever it concerned before and whoever it concerns
 * after, each where it leaves them; a change that changes nothing a member sees tells nobody.
 *
 * <p>Called by the handlers inside their transaction, after their change is written; the mails leave once it
 * commits. A gesture that concerns nobody but its author reads no member at all.
 */
@ApplicationService
public class OperationNotifier {

    private final FinanceAudience audience;
    private final FinanceFacts facts;
    private final FinanceNotificationPort notifications;

    public OperationNotifier(FinanceAudience audience, FinanceFacts facts, FinanceNotificationPort notifications) {
        this.audience = audience;
        this.facts = facts;
        this.notifications = notifications;
    }

    public void transactionAdded(SpaceMembership actor, Transaction created) {
        if (concernsOthers(actor, created.concernedMembers())) {
            audience.of(actor).ifPresent(heard -> notifications.transactionAdded(heard.gesture(),
                facts.of(created, heard), heard.readers(created.concernedMembers())));
        }
    }

    public void transactionUpdated(SpaceMembership actor, Transaction before, Transaction after) {
        SequencedSet<UUID> concerned = union(before.concernedMembers(), after.concernedMembers());
        if (before.differsFrom(after) && concernsOthers(actor, concerned)) {
            audience.of(actor).ifPresent(heard -> notifications.transactionUpdated(heard.gesture(),
                facts.of(before, heard), facts.of(after, heard),
                involved(heard, concerned, before.concernedMembers(), after.concernedMembers())));
        }
    }

    public void transactionDeleted(SpaceMembership actor, Transaction deleted) {
        if (concernsOthers(actor, deleted.concernedMembers())) {
            audience.of(actor).ifPresent(heard -> notifications.transactionDeleted(heard.gesture(),
                facts.of(deleted, heard), heard.readers(deleted.concernedMembers())));
        }
    }

    public void recurringCreated(SpaceMembership actor, RecurringTransactionSeries created) {
        if (concernsOthers(actor, created.concernedMembers())) {
            audience.of(actor).ifPresent(heard -> notifications.recurringCreated(heard.gesture(),
                facts.of(created, heard), heard.readers(created.concernedMembers())));
        }
    }

    public void recurringUpdated(SpaceMembership actor, RecurringTransactionSeries before, RecurringTransactionSeries after) {
        SequencedSet<UUID> concerned = union(before.concernedMembers(), after.concernedMembers());
        if (before.differsFrom(after) && concernsOthers(actor, concerned)) {
            audience.of(actor).ifPresent(heard -> notifications.recurringUpdated(heard.gesture(),
                facts.of(before, heard), facts.of(after, heard),
                involved(heard, concerned, before.concernedMembers(), after.concernedMembers())));
        }
    }

    public void recurringStopped(SpaceMembership actor, RecurringTransactionSeries stopped) {
        if (concernsOthers(actor, stopped.concernedMembers())) {
            audience.of(actor).ifPresent(heard -> notifications.recurringStopped(heard.gesture(),
                facts.of(stopped, heard), heard.readers(stopped.concernedMembers())));
        }
    }

    /** Whether anyone but the author is concerned — when nobody is, the members are not even read. */
    private static boolean concernsOthers(SpaceMembership actor, SequencedSet<UUID> concerned) {
        return concerned.stream().anyMatch(id -> !id.equals(actor.userId()));
    }

    private static SequencedSet<UUID> union(SequencedSet<UUID> before, SequencedSet<UUID> after) {
        SequencedSet<UUID> all = new LinkedHashSet<>(before);
        all.addAll(after);
        return all;
    }

    private static List<Involved> involved(Audience heard, SequencedSet<UUID> concerned,
                                           SequencedSet<UUID> before, SequencedSet<UUID> after) {
        return heard.readers(concerned).stream()
            .map(reader -> new Involved(reader, Involvement.of(before.contains(reader.userId()), after.contains(reader.userId()))))
            .toList();
    }
}
