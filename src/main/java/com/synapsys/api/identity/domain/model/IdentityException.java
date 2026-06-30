package com.synapsys.api.identity.domain.model;

public abstract sealed class IdentityException extends RuntimeException
    permits IdentityException.UserNotFound,
            IdentityException.UserNotActive,
            IdentityException.UserAlreadyInactive,
            IdentityException.UserAlreadyActive,
            IdentityException.UsernameAlreadyExists,
            IdentityException.EmailAlreadyExists,
            IdentityException.InsufficientPermissions,
            IdentityException.InvalidCurrentPassword,
            IdentityException.RoleAlreadyAssigned,
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
    public static final class UsernameAlreadyExists extends IdentityException {
        public UsernameAlreadyExists() { super("Username already taken"); }
    }
    public static final class EmailAlreadyExists extends IdentityException {
        public EmailAlreadyExists() { super("Email already taken"); }
    }
    public static final class InsufficientPermissions extends IdentityException {
        public InsufficientPermissions() { super("Insufficient permissions"); }
    }
    public static final class InvalidCurrentPassword extends IdentityException {
        public InvalidCurrentPassword() { super("Current password is incorrect"); }
    }

    public static final class RoleAlreadyAssigned extends IdentityException {
        public RoleAlreadyAssigned() { super("User already has this role"); }
    }

    public static final class DataIntegrityError extends IdentityException {
        public DataIntegrityError() { super("Unexpected data integrity violation"); }
    }
}