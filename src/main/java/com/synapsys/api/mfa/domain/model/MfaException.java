package com.synapsys.api.mfa.domain.model;

public abstract sealed class MfaException extends RuntimeException
    permits MfaException.UserNotFound, MfaException.TotpAlreadyEnabled,
            MfaException.TotpNotEnabled, MfaException.TotpSetupNotStarted,
            MfaException.TotpCodeInvalid, MfaException.TotpConfirmMaxAttemptsExceeded,
            MfaException.InsufficientPermissions {

    private MfaException(String message) { super(message); }

    public static final class UserNotFound extends MfaException {
        public UserNotFound() { super("User not found"); }
    }
    public static final class TotpAlreadyEnabled extends MfaException {
        public TotpAlreadyEnabled() { super("Two-factor authentication is already enabled"); }
    }
    public static final class TotpNotEnabled extends MfaException {
        public TotpNotEnabled() { super("Two-factor authentication is not enabled"); }
    }
    public static final class TotpSetupNotStarted extends MfaException {
        public TotpSetupNotStarted() { super("Two-factor authentication setup has not been started"); }
    }
    public static final class TotpCodeInvalid extends MfaException {
        public TotpCodeInvalid() { super("Invalid or expired TOTP code"); }
    }
    public static final class TotpConfirmMaxAttemptsExceeded extends MfaException {
        public TotpConfirmMaxAttemptsExceeded() { super("Too many failed confirmation attempts. Please restart the setup."); }
    }
    public static final class InsufficientPermissions extends MfaException {
        public InsufficientPermissions() { super("Insufficient permissions"); }
    }
}