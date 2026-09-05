package com.nido.api.finance.domain.model;

public abstract sealed class FinanceException extends RuntimeException
    permits FinanceException.TransactionNotFound, FinanceException.RecurringSeriesNotFound,
            FinanceException.TransactionLinkedToSeries, FinanceException.CategoryNotFound, FinanceException.CategoryInUse,
            FinanceException.SameSpaceTransfer, FinanceException.InvalidContributionShares,
            FinanceException.SavingsGoalNotFound, FinanceException.DecryptionFailed {

    private FinanceException(String message) { super(message); }

    public static final class TransactionNotFound extends FinanceException {
        public TransactionNotFound() { super("Transaction not found"); }
    }

    public static final class RecurringSeriesNotFound extends FinanceException {
        public RecurringSeriesNotFound() { super("Recurring series not found"); }
    }

    /** Thrown when deleting a transaction that was materialized by a recurring series directly — delete the series instead. */
    public static final class TransactionLinkedToSeries extends FinanceException {
        public TransactionLinkedToSeries() { super("Cannot delete a transaction that belongs to a recurring series; delete the series instead"); }
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

    public static final class SavingsGoalNotFound extends FinanceException {
        public SavingsGoalNotFound() { super("Savings goal not found"); }
    }

    /** Thrown when a stored ciphertext cannot be decrypted (wrong/rotated master secret, corruption). */
    public static final class DecryptionFailed extends FinanceException {
        public DecryptionFailed() { super("Could not decrypt stored finance data"); }
    }
}
