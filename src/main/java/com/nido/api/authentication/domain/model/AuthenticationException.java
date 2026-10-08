package com.nido.api.authentication.domain.model;

public abstract sealed class AuthenticationException extends RuntimeException
    permits AuthenticationException.InvalidCredentials,
            AuthenticationException.UserNotActive,
            AuthenticationException.TokenExpired,
            AuthenticationException.TokenNotFound,
            AuthenticationException.TokenRevoked,
            AuthenticationException.UserNotFound,
            AuthenticationException.TwoFactorCodeInvalid,
            AuthenticationException.TwoFactorChallengeExpired,
            AuthenticationException.TwoFactorMaxAttemptsExceeded,
            AuthenticationException.MethodNotEnabled,
            AuthenticationException.MethodUnavailable,
            AuthenticationException.MailCodeRefused,
            AuthenticationException.InvalidCurrentPassword,
            AuthenticationException.InvalidResetToken,
            AuthenticationException.InvalidInvitationToken,
            AuthenticationException.AccountAlreadyJoined,
            AuthenticationException.DataIntegrityError {

    private AuthenticationException(String message) {
        super(message);
    }

    public static final class InvalidCredentials extends AuthenticationException {
        public InvalidCredentials() { super("Invalid credentials"); }
    }

    public static final class UserNotActive extends AuthenticationException {
        public UserNotActive() { super("User account is disabled"); }
    }

    public static final class TokenExpired extends AuthenticationException {
        public TokenExpired() { super("Token has expired"); }
    }

    public static final class TokenNotFound extends AuthenticationException {
        public TokenNotFound() { super("Token not found"); }
    }

    public static final class TokenRevoked extends AuthenticationException {
        public TokenRevoked() { super("Token has been revoked"); }
    }

    public static final class UserNotFound extends AuthenticationException {
        public UserNotFound() { super("User not found"); }
    }

    public static final class TwoFactorCodeInvalid extends AuthenticationException {
        public TwoFactorCodeInvalid() { super("Invalid or expired code"); }
    }

    public static final class TwoFactorChallengeExpired extends AuthenticationException {
        public TwoFactorChallengeExpired() { super("Two-factor authentication challenge has expired"); }
    }

    public static final class TwoFactorMaxAttemptsExceeded extends AuthenticationException {
        public TwoFactorMaxAttemptsExceeded() { super("Too many incorrect codes"); }
    }

    /** The method asked for is not on for this account — removed by an administrator during the sign-in, say. */
    public static final class MethodNotEnabled extends AuthenticationException {
        public MethodNotEnabled() { super("This two-factor method is not on for this account"); }
    }

    /** On, but paused: the mail method while mail is off. */
    public static final class MethodUnavailable extends AuthenticationException {
        public MethodUnavailable() { super("This two-factor method cannot be used right now"); }
    }

    /** The sign-in code could not leave now: too soon after the last one, or too many in the window. */
    public static final class MailCodeRefused extends AuthenticationException {
        private final boolean tooSoon;
        private final long retryAfterSeconds;
        public MailCodeRefused(boolean tooSoon, long retryAfterSeconds) {
            super(tooSoon ? "A code was sent moments ago" : "Too many codes sent by mail");
            this.tooSoon = tooSoon;
            this.retryAfterSeconds = retryAfterSeconds;
        }
        public boolean tooSoon() { return tooSoon; }
        public long retryAfterSeconds() { return retryAfterSeconds; }
    }

    public static final class InvalidCurrentPassword extends AuthenticationException {
        public InvalidCurrentPassword() { super("Current password is incorrect"); }
    }

    /** Expired, already used, replaced by a newer one, or never issued: one answer for all of them. */
    public static final class InvalidResetToken extends AuthenticationException {
        public InvalidResetToken() { super("Password reset link is no longer valid"); }
    }

    /** Expired, already used, replaced by a newer one, never issued, or its account is off: one answer for all. */
    public static final class InvalidInvitationToken extends AuthenticationException {
        public InvalidInvitationToken() { super("Invitation link is no longer valid"); }
    }

    /** An invitation for an account that already chose its password: there is nothing left to invite to. */
    public static final class AccountAlreadyJoined extends AuthenticationException {
        public AccountAlreadyJoined() { super("This account already chose its password"); }
    }

    public static final class DataIntegrityError extends AuthenticationException {
        public DataIntegrityError() { super("Unexpected data integrity violation"); }
    }
}