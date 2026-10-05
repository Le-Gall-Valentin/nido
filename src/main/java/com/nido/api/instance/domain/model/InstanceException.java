package com.nido.api.instance.domain.model;

import java.util.List;

public abstract sealed class InstanceException extends RuntimeException
    permits InstanceException.SetupCodeInvalid,
            InstanceException.SetupAlreadyCompleted,
            InstanceException.UnknownSetting,
            InstanceException.SettingsInvalid,
            InstanceException.SettingLockedByEnvironment,
            InstanceException.MailTestFailed,
            InstanceException.EncryptionKeyNotSaved,
            InstanceException.InitialAdminRefused {

    private InstanceException(String message) { super(message); }

    public static final class SetupCodeInvalid extends InstanceException {
        public SetupCodeInvalid() { super("The setup code is not the one in the logs"); }
    }

    public static final class SetupAlreadyCompleted extends InstanceException {
        public SetupAlreadyCompleted() { super("This installation is already set up"); }
    }

    public static final class UnknownSetting extends InstanceException {
        public UnknownSetting(String code) { super("There is no such setting here: " + code); }
    }

    public static final class SettingsInvalid extends InstanceException {
        private final List<SettingProblem> problems;
        public SettingsInvalid(List<SettingProblem> problems) {
            super("Some settings are not valid: " + problems);
            this.problems = List.copyOf(problems);
        }
        public List<SettingProblem> problems() { return problems; }
    }

    public static final class SettingLockedByEnvironment extends InstanceException {
        private final SettingKey key;
        public SettingLockedByEnvironment(SettingKey key) {
            super(key.code() + " is set by " + key.variable() + " in the environment and cannot be changed here");
            this.key = key;
        }
        public SettingKey key() { return key; }
    }

    /** {@code serverReply}: what the SMTP server answered, when it answered — shown to the administrator, never logged. */
    public static final class MailTestFailed extends InstanceException {
        private final String reason;
        private final String serverReply;
        public MailTestFailed(String reason, String serverReply) {
            super("The test mail could not be sent: " + reason);
            this.reason = reason;
            this.serverReply = serverReply;
        }
        public String reason() { return reason; }
        public String serverReply() { return serverReply; }
    }

    public static final class EncryptionKeyNotSaved extends InstanceException {
        public EncryptionKeyNotSaved() { super("Confirm that the encryption key is saved somewhere safe before finishing"); }
    }

    public static final class InitialAdminRefused extends InstanceException {
        public InitialAdminRefused(String field) { super("The administrator account was refused: invalid " + field); }
    }
}
