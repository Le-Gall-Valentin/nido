package com.nido.api.mail.domain.port.out;

import com.nido.api.mail.domain.model.OutboxEntry;
import com.nido.api.mail.domain.model.OutgoingMail;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MailOutboxPort {

    /** Queues a mail in the caller's transaction, due at {@code now}. {@code expiresAt} may be null. */
    void enqueue(String kind, OutgoingMail mail, Instant now, Instant expiresAt);

    /**
     * Reserves up to {@code limit} due mails for {@code lease}, in a transaction of its own, skipping
     * rows another dispatcher holds. A reserved mail is invisible to other claims until its lease ends.
     */
    List<OutboxEntry> claimDue(Instant now, int limit, Duration lease);

    void delete(UUID id);

    /** Records a failed attempt, releases the lease and sets when the mail is due again. */
    void reschedule(UUID id, int attempts, Instant nextAttemptAt, String lastError);

    /**
     * Deletes every mail still waiting for this address, in the caller's transaction, and says how many. A
     * mail already handed to the SMTP server is beyond reach; a row this key cannot read is left to the
     * dispatcher, which drops it.
     */
    int deleteAddressedTo(String address);
}
