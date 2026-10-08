package com.nido.api.identity.domain.model;

public abstract sealed class IdentityException extends RuntimeException
    permits IdentityException.UserNotFound,
            IdentityException.UserNotActive,
            IdentityException.UserAlreadyInactive,
            IdentityException.UserAlreadyActive,
            IdentityException.InvalidUsername,
            IdentityException.UsernameAlreadyExists,
            IdentityException.EmailAlreadyExists,
            IdentityException.EmailCodeInvalid,
            IdentityException.EmailCodeSpent,
            IdentityException.InsufficientPermissions,
            IdentityException.CurrentPasswordRequired,
            IdentityException.InvalidCurrentPassword,
            IdentityException.RoleAlreadyAssigned,
            IdentityException.AccountAlreadyJoined,
            IdentityException.DataIntegrityError {

    private IdentityException(String message) { super(message); }

    public static final class UserNotFound extends IdentityException {
        public UserNotFound() { super("User not found"); }
    }
    public static final class UserNotActive extends IdentityException {
        public UserNotActive() { super("User account is disabled"); }
    }
    public static final class UserAlreadyInactive extends IdentityException {
        public UserAlreadyInactive() { super("User account is already inactive"); }
    }
    public static final class UserAlreadyActive extends IdentityException {
        public UserAlreadyActive() { super("User account is already active"); }
    }
    public static final class InvalidUsername extends IdentityException {
        public InvalidUsername() { super("A username is 3 to 50 characters and holds no @"); }
    }
    public static final class UsernameAlreadyExists extends IdentityException {
        public UsernameAlreadyExists() { super("Username already taken"); }
    }
    public static final class EmailAlreadyExists extends IdentityException {
        public EmailAlreadyExists() { super("Email already taken"); }
    }

    /** Too many wrong guesses took the code for the new address with them: a new one has to be asked for. */
    public static final class EmailCodeSpent extends IdentityException {
        public EmailCodeSpent() { super("The code sent to the new address no longer works"); }
    }

    /** The code given for a new address is not the one sent there — or no longer valid. */
    public static final class EmailCodeInvalid extends IdentityException {
        public EmailCodeInvalid() { super("The code sent to the new address is invalid or expired"); }
    }
    public static final class InsufficientPermissions extends IdentityException {
        public InsufficientPermissions() { super("Insufficient permissions"); }
    }
    /** An address change without the current password: the address is how an account is recovered. */
    public static final class CurrentPasswordRequired extends IdentityException {
        public CurrentPasswordRequired() { super("The current password is required to change the email address"); }
    }
    public static final class InvalidCurrentPassword extends IdentityException {
        public InvalidCurrentPassword() { super("Current password is incorrect"); }
    }

    public static final class RoleAlreadyAssigned extends IdentityException {
        public RoleAlreadyAssigned() { super("User already has this role"); }
    }

    /** An invitation resent to an account that already chose its password: there is nothing left to invite to. */
    public static final class AccountAlreadyJoined extends IdentityException {
        public AccountAlreadyJoined() { super("This account already chose its password"); }
    }

    public static final class DataIntegrityError extends IdentityException {
        public DataIntegrityError() { super("Unexpected data integrity violation"); }
    }
}