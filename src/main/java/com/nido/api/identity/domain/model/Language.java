package com.nido.api.identity.domain.model;

import java.util.Arrays;
import java.util.Optional;

/** A language Nido is written in. Stored and exchanged as its code. */
public enum Language {
    FR("fr"),
    EN("en");

    private final String code;

    Language(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static Optional<Language> fromCode(String code) {
        return Arrays.stream(values()).filter(language -> language.code.equals(code)).findFirst();
    }
}
