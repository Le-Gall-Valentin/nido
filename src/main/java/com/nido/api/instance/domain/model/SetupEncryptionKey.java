package com.nido.api.instance.domain.model;

/** The key for the last step: shown only when Nido generated it, to be saved by the administrator. */
public record SetupEncryptionKey(boolean generated, String key) {
    @Override
    public String toString() {
        return "SetupEncryptionKey[generated=" + generated + ", key=***]";
    }
}
