package com.nido.api.finance.domain.model;

public abstract sealed class FinanceException extends RuntimeException
    permits FinanceException.TransactionNotFound, FinanceException.RecurringSeriesNotFound,
            FinanceException.CategoryNotFound, FinanceException.CategoryInUse,
            FinanceException.SameSpaceTransfer, FinanceException.InvalidContributionShares, FinanceException.PayerRequired,
            FinanceException.SavingsGoalNotFound, FinanceException.DecryptionFailed {

    private FinanceException(String message) { super(message); }

    public static final class TransactionNotFound extends FinanceException {
        public TransactionNotFound() { super("Transaction not found"); }
    }

    public static final class RecurringSeriesNotFound extends FinanceException {
        public RecurringSeriesNotFound() { super("Recurring series not found"); }
    }

    public static final class CategoryNotFound extends FinanceException {
        public CategoryNotFound() { super("Category not found"); }
    }

    /** Thrown when deleting a category still referenced by at least one transaction. */
    public static final class CategoryInUse extends FinanceException {
        public CategoryInUse() { super("Category is still used by existing transactions"); }
    }

    /** Thrown when a move targets the same context the transaction is already in. */
    public static final class SameSpaceTransfer extends FinanceException {
        public SameSpaceTransfer() { super("Cannot transfer a transaction into its own context"); }
    }

    /** Thrown when custom contribution shares don't sum to the transaction's total amount. */
    public static final class InvalidContributionShares extends FinanceException {
        public InvalidContributionShares() { super("Contribution shares must sum to the transaction amount"); }
    }

    /** Thrown when a transaction or recurring series has contributors (a split) but no payer — someone must have advanced the money being split. */
    public static final class PayerRequired extends FinanceException {
        public PayerRequired() { super("A payer is required when the transaction has contributors"); }
    }

    public static final class SavingsGoalNotFound extends FinanceException {
        public SavingsGoalNotFound() { super("Savings goal not found"); }
    }

    /** Thrown when a stored ciphertext cannot be decrypted (wrong/rotated master secret, corruption). */
    public static final class DecryptionFailed extends FinanceException {
        public DecryptionFailed() { super("Could not decrypt stored finance data"); }
    }
}
