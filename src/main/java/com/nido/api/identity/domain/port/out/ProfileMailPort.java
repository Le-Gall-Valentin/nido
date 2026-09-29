package com.nido.api.identity.domain.port.out;

import com.nido.api.identity.domain.model.Language;

public interface ProfileMailPort {
    /** Tells the previous address that the account now uses another one. */
    void emailChanged(String username, String previousEmail, String newEmail, Language language);
}
