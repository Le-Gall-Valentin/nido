package com.nido.api.mfa.domain.model;

public abstract sealed class MfaException extends RuntimeException
    permits MfaException.UserNotFound, MfaException.MethodAlreadyEnabled, MfaException.MethodNotEnabled,
            MfaException.MethodUnavailable, MfaException.MethodSendsNoCode, MfaException.EnrolmentNotStarted,
            MfaException.CodeInvalid, MfaException.CodeSpent, MfaException.ConfirmMaxAttemptsExceeded, MfaException.ResendTooSoon,
            MfaException.SendLimitReached, MfaException.InsufficientPermissions {

    private MfaException(String message) { super(message); }

    public static final class UserNotFound extends MfaException {
        public UserNotFound() { super("User not found"); }
    }
    public static final class MethodAlreadyEnabled extends MfaException {
        public MethodAlreadyEnabled() { super("This two-factor method is already on"); }
    }
    public static final class MethodNotEnabled extends MfaException {
        public MethodNotEnabled() { super("This two-factor method is not on"); }
    }
    /** The mail method while mail is off: it can be neither turned on nor asked for. */
    public static final class MethodUnavailable extends MfaException {
        public MethodUnavailable() { super("This two-factor method cannot be used right now"); }
    }
    /** A code asked of the application method, whose codes come from the app itself. */
    public static final class MethodSendsNoCode extends MfaException {
        public MethodSendsNoCode() { super("This two-factor method sends no code"); }
    }
    /** Never started, or started and expired — one answer for both, on purpose. */
    public static final class EnrolmentNotStarted extends MfaException {
        public EnrolmentNotStarted() { super("No two-factor setup is under way"); }
    }
    public static final class CodeInvalid extends MfaException {
        public CodeInvalid() { super("Invalid or expired code"); }
    }
    /** Too many wrong guesses took the code with it — or none is waiting: a new one has to be asked for. */
    public static final class CodeSpent extends MfaException {
        public CodeSpent() { super("This code no longer works. Please ask for a new one."); }
    }

    public static final class ConfirmMaxAttemptsExceeded extends MfaException {
        public ConfirmMaxAttemptsExceeded() { super("Too many failed confirmation attempts. Please restart the setup."); }
    }
    /** A code was sent for the same thing moments ago: the one already sent still works. */
    public static final class ResendTooSoon extends MfaException {
        private final long seconds;
        public ResendTooSoon(long seconds) { super("A code was sent moments ago"); this.seconds = seconds; }
        public long seconds() { return seconds; }
    }
    /** The account has been sent all the mails of code its window allows. */
    public static final class SendLimitReached extends MfaException {
        private final long seconds;
        public SendLimitReached(long seconds) { super("Too many codes sent by mail"); this.seconds = seconds; }
        public long seconds() { return seconds; }
    }
    public static final class InsufficientPermissions extends MfaException {
        public InsufficientPermissions() { super("Insufficient permissions"); }
    }
}
