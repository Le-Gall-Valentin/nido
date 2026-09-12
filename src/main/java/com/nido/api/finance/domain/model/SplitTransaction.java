package com.nido.api.finance.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * A transaction reduced to what a balance is made of: one member handled an amount, and named
 * members each hold a share of it.
 *
 * <p>Which way that moves a balance depends on the type, and the two are mirror images. On an
 * EXPENSE the member fronted the money and the others owe their share back. On an INCOME the
 * member received money partly belonging to the others, so the debt runs the other way — hence
 * {@code type}, without which a shared refund would be folded as though it were a shared bill.
 *
 * <p>{@link BalanceCalculator} never looks at anything else — not the label, the category, the
 * date or the recurring series. Reading the full {@link Transaction} to fold a
 * balance therefore decrypted every label for nothing, which is half the cryptographic work on
 * a page that already loads the whole history.
 *
 * <p>Deliberately not a snapshot or a cache of the balance itself. A net per member is
 * financial data, so it would have to be encrypted, and an encrypted aggregate cannot be
 * updated incrementally in SQL — every write would become read-decrypt-add-reencrypt-write
 * under a lock, with a new place for the figure to drift and no way to notice. Folding the
 * ledger every time keeps one source of truth, and makes drift impossible rather than merely
 * unlikely. This shape is what makes that fold cheap enough to keep doing.
 */
public record SplitTransaction(
    /** The member who paid on an EXPENSE, the one who received on an INCOME. */
    UUID payerId,
    BigDecimal amount,
    /** Who owes a share on an EXPENSE, who is owed one on an INCOME. */
    List<Contribution> contributors,
    TransactionType type
) {}
