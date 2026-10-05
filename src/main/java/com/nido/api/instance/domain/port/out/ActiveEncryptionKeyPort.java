package com.nido.api.instance.domain.port.out;

/** The encryption key in use, for the one moment it is shown: the last step of the setup, when Nido generated it. */
public interface ActiveEncryptionKeyPort {
    String value();
}
