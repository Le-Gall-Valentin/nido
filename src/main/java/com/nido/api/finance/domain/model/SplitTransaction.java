package com.nido.api.finance.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * A transaction reduced to what a balance is made of: someone advanced an amount, and named
 * members owe a share of it.
 *
 * <p>{@link BalanceCalculator} never looks at anything else — not the label, the type, the
 * category, the date or the recurring series. Reading the full {@link Transaction} to fold a
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
public record SplitTransaction(UUID payerId, BigDecimal amount, List<Contribution> contributors) {}
