package com.nido.api.finance.domain.model;

public abstract sealed class FinanceException extends RuntimeException
    permits FinanceException.TransactionNotFound, FinanceException.RecurringSeriesNotFound,
            FinanceException.CategoryNotFound, FinanceException.CategoryInUse,
            FinanceException.InvalidContributionShares, FinanceException.PayerRequired,
            FinanceException.InvalidEndDate, FinanceException.NotAPartyToSettlement,
            FinanceException.SavingsGoalNotFound, FinanceException.InvalidSavingsGoalAppearance,
            FinanceException.ContributionExceedsGoalTarget, FinanceException.DecryptionFailed,
            FinanceException.MemberNotInSpace, FinanceException.SettlementExceedsDebt {

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

    /** Thrown when custom contribution shares don't sum to the transaction's total amount. */
    public static final class InvalidContributionShares extends FinanceException {
        public InvalidContributionShares() { super("Contribution shares must sum to the transaction amount"); }
    }

    /** Thrown when a transaction or recurring series has contributors (a split) but no payer — someone must have advanced the money being split. */
    public static final class PayerRequired extends FinanceException {
        public PayerRequired() { super("A payer is required when the transaction has contributors"); }
    }

    /** Thrown when a recurring series' end date is before its anchor date — it would never produce a single occurrence. */
    public static final class InvalidEndDate extends FinanceException {
        public InvalidEndDate() { super("The end date must be on or after the anchor date"); }
    }

    /** Thrown when settling a debt whose caller is neither the debtor nor the creditor — a third space member, even one who can write, has no business settling someone else's debt. */
    public static final class NotAPartyToSettlement extends FinanceException {
        public NotAPartyToSettlement() { super("Only the debtor or the creditor can settle this debt"); }
    }

    public static final class SavingsGoalNotFound extends FinanceException {
        public SavingsGoalNotFound() { super("Savings goal not found"); }
    }

    /** Thrown when a savings goal's color or glyph falls outside the fixed, validated design palette. */
    public static final class InvalidSavingsGoalAppearance extends FinanceException {
        public InvalidSavingsGoalAppearance() { super("Color or glyph outside the allowed palette"); }
    }

    /** Thrown when a contribution would push a goal's total past its target amount. */
    public static final class ContributionExceedsGoalTarget extends FinanceException {
        public ContributionExceedsGoalTarget() { super("This contribution would exceed the goal's target amount"); }
    }

    /** Thrown when a stored ciphertext cannot be decrypted (wrong/rotated master secret, corruption). */
    public static final class DecryptionFailed extends FinanceException {
        public DecryptionFailed() { super("Could not decrypt stored finance data"); }
    }

    /** Thrown when a submitted memberId (payer, contributor, settlement party, savings contributor) isn't actually a member of the space. */
    public static final class MemberNotInSpace extends FinanceException {
        public MemberNotInSpace() { super("Member is not part of this space"); }
    }

    /** Thrown when a settlement amount exceeds what the debtor actually owes the creditor, computed from every shared transaction and settlement so far. */
    public static final class SettlementExceedsDebt extends FinanceException {
        public SettlementExceedsDebt() { super("This settlement would exceed the amount actually owed"); }
    }
}
