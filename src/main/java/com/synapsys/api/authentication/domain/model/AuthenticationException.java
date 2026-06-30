package com.synapsys.api.authentication.domain.model;

public abstract sealed class AuthenticationException extends RuntimeException
    permits AuthenticationException.InvalidCredentials,
            AuthenticationException.UserNotActive,
            AuthenticationException.TokenExpired,
            AuthenticationException.TokenNotFound,
            AuthenticationException.TokenRevoked,
            AuthenticationException.UserNotFound,
            AuthenticationException.TotpCodeInvalid,
            AuthenticationException.TotpChallengeExpired,
            AuthenticationException.TotpMaxAttemptsExceeded,
            AuthenticationException.InvalidCurrentPassword,
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

    public static final class TotpCodeInvalid extends AuthenticationException {
        public TotpCodeInvalid() { super("Invalid or expired TOTP code"); }
    }

    public static final class TotpChallengeExpired extends AuthenticationException {
        public TotpChallengeExpired() { super("Two-factor authentication challenge has expired"); }
    }

    public static final class TotpMaxAttemptsExceeded extends AuthenticationException {
        public TotpMaxAttemptsExceeded() { super("Too many incorrect TOTP attempts"); }
    }

    public static final class InvalidCurrentPassword extends AuthenticationException {
        public InvalidCurrentPassword() { super("Current password is incorrect"); }
    }

    public static final class DataIntegrityError extends AuthenticationException {
        public DataIntegrityError() { super("Unexpected data integrity violation"); }
    }
}